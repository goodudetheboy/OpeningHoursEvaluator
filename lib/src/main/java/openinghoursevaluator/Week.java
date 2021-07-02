package openinghoursevaluator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

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
        this.weekOfYear = defDate.get(WeekFields.of(Locale.FRANCE).weekBasedYear());
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
     */
    public void build(Rule rule) {
        build(rule, null);
    }

    /**
     * Build the WeekDayRule in this week with an input rule and a restriction
     * on what weekday this can apply. Used during build() of MonthRule()
     * 
     * @param rule an input rule
     * @param restriction a WeekDayRange restriction
     */
    public void build(Rule rule, WeekDayRange restriction) {
        applyPreviousSpill();
        update(rule, restriction);
        clean();
    }

    /**
     * Update Week with a rule
     * 
     * @param rule a Rule
     */
    public void update(Rule rule, WeekDayRange restriction) {
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

    /** update() helper */
    private void updateWithRange(Rule rule, WeekDayRange weekdays) {
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

    /** update() helper, used for WeekDayRange with offset */
    private void updateWithOffsetRange(Rule rule, WeekDayRange weekdays) {
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
     * Check if this WeekDay is between this Week's start WeekDay and end WeekDay
     * 
     * @param weekday WeekDay to check
     * @return true if this WeekDay is between this Week's start WeekDay and end WeekDay, false otherwise
     */
    public boolean hasWeekDay(WeekDay weekday) {
        return DateManager.isBetweenWeekDays(weekday, startWeekDay, endWeekDay);
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

    /** Clean all WeekDayRule in this Week */
    public void clean() {
        for (WeekDay weekday : WeekDay.values()) {
            if(weekDayStorage.get(weekday) != null) {
                weekDayStorage.get(weekday).clean();
            }
        }
    }

    /** Reset this Week by removing all current WeekDayRule and filling it with empty ones*/
    public void reset() {
        weekDayStorage = new EnumMap<>(WeekDay.class);
    }

    /** Populate this WeekRule with empty WeekDayRule. */
    public void populate() {
        populateHelper(startWeekDay);
        dayAfter = new WeekDayRule();
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
        LocalDate dateOfCurrent = getWeekDayOfWeek(defDate, current);
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
        LocalDate firstDayOfWeek = getFirstDayOfWeek(date);
        LocalDate lastDayOfWeek = getLastDayOfWeek(date);
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

    /**
     * 
     * @param time the LocalDateTime of desired week
     * @return the LocalDate of the first weekday in the week of input date
     */
    public static LocalDate getFirstDayOfWeek(LocalDate date) {
        return getWeekDayOfWeek(date, WeekDay.MO);
    }

    /**
     * 
     * @param time the LocalDateTime of desired week
     * @return the LocalDate of the last weekday in the week of input date
     */
    public static LocalDate getLastDayOfWeek(LocalDate date) {
        return getWeekDayOfWeek(date, WeekDay.SU);
    }

    /**
     * 
     * @param date
     * @param weekday
     * @return the LocalDate of an input weekday in the week of input date
     */
    public static LocalDate getWeekDayOfWeek(LocalDate date, WeekDay weekday) {
        // ordinal starts with 0, so need this to make up for date.with()
        int n = weekday.ordinal() + 1;
        return date.with(WeekFields.ISO.dayOfWeek(), n);
    }

    public static WeekDay getWeekDayByNumber(int i) {
        return WeekDay.values()[i % 7];
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        WeekDay current = startWeekDay;
        do {
            if (weekDayStorage.get(current) != null) {
                b.append(weekDayStorage.get(current).toString());
            }
            b.append(System.getProperty("line.separator"));
        } while ((current = getNextWeekDay(current)) != getNextWeekDay(endWeekDay));
        return b.toString();
    }
}
