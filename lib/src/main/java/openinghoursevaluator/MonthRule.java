package openinghoursevaluator;

import java.time.chrono.ChronoLocalDate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ch.poole.openinghoursparser.DateRange;
import ch.poole.openinghoursparser.DateWithOffset;
import ch.poole.openinghoursparser.Month;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.WeekDayRange;
import ch.poole.openinghoursparser.YearRange;

public class MonthRule {
    int         year;
    Month       month;
    List<Rule>  rules = null;
    List<Week>  weekStorage = null;

    public MonthRule() {
        // nothing here
    }

    public MonthRule(List<Rule> rules) {
        weekStorage = new ArrayList<>();
        this.rules = rules;
    }

    /**
     * Build a Week of this MonthRule on an input of time, complete with week number.
     * Also supports Week that is split between two months.
     * 
     * @param time input LocalDateTime
     */
    public void build(LocalDateTime time) {
        year = time.getYear();
        month = convertMonth(time.toLocalDate());
        populate(time);
        for (Rule rule : rules) {
            List<TimeRange> previousSpill = simulateSpill(weekStorage.get(0), rule);
            weekStorage.get(0).setPreviousSpill(previousSpill);
            for (Week week : weekStorage) {
                update(week, rule);
            }
        }
    }

    /**
     * Helper function for build().
     */
    private void update(Week week, Rule rule) {
        if (rule.getDates() != null) {
            for (DateRange dateRange : rule.getDates()) {
                List<List<LocalDate>> temps = processDateRange(dateRange, week);
                LocalDate startWDR = week.getStartWeekDayRule().getDefDate();
                LocalDate endWDR = week.getEndWeekDayRule().getDefDate();
                for (List<LocalDate> temp : temps) {
                    LocalDate start = temp.remove(0);
                    LocalDate end = (temp.isEmpty()) ? start : temp.remove(0);
                    List<ChronoLocalDate> overlap = Utils.getOverlap(start, end, startWDR, endWDR);
                    if (overlap != null) {
                        WeekDayRange restriction = new WeekDayRange();
                        restriction.setStartDay(Week.convertWeekDay(((LocalDate) overlap.get(0)).getDayOfWeek()));
                        restriction.setEndDay(Week.convertWeekDay(((LocalDate) overlap.get(1)).getDayOfWeek()));
                        week.build(rule, restriction);
                    }
                }
            }
        } else {
            week.build(rule);
        }
    }

    /**
     * Used during MonthRule build(). This is to get the time spills of previous
     * week
     * 
     * @param week a Week to be simulated
     * @param rule a Rule to be applied
     * @return the simulated spill
     */
    private List<TimeRange> simulateSpill(Week week, Rule rule) {
        LocalDate firstDateOfWeek = week.getStartWeekDayRule().getDefDate();
        LocalDate previousDay = WeekDayRule.getOffsetDate(firstDateOfWeek, -1);
        Week w = new Week(previousDay, Week.convertWeekDay(previousDay.getDayOfWeek()));
        update(w, rule);
        return w.getWeekSpill();
    }

    /**
     * Process the DateRange inside to a LocalDate range. Used to find applicable
     * WeekDay to which a Rule can apply.
     * 
     * @param dateRange a DateRange
     * @param week a Week where this DateRange will apply
     * @return a LocalDate range processed from DateRange
     * @see https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification#monthday_range, explanation
     */
    private List<List<LocalDate>> processDateRange(DateRange dateRange, Week week) {
        int defaultYear = week.getYear();
        List<List<LocalDate>> result = new ArrayList<>();
        DateWithOffset start = dateRange.getStartDate();
        DateWithOffset end = dateRange.getEndDate();
        checkError(start, end);
        // there is always a start of DateRange
        List<LocalDate> subResult = new ArrayList<>();
        result.add(subResult);
        subResult.add(Utils.convertToLocalDate(start, defaultYear, start.getMonth()));
        if (end != null) {
            // handle when there is no year specified but there is year wrapping
            // the compare below only check for date and month
            // @see https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification#explain:monthday_range:date_offset:to:date_offset
            if (compareStartAndEnd(start, end) > 0) {
                subResult.add(Utils.convertToLocalDate(end, defaultYear + 1, start.getMonth()));
                List<LocalDate> otherResult = new ArrayList<>();
                otherResult.add(Utils.convertToLocalDate(start, defaultYear-1, start.getMonth()));
                otherResult.add(Utils.convertToLocalDate(end, defaultYear, start.getMonth()));
                result.add(otherResult);
            } else { 
                subResult.add(Utils.convertToLocalDate(end, defaultYear, start.getMonth()));
            }
        }
        return result;
    }

    /**
     * compareTo() for DateWithOffset (for now), check only the date stored in it
     * 
     * @param start start of a DateRange
     * @param end end of a DateRange
     * @return <0 if d1 is before d2, >0 if d1 is after d2, =0 if d1 is same day as d2
     */
    private int compareStartAndEnd(DateWithOffset start, DateWithOffset end) {
        return ((end.getMonth() == null) || start.getMonth() == end.getMonth())
                ? start.getDay() - end.getDay()
                : start.getMonth().ordinal() - end.getMonth().ordinal();
    }

