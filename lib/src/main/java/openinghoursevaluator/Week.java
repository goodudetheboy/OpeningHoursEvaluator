package openinghoursevaluator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import ch.poole.openinghoursparser.Month;
import ch.poole.openinghoursparser.Nth;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.WeekDay;
import ch.poole.openinghoursparser.WeekDayRange;

public class Week {
    public static final int INVALID_NUM = Integer.MIN_VALUE;

    // defining date of this Week
    LocalDate defDate               = null;

    // used during eval
    int     year                    = INVALID_NUM;
    Month   month                   = null;
    int     weekOfYear              = INVALID_NUM;
    int     weekOfMonth             = INVALID_NUM;
    int     reverseWeekOfMonth      = INVALID_NUM;

    WeekDay startWeekDay            = null;
    WeekDay endWeekDay              = null;

    // used for connecting between days
    List<TimeRange> previousSpill   = null;
    WeekDayRule dayAfter            = null;
    
    // weekday storage
    EnumMap<WeekDay, WeekDayRule>   weekDayStorage = null;

    public Week() {
        // nothing here
    }

    public Week(LocalDate defDate) {
        // setting weekOfYear and weekOfMonth
        this(defDate, WeekDay.MO, WeekDay.SU);
    }

    public Week(LocalDate defDate, WeekDay oneWeekDay) {
        this(defDate, oneWeekDay, oneWeekDay);
    }

    /**
     * 
     * @param rules 
     * @param defDate
     * @param startWeekDay
     * @param endWeekDay
     */
    public Week(LocalDate defDate, WeekDay startWeekDay, WeekDay endWeekDay) {
        this.defDate = defDate;
        dissectDefDate(defDate);
        setStartWeekDay(startWeekDay);
        setEndWeekDay(endWeekDay);
        weekDayStorage = new EnumMap<>(WeekDay.class);
        populate();
    }

