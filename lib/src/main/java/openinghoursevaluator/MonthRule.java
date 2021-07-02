package openinghoursevaluator;

import java.time.chrono.ChronoLocalDate;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ch.poole.openinghoursparser.DateRange;
import ch.poole.openinghoursparser.Month;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.WeekDayRange;

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
            DateManager manager = new DateManager();
            for (DateRange dateRange : rule.getDates()) {
                List<List<LocalDate>> temps = manager.processDateRange(dateRange, week);
                LocalDate startWDR = week.getStartWeekDayRule().getDefDate();
                LocalDate endWDR = week.getEndWeekDayRule().getDefDate();
                for (List<LocalDate> temp : temps) {
                    LocalDate start = temp.remove(0);
                    LocalDate end = (temp.isEmpty()) ? start : temp.remove(0);
                    List<ChronoLocalDate> overlap = Utils.getOverlap(start, end, startWDR, endWDR);
                    if (overlap != null) {
                        WeekDayRange restriction = new WeekDayRange();
                        DayOfWeek startWDay = ((LocalDate) overlap.get(0)).getDayOfWeek();
                        DayOfWeek endWDay = ((LocalDate) overlap.get(1)).getDayOfWeek();
                        restriction.setStartDay(Week.convertWeekDay(startWDay));
                        restriction.setEndDay(Week.convertWeekDay(endWDay));
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
        LocalDate previousDay = DateManager.getOffsetDate(firstDateOfWeek, -1);
        Week w = new Week(previousDay, Week.convertWeekDay(previousDay.getDayOfWeek()));
        update(w, rule);
        return w.getWeekSpill();
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
     * Return the number of week of a month of an input time
     * 
     * @param time input time
     * @return number of week of a month of an input time
     */
    public static int getNumOfWeekOfMonth(LocalDate date, Locale locale) {
        return getNthWeekOfMonth(getLastDayOfMonth(date), locale);
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