    private void checkError(DateWithOffset start, DateWithOffset end) {
        String rangeString = "(" + start + " - " + end + ")";
        if (end == null) {
            return;
        }
        if (start.getYear() == YearRange.UNDEFINED_YEAR
                && end.getYear() != YearRange.UNDEFINED_YEAR) {
            throw new IllegalArgumentException("Year must be defined at start rather than end, this range "
                                                + rangeString + " is meaningless");
        }
        if (start.getYear() != YearRange.UNDEFINED_YEAR
                && compareStartAndEnd(start, end) > 0) {
            throw new IllegalArgumentException("Illegal range " + rangeString + ", please double check");
        }
        if (start.getYear() != YearRange.UNDEFINED_YEAR
                && end.getYear() != YearRange.UNDEFINED_YEAR
                && start.getYear() > end.getYear()) {
            throw new IllegalArgumentException("Illegal range " + rangeString + ", please double check");
        }
    }

    /**
     * Evaluate the stored OH string with a time to see if it's opening or closed
     * 
     * @param time input LocalDateTime
     * @return the result of the evaluation
     */
    public Result checkStatus(LocalDateTime time) {
        if (weekStorage.get(0).hasWeekDay(Week.convertWeekDay(time.getDayOfWeek()))) {
            return weekStorage.get(0).checkStatus(time);
        } else {
            return weekStorage.get(1).checkStatus(time);
        }
    }

    /**
     * Populate a Week of this MonthRule on an input time.
     * 
     * @param time input LocalDateTime
     */
    public void populate(LocalDateTime time) {
        weekStorage = Week.createEmptyWeek(time.toLocalDate());
    }

    /**
     * Return the first day of a month, based on an input time
     *  
     * @param date input time
     * @return the first day of a month, based on an input time
     */
    public static LocalDate getFirstDayOfMonth(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    /**
     * Return the last day of a month, based on an input time
     *  
     * @param date input time
     * @return the last day of a month, based on an input time
     */
    public static LocalDate getLastDayOfMonth(LocalDate date) {
       return date.withDayOfMonth(date.lengthOfMonth());
    }

    /**
     * Convert the Month class in the LocalDateTime to Month class in the OpeningHoursParser
     * 
     * @param time
     * @return
     */
    public static Month convertMonth(LocalDate date) {
        return Month.values()[date.getMonth().ordinal()];
    }

    /**
     * Return the week of a month, based on a time and a Locale
     * 
     * @param date
     * @return
     */
    public static int getNthWeekOfMonth(LocalDate date, Locale locale) {
        Calendar c = Calendar.getInstance(locale);
        c.set(date.getYear(), date.getMonthValue()-1, date.getDayOfMonth());
        c.setMinimalDaysInFirstWeek(1);
        return c.get(Calendar.WEEK_OF_MONTH);
    }

    /**
     * Return the order of a week of the input time, in reverse order of the month.
     * For example: normally, in a 5-week month, the 2nd week would be the 3rd week
     * in reverse of the month
     * 
     * @param date input time
     * @return order of a week of the input time, in reverse order of the month
     */
    public static int getReverseNthWeekOfMonth(LocalDate date, Locale locale) {
        return getNthWeekOfMonth(date, locale) - 1 - getNumOfWeekOfMonth(date, locale);
    }

    /**
     * Return the number of week of a month of an input time
     * 
     * @param time input time
     * @return number of week of a month of an input time
     */
    public static int getNumOfWeekOfMonth(LocalDate date, Locale locale) {
        return getNthWeekOfMonth(getLastDayOfMonth(date), locale);
    }

    /**
     * Return the number of the previous week of a week of a month
     * 
     * @param weekOfMonth input week of month
     * @return the number of the previous week of a week of a month
     */
    public static int getPreviousWeekOfMonth(int weekOfMonth) {
        // TODO: Refactor this to be more dynamic
        return (weekOfMonth == 1) ? 5 : weekOfMonth-1;
    }

    /**
     * Return the number of the previous week of a week of a month
     * 
     * @param weekOfMonth input week of month
     * @return the number of the previous week of a week of a month
     */
    public static int getPreviousReverseWeekOfMonth(int weekOfMonth) {
        // TODO: Refactor this to be more dynamic
        return (weekOfMonth == -5) ? -1 : weekOfMonth-1;
    }

    /**
     * @return the toString of the Week stored in this MonthRule
     */
    public String toWeekString() {
        StringBuilder b = new StringBuilder();
        if(weekStorage.size() == 1) {
            b.append(weekStorage.get(0).toString());
        } else {
            b.append(weekStorage.get(0).toString());
            b.append("Next month\n");
            b.append(weekStorage.get(1).toString());
        }
        return b.toString();
    }
}