    /** Helper function for constructor */
    private void dissectDefDate(LocalDate defDate) {
        this.year = defDate.getYear();
        this.month = MonthRule.convertMonth(defDate);
        this.weekOfYear = defDate.get(WeekFields.of(Locale.FRANCE).weekOfWeekBasedYear());
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setMonth(Month month) {
        this.month = month;
    }

    public void setWeekOfMonth(int weekOfMonth) {
        if (weekOfMonth < 1 || weekOfMonth > 5) {
            throw new IllegalArgumentException("Illegal nth week of month: "
                                                + weekOfMonth);
        }
        this.weekOfMonth = weekOfMonth;
    }

    public void setReverseWeekOfMonth(int reverseWeekOfMonth) {
        if (reverseWeekOfMonth > -1 || reverseWeekOfMonth < -5) {
            throw new IllegalArgumentException("Illegal reverse nth week of month: "
                                                + weekOfMonth);
        }
        this.reverseWeekOfMonth = reverseWeekOfMonth;
    }

    public void setPreviousSpill(List<TimeRange> previousSpill) {
        this.previousSpill = previousSpill;
    }

    public void setStartWeekDay(WeekDay startWeekDay) {
        this.startWeekDay = startWeekDay;
    }

    public void setEndWeekDay(WeekDay endWeekDay) {
        if (startWeekDay == null) {
            throw new IllegalArgumentException("Start day has not been set");
        }
        if(startWeekDay.ordinal() > endWeekDay.ordinal()) {
            throw new IllegalArgumentException("Start weekday cannot be after end weekday");
        }
        this.endWeekDay = endWeekDay;
    }

    public int getYear() {
        return year;
    }

    public Month getMonth() {
        return month;
    }

    public int getWeekOfMonth() {
        return weekOfMonth;
    }

    public int getWeekOfYear() {
        return weekOfYear;
    }

    public int getReverseWeekOfMonth() {
        return reverseWeekOfMonth;
    }

    public List<TimeRange> getPreviousSpill() {
        return previousSpill;
    }

    public WeekDay getStartWeekday() {
        return startWeekDay;
    }

    public WeekDay getEndWeekDay() {
        return endWeekDay;
    }

    public WeekDayRule getStartWeekDayRule() {
        return weekDayStorage.get(startWeekDay);
    }

    public WeekDayRule getEndWeekDayRule() {
        return weekDayStorage.get(endWeekDay);
    }

    /**
     * Build Week with an input rule
     * 
     * @param isStrict strict or not
     * @throws OpeningHoursEvaluationException
     */
    public void build(Rule rule) throws OpeningHoursEvaluationException {
        build(rule, null);
    }

    /**
     * Build the WeekDayRule in this week with an input rule and a restriction
     * on what weekday this can apply. Used during build() of MonthRule()
     * 
     * @param rule an input rule
     * @param restriction a WeekDayRange restriction
     * @throws OpeningHoursEvaluationException
     */
    public void build(Rule rule, WeekDayRange restriction) throws OpeningHoursEvaluationException {
        applyPreviousSpill();
        update(rule, restriction);
        clean();
    }

    /**
     * Update Week with a rule
     * 
     * @param rule a Rule
     * @throws OpeningHoursEvaluationException
     */
    public void update(Rule rule, WeekDayRange restriction) throws OpeningHoursEvaluationException {
        List<WeekDayRange> weekdayRange;
        if (rule.getDays() != null) {
            weekdayRange = rule.getDays();
        } else {
            WeekDayRange allWeek = new WeekDayRange();
            allWeek.setStartDay(WeekDay.MO);
            allWeek.setEndDay(WeekDay.SU);
            weekdayRange = new ArrayList<>();
            weekdayRange.add(allWeek);
        }
        for (WeekDayRange weekdays : weekdayRange) {
            WeekDayRange processed = processRestriction(weekdays, restriction);
            if (processed != null) {
                if (weekdays.getOffset() == 0) {
                    updateWithRange(rule, processed);
                } else {
                    updateWithOffsetRange(rule, processed);
                }
            }
        }
    }

    /**
     * Process and return an applicable WeekDayRange after being processed
     * by the restriction. Return null if restriction wipes the whole range
     * 
     * @param range input WeekDayRange
     * @param restriction input restriction
     * @return applicable WeekDayRange after being processed by the restriction
     */
    private WeekDayRange processRestriction(WeekDayRange range, WeekDayRange restriction) {
        if (restriction == null) {
            return range;
        }
        WeekDayRange result = null;
        WeekDayRange otherResult = null;
        int startRange = range.getStartDay().ordinal();
        WeekDay endDay = range.getEndDay();
        int endRange;
        int startRes = restriction.getStartDay().ordinal();
        int endRes = restriction.getEndDay().ordinal();
        if (range.getEndDay() != null) {
            if (endDay.ordinal() >= startRange) {
                endRange = endDay.ordinal();
            } else {
                // handle week spilling
                endRange = WeekDay.SU.ordinal();
                List<Integer> overlap = Utils.getOverlap(WeekDay.MO.ordinal(),
                                            endDay.ordinal(), startRes, endRes);
                if (overlap != null) {
                    otherResult = range.copy();
                    otherResult.setStartDay(getWeekDayByNumber(overlap.get(0)));
                    otherResult.setEndDay(getWeekDayByNumber(overlap.get(1)));
                }
            }   
        } else {
            endRange = startRange;
        }
        List<Integer> overlap = Utils.getOverlap(startRange, endRange, 
                                                    startRes, endRes);
        if (overlap != null) {
            result = range.copy(); 
            result.setStartDay(getWeekDayByNumber(overlap.get(0)));
            result.setEndDay(getWeekDayByNumber(overlap.get(1)));
        }
        return helperMerge(result, otherResult);
    }

    /**
     * A very specific merge for two WeekDayRange. Used for connecting week
     * spilling.
     * <p>
     * For example, w1: Fr-Su, w2: Mo-Tu, output: Fr-Tu
     */
    private WeekDayRange helperMerge(WeekDayRange w1, WeekDayRange w2) {
        if (w1 == null) {
            return w2;
        }
        if (w2 == null) {
            return w1;
        }
        WeekDayRange result = w1.copy();
        if (w1.getEndDay() == WeekDay.SU) {
            result.setStartDay(w1.getStartDay());
            result.setEndDay(w2.getEndDay());
        } else { // assume that w2.getEndDay() == WeekDay.SU
            result.setStartDay(w2.getStartDay());
            result.setEndDay(w1.getEndDay());
        }
        return result;
    }

    /** update() helper 
     * @throws OpeningHoursEvaluationException
     */
    private void updateWithRange(Rule rule, WeekDayRange weekdays) throws OpeningHoursEvaluationException {
        List<Nth> nths = weekdays.getNths();
        WeekDay current = weekdays.getStartDay();
        WeekDay end = (weekdays.getEndDay() != null)
                        ? weekdays.getEndDay()
                        : current;
        do {
            if (hasWeekDay(current)
                    && weekDayStorage.get(current).isApplicableNth(nths)) {
                weekDayStorage.get(current).build(rule);
            }
        } while ((current = getNextWeekDay(current)) != getNextWeekDay(end));
    }

    /** update() helper, used for WeekDayRange with offset 
     * @throws OpeningHoursEvaluationException*/
    private void updateWithOffsetRange(Rule rule, WeekDayRange weekdays) throws OpeningHoursEvaluationException {
        WeekDay current = startWeekDay;
        WeekDay end = endWeekDay;
        do {
            if (weekDayStorage.get(current).isApplicableOffset(weekdays)) {
                weekDayStorage.get(current).build(rule);
            }
        } while ((current = getNextWeekDay(current)) != getNextWeekDay(end));
    }

    /**
     * @return true if input Rule can be applied to any day in the week
     */
    private boolean isUniversal(Rule rule) {
        return rule.isTwentyfourseven()
                || (rule.getDays() == null && rule.getTimes() == null);
    }

    /**
     * Evaluate the stored OH string with a time to see if it's opening or closed
     * 
     * @param time input LocalDateTime
     * @return the result of the evaluation
     */
    public Result checkStatus(LocalDateTime time) {
        WeekDay weekday = convertWeekDay(time.getDayOfWeek());
        WeekDayRule toCheck = weekDayStorage.get(weekday);
        if (toCheck == null) {
            return null;
        } else {
            return weekDayStorage.get(weekday).checkStatus(time);
        }
    }

    /**
     * Return next differing event of the input time (status different
     * from status of the evaluation of inputTime against the stored rules).
     * This assumes the input time is within the time of this Week
     * 
     * @param inputTime time to be checked
     * @param status the status that needs that the next event's status
     *      has to be different from
     * @return next differing event of the input time (status different from
     *      status of the evaluation of inputTime against the stored rules)
     */
    @Nullable
    Result getNextDifferingEventThisWeek(LocalDateTime inputTime, Status status) {
        WeekDay weekday = convertWeekDay(inputTime.getDayOfWeek());
        int time = Utils.timeInMinute(inputTime);
        WeekDayRule dayToCheck = weekDayStorage.get(weekday);
        TimeRange check = dayToCheck.getNextDifferingEventToday(time);
        return (check != null)
                ? processNextDifferingEvent(dayToCheck, check)
                : getNextDifferingEvent(dayToCheck.getNextDayRule(), status);
    }

    /**
     * Return next differing event of the input time (status different
     * from status of the evaluation of inputTime against the stored rules).
     * This starts looking from the WeekDayRule defined by start until it finds
     * a dummy WeekDayRule, which usually the end of the Week
     * 
     * @param start start WeekDayRule, search until dummy
     * @param status the status that needs that the next event's status
     *      has to be different from
     * @return next differing event of the input time (status different from
     *      status of the evaluation of inputTime against the stored rules)
     */
    @Nullable
    Result getNextDifferingEvent(WeekDayRule start, Status status) {
        while (!start.isDummy()) {
            TimeRange check = start.getNextDifferingEvent(status);
            if (check != null) {
                return processNextDifferingEvent(start, check);
            }
            start = start.getNextDayRule();
        }
        return null;
    }

    /**
     * Process to return a type of special Result containing open/close next
     * event, used in getting next differing event
     * 
     * @param day a desired WeekDayRule
     * @param timerange a desired TimeRange
     * @return Result that can be rad
     */
    private Result processNextDifferingEvent(WeekDayRule day, TimeRange timerange) {
        Result result = new Result(timerange);
        LocalDate date = day.getDefDate();
        LocalTime time = LocalTime.of(timerange.getStart() / 60, timerange.getStart() % 60);
        LocalDateTime nextEvent = LocalDateTime.of(date, time);
        result.setNextEventTime(nextEvent);
        return result;
    }

    /**
     * Check if this WeekDay is between this Week's start WeekDay and end WeekDay
     * 
     * @param weekday WeekDay to check
     * @return true if this WeekDay is between this Week's start WeekDay and end WeekDay, false otherwise
     */
    public boolean hasWeekDay(WeekDay weekday) {
        return DateManager.isBetweenWeekDays(weekday, startWeekDay, endWeekDay);
    }

    /** Clean all WeekDayRule in this Week */
    public void clean() {
        WeekDay current = startWeekDay;
        do {
            weekDayStorage.get(current).clean();
        } while ((current = getNextWeekDay(current))
                    != getNextWeekDay(endWeekDay));
    }

    /** Reset this Week by removing all current WeekDayRule and filling it with empty ones*/
    public void reset() {
        weekDayStorage = new EnumMap<>(WeekDay.class);
    }

    /** Populate this WeekRule with empty WeekDayRule. */
    public void populate() {
        populateHelper(startWeekDay);
        dayAfter = new WeekDayRule();
        dayAfter.setDummy(true);
        weekDayStorage.get(endWeekDay).setNextDayRule(dayAfter);
        if (previousSpill != null) {
            for (TimeRange spill : previousSpill) {
                weekDayStorage.get(startWeekDay).addSpill(spill);
            }
            // reset spill
            previousSpill = null;
        }
    }

    /** Helper of populate() */
    private void populateHelper(WeekDay current) {
        LocalDate dateOfCurrent = WeekManager.getWeekDayOfWeek(defDate, current);
        WeekDayRule newWeekDay = new WeekDayRule(dateOfCurrent);
        weekDayStorage.put(current, newWeekDay);
        WeekDay nextDay = getNextWeekDay(current);
        if(current != endWeekDay) {
            populateHelper(nextDay);
            weekDayStorage.get(current).setNextDayRule(weekDayStorage.get(nextDay));
        }
    }

    /**
     * Connect a partial Week to another partial week by setting the nextDayRule of 
     * the endWeekDay's WeekDayRule of this week to the startWeekDay's WeekDayRule of
     * the other Week
     * 
     * @param other the other Week to be connected
     */
    public void connect(Week other) {
        WeekDayRule nextStartDay = other.weekDayStorage.get(other.getStartWeekday());
        weekDayStorage.get(endWeekDay).setNextDayRule(nextStartDay);
    }

    /**
     * @return the spill of this Week
     */
    public List<TimeRange> getWeekSpill() {
        return dayAfter.getSpilledTime();
    }

    /**
     * Set the spill of the startWeekDay's WeekDayRule of this Week with any previous
     * spill, if any
     */
    public void applyPreviousSpill() {
        if (previousSpill != null) {
            weekDayStorage.get(startWeekDay).setSpilledTime(previousSpill);
            previousSpill = null;
        }
    }

    /**
     * Create and return a List<Week> created from an input date. The weekday data
     * is extracted from the week of input date. If there's a cutoff (a week between
     * months), List<Week> will cotain two
     * 
     * @param date a LocalDate to be built around
     * @return a List<Week> built around LocalDate
     */
    public static List<Week> createEmptyWeek(LocalDate date) {
        List<Week> result = new ArrayList<>();
        LocalDate firstDayOfWeek = WeekManager.getFirstDayOfWeek(date);
        LocalDate lastDayOfWeek = WeekManager.getLastDayOfWeek(date);
        LocalDate cutoffDate = null;
        // check for cutoff between months
        Week first = null;
        Week second = null;
        // handles when input week of date is split between previous and this month
        if (firstDayOfWeek.getMonth() != date.getMonth()) {
            cutoffDate = MonthRule.getLastDayOfMonth(firstDayOfWeek);
            WeekDay cutoff = convertWeekDay(cutoffDate.getDayOfWeek());
            first = new Week(cutoffDate, WeekDay.MO, cutoff);
            second = new Week(date, getNextWeekDay(cutoff), WeekDay.SU);
        // handles when input week of date is split between this and next month
        } else if (lastDayOfWeek.getMonth() != date.getMonth()) {
            cutoffDate = MonthRule.getFirstDayOfMonth(lastDayOfWeek);
            WeekDay cutoff = convertWeekDay(cutoffDate.getDayOfWeek());
            first = new Week(date, WeekDay.MO, getPreviousWeekDay(cutoff));
            second = new Week(cutoffDate, cutoff, WeekDay.SU);
        // handles when input week of date is wholly in a month
        } else {
            Week week = new Week(date);
            result.add(week);
            return result;
        }
        first.connect(second);
        result.add(first);
        result.add(second);
        return result;
    }

    public static WeekDay getWeekDayByNumber(int i) {
        return WeekDay.values()[i % 7];
    }

    /**
     * 
     * @param date
     * @return the week of year in which the input LocalDate is in
     */
    public static int getWeekOfYear(LocalDate date, Locale locale) {
        return date.get(WeekFields.of(locale).weekOfWeekBasedYear());

    }

    
    /**
     * Convert java.time.DayOfWeek enum to OpeningHoursParser.WeekDay enum
     * 
     * @param dayOfWeek an enum of DayOfWeek to be converted
     * @return an equivalent weekday in WeekDay enum
     */
    public static WeekDay convertWeekDay(DayOfWeek dayOfWeek) {
        return WeekDay.values()[dayOfWeek.ordinal()];
    }
    
    /**
     * Return the next WeekDay wrt a current WeekDay
     * 
     * @param current the current WeekDay
     * @return the following WeekDay
     */
    public static WeekDay getNextWeekDay(WeekDay current) {
        WeekDay[] weekdays = WeekDay.values();
        int next = (current.ordinal()+1) % weekdays.length;
        return weekdays[next];
    }

    /**
     * Return the previous WeekDay wrt a current WeekDay
     * 
     * @param current the current WeekDay
     * @return the previous WeekDay
     */
    public static WeekDay getPreviousWeekDay(WeekDay current) {
        WeekDay[] weekdays = WeekDay.values();
        int previous = (current.ordinal()-1 + weekdays.length) % weekdays.length;
        return weekdays[previous];
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        WeekDay current = startWeekDay;
        do {
            if (weekDayStorage.get(current) != null) {
                b.append(weekDayStorage.get(current));
            }
            b.append(Utils.LINE_SEPARATOR);
        } while ((current = getNextWeekDay(current)) != getNextWeekDay(endWeekDay));
        return b.toString();
    }

    /**
     * @return similar to toString(), but more debug info is printed out
     */
    public String toDebugString() {
        StringBuilder b = new StringBuilder();
        WeekDay current = startWeekDay;
        do {
            if (weekDayStorage.get(current) != null) {
                b.append(weekDayStorage.get(current).toDebugString());
            }
            b.append(Utils.LINE_SEPARATOR);
        } while ((current = getNextWeekDay(current)) != getNextWeekDay(endWeekDay));
        return b.toString();
    }
}
