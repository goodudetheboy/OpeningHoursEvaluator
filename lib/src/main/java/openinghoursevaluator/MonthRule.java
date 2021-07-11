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

import javax.annotation.Nullable;

import ch.poole.openinghoursparser.DateRange;
import ch.poole.openinghoursparser.Month;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.RuleModifier;
import ch.poole.openinghoursparser.WeekDay;
import ch.poole.openinghoursparser.WeekDayRange;
import ch.poole.openinghoursparser.WeekRange;
import ch.poole.openinghoursparser.YearRange;
import ch.poole.openinghoursparser.RuleModifier.Modifier;

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
            simulateSpill(weekStorage.get(0), rule);
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
            simulateSpill(oneDay, rule);
            // oneDay.setPreviousSpill(previousSpill);
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
                List<List<LocalDate>> restrictions = dateManager.processDateRange(dateRange, week);
                restrictionProcessing(restrictions, rule, week, dateRange);
            }
        } else {
            week.build(rule);
        }
    }

    /**
     * Helper function for updateWithDateRange(). Process LocalDate restrictions
     * on input Week and build if applicable
     * @throws OpeningHoursEvaluationException
     * 
     */
    private void restrictionProcessing(List<List<LocalDate>> restrictions,
                    Rule rule, Week week, DateRange dateRange)
            throws OpeningHoursEvaluationException {
        // get LocalDate of start and end of input Week
        LocalDate startWDR = week.getStartWeekDayRule().getDefDate();
        LocalDate endWDR = week.getEndWeekDayRule().getDefDate();
        for (List<LocalDate> resDate : restrictions) {
            // get LocalDate of start and end of restriction
            LocalDate start = resDate.remove(0);
            LocalDate end = (resDate.isEmpty()) ? start : resDate.remove(0);
            List<ChronoLocalDate> overlap = Utils.getOverlap(start, end, startWDR, endWDR);

            // build if there is applicable range
            if (overlap != null) {
                // create weekday restriction
                WeekDayRange restriction = new WeekDayRange();
                DayOfWeek startWDay = ((LocalDate) overlap.get(0)).getDayOfWeek();
                DayOfWeek endWDay = ((LocalDate) overlap.get(1)).getDayOfWeek();
                restriction.setStartDay(Week.convertWeekDay(startWDay));
                restriction.setEndDay(Week.convertWeekDay(endWDay));

                // check for open ended date range
                if (DateManager.isOpenEndDateRange(dateRange)) {
                    week.build(processRuleWithOpenEnd(rule), restriction);
                } else {
                    week.build(rule, restriction);
                }
            }
        }
    }

    private Rule processRuleWithOpenEnd(Rule rule) {
        Rule openEndRule = rule.copy();
        if (openEndRule.getModifier() != null) {
            RuleModifier modifier = openEndRule.getModifier();
            if (modifier.getComment() != null) {
                modifier.setComment(DateManager.DEFAULT_OPEN_END_COMMENT);
            }
        } else {
            RuleModifier modifier = new RuleModifier();
            modifier.setComment(DateManager.DEFAULT_OPEN_END_COMMENT);
            modifier.setModifier(Modifier.UNKNOWN);
            openEndRule.setModifier(modifier);
        }
        return openEndRule;
    }

    /**
     * Used during MonthRule build(). This is to get the time spills of previous
     * week
     * 
     * @param week a Week to be simulated
     * @param rule a Rule to be applied
     * @throws OpeningHoursEvaluationException
     */
    private void simulateSpill(Week week, Rule rule) throws OpeningHoursEvaluationException {
        Week dayBeforeWeek = new Week(week.getDayBefore());
        update(dayBeforeWeek, rule);
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
     * Return next differing event of the input time (status different
     * from status of the evaluation of inputTime against the stored rules).
     * <p>
     * This assumes that the weekday of the input time is within this Week
     * 
     * @param inputTime time to be checked
     * @param status the status that needs that the next event's status
     *      has to be different from
     * @param isNext true to look next differing event, false to look last
     * @return next differing event of the input time (status different from
     *      status of the evaluation of inputTime against the stored rules)
     */
    @Nullable
    Result getDifferingEvent(LocalDateTime inputTime, Status status, boolean isNext) {
        WeekDay weekday = Week.convertWeekDay(inputTime.getDayOfWeek());
        Result result = null;
        for (Week week : weekStorage) {
            if (week.hasWeekDay(weekday)) {
                result = week.getDifferingEventThisWeek(inputTime, status, isNext);
                break;
            }
        }
        return result;
    }

    /**
     * Return next differing event whose status is different from the input
     * Status
     * 
     * @param status the status that needs that the next event's status
     *      has to be different from
     * @param isNext true to look next differing event, false to look last
     * @return next differing event whose status is different from the
     *      input Status
     */
    @Nullable
    Result getDifferingEvent(Status status, boolean isNext) {
        Week week = (isNext) ? weekStorage.get(0) 
                             : weekStorage.get(weekStorage.size()-1);
        Result result;
        WeekDayRule start = (isNext) ? week.getStartWeekDayRule()
                                     : week.getEndWeekDayRule();
        result = week.getDifferingEvent(start, status, isNext);
        return result;
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

    public static int getMaxDayOfMonth(int month, int year) {
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
