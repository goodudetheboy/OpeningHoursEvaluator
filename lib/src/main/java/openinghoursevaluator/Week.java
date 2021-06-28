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
    public Week(LocalDate defDate,
                    WeekDay startWeekDay, WeekDay endWeekDay) {
        this.defDate = defDate;
        dissectDefDate(defDate);
        setStartWeekDay(startWeekDay);
        setEndWeekDay(endWeekDay);
        weekDayStorage = new EnumMap<>(WeekDay.class);
        populate();
    }

    /** Helper function for constructor */
    private void dissectDefDate(LocalDate defDate) {
        this.month = MonthRule.convertMonth(defDate);
        this.weekOfYear = defDate.get(WeekFields.of(Locale.FRANCE).weekBasedYear());
    }

    public void setWeekOfMonth(int weekOfMonth) {
        if (weekOfMonth < 1 || weekOfMonth > 5) {
            throw new IllegalArgumentException("Illegal nth week of month: " + weekOfMonth);
        }
        this.weekOfMonth = weekOfMonth;
    }

    public void setReverseWeekOfMonth(int reverseWeekOfMonth) {
        if (reverseWeekOfMonth > -1 || reverseWeekOfMonth < -5) {
            throw new IllegalArgumentException("Illegal reverse nth week of month: " + weekOfMonth);
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

    /**
     * Build Week with the current openingHours string
     * 
     * @param isStrict strict or not
     */
    // public void build() {
    //     populate();
    //     for (Rule rule : rules) {
    //         update(rule);
    //     }
    // }

    /**
     * Update Week with a rule
     * 
     * @param rule a Rule
     */
    public void update(Rule rule) {
        applyPreviousSpill();
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
            if (weekdays.getOffset() == 0) {
                updateWithRange(rule, weekdays);
            } else {
                updateWithOffsetRange(rule, weekdays);
            }
        }
        clean();
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
        return Utils.isBetweenWeekDays(weekday, startWeekDay, endWeekDay);
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
     * Used during MonthRule build(). This is to get the time spills of previous
     * week
     * 
     * @param week a Week to be simulated
     * @param rule a Rule to be applied
     * @return the simulated spill
     */
    public static List<TimeRange> simulateSpill(Week week, Rule rule) {
        LocalDate firstDateOfWeek = week.getStartWeekDayRule().getDefDate();
        LocalDate previousDay = WeekDayRule.getOffsetDate(firstDateOfWeek, -1);
        Week w = new Week(previousDay);
        w.update(rule);
        return w.getWeekSpill();
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
