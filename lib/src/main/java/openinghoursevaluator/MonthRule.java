package openinghoursevaluator;

import java.time.chrono.ChronoLocalDate;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ch.poole.openinghoursparser.DateRange;
import ch.poole.openinghoursparser.Month;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.WeekDayRange;
import ch.poole.openinghoursparser.WeekRange;
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
     * @throws OpeningHoursEvaluationException
     */
    public void buildWeek(LocalDateTime time) throws OpeningHoursEvaluationException {
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
     * Build only one day, taken from the date of LocalDateTime. Used during
     * checkStatus()
     * 
     * @param time input LocalDateTime
     * @return a Week that contains only one WeekDayRule of the date
     *      from LocalDateTime
     * @throws OpeningHoursEvaluationException
     */
    public Week buildOneDay(LocalDateTime time) throws OpeningHoursEvaluationException {
        year = time.getYear();
        month = convertMonth(time.toLocalDate());
        LocalDate date = time.toLocalDate();
        Week oneDay = new Week(date, Week.convertWeekDay(date.getDayOfWeek()));
        for (Rule rule : rules) {
            List<TimeRange> previousSpill = simulateSpill(oneDay, rule);
            oneDay.setPreviousSpill(previousSpill);
            update(oneDay, rule);
        }
        oneDay.applyPreviousSpill();
        return oneDay;
    }

    /**
     * Helper function for build().
     * @throws OpeningHoursEvaluationException
     */
    private void update(Week week, Rule rule) throws OpeningHoursEvaluationException {
        updateWithYearRange(rule, week);
    }

    /** 
     * Helper function for update(). Check for YearRange and then build accordingly
     */
    private void updateWithYearRange(Rule rule, Week week) throws OpeningHoursEvaluationException {
        if (rule.getYears() != null) {
            YearManager yearManager = new YearManager();
            for (YearRange yearRange : rule.getYears()) {
                // if found applicable YearRange move to check for update with
                // WeekRange right away
                if (yearManager.processYearRange(yearRange, week)) {
                    updateWithWeekRange(rule, week);
                    return;
                }
            }
        } else {
            updateWithWeekRange(rule, week);
        }
    }

    /** 
     * Helper function for updateWithYearRange(). Check for WeekRange and then
     * build accordingly
     */
    private void updateWithWeekRange(Rule rule, Week week) throws OpeningHoursEvaluationException {
        if (rule.getWeeks() != null) {
            WeekManager weekManager = new WeekManager();
            for (WeekRange weekRange : rule.getWeeks()) {
                // if found applicable WeekRange move to check for update with
                // DateRange right away
                if (weekManager.processWeekRange(weekRange, week)) {
                    updateWithDateRange(rule, week);
                    return;
                }
            }
        } else {
            updateWithDateRange(rule, week);
        }
    }

    /** 
     * Helper function for updateWithWeekRange(). Check for DateRange and then
     * build accordingly
     */
    private void updateWithDateRange(Rule rule, Week week) throws OpeningHoursEvaluationException {
        if (rule.getDates() != null) {
            DateManager dateManager = new DateManager();
            for (DateRange dateRange : rule.getDates()) {
                List<List<LocalDate>> temps = dateManager.processDateRange(dateRange, week);
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
     * @throws OpeningHoursEvaluationException
     */
    private List<TimeRange> simulateSpill(Week week, Rule rule) throws OpeningHoursEvaluationException {
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
     * @throws OpeningHoursEvaluationException
     */
    public Result checkStatus(LocalDateTime time) throws OpeningHoursEvaluationException {
        Week dayToCheck = buildOneDay(time);
        dayToCheck.clean();
        return dayToCheck.checkStatus(time);
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

    public static int maxDayOfMonth(int month, int year) {
        YearMonth monthOfYear = YearMonth.of(year, month);
        return monthOfYear.lengthOfMonth();
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

    /**
     * @return the toDebugString of the Week stored in this MonthRule
     */
    public String toDebugWeekString() {
        StringBuilder b = new StringBuilder();
        if(weekStorage.size() == 1) {
            b.append(weekStorage.get(0).toDebugString());
        } else {
            b.append(weekStorage.get(0).toDebugString());
            b.append("Next month\n");
            b.append(weekStorage.get(1).toDebugString());
        }
        return b.toString();
    }
}
