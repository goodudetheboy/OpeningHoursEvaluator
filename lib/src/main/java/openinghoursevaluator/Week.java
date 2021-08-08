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

    // geolocation, temporary put here for visibility
    Geolocation geolocation         = null;

    WeekDay startWeekDay            = null;
    WeekDay endWeekDay              = null;

    // used for connecting between days
    List<TimeRange> previousSpill   = null;
    WeekDayRule dayBefore           = null;
    WeekDayRule dayAfter            = null;
    
    // weekday storage
    EnumMap<WeekDay, WeekDayRule>   weekDayStorage = null;

    /**
     * Default constructor
     */
    public Week() {
        // nothing here
    }

    /**
     * Constructor to clone another Week
     * 
     * @param other the Week to clone
     */
    public Week(Week other) {
        this.defDate = other.defDate;
        this.year = other.year;
        this.month = other.month;
        this.weekOfYear = other.weekOfYear;
        this.geolocation = other.geolocation;
        this.startWeekDay = other.startWeekDay;
        this.endWeekDay = other.endWeekDay;
        this.previousSpill = other.previousSpill;
        this.dayBefore = other.dayBefore;
        this.dayAfter = other.dayAfter;
        this.weekDayStorage = other.weekDayStorage;
    }

    /**
     * Constructor for a Week with defining date and a geolocation. The start
     * and end week will be set to MO and SU, respectively.
     * 
     * @param defDate a defining date
     * @param geolocation a geolocation
     */
    public Week(LocalDate defDate, Geolocation geolocation) {
        // setting weekOfYear and weekOfMonth
        this(defDate, WeekDay.MO, WeekDay.SU, geolocation);
    }

    /**
     * Constructor for a Week with defining date, a sole weekday, a geolocation.
     * The resulting instance will contain only the WeekDayRule of the specified
     * WeekDay.
     * 
     * @param defDate a defining date
     * @param oneWeekDay a sole weekday
     * @param geolocation a geolocation
     */
    public Week(LocalDate defDate, WeekDay oneWeekDay, Geolocation geolocation) {
        this(defDate, oneWeekDay, oneWeekDay, geolocation);
    }

    /**
     * A constructor for a Week with defining date, start/end weekday, and a
     * geolocation
     * 
     * @param defDate a defining date
     * @param startWeekDay a start weekday
     * @param endWeekDay a end weekday
     * @param geolocation a geolocation
     */
    public Week(LocalDate defDate, WeekDay startWeekDay, WeekDay endWeekDay, Geolocation geolocation) {
        setGeolocation(geolocation);
        this.defDate = defDate;
        dissectDefDate(defDate);
        setStartWeekDay(startWeekDay);
        setEndWeekDay(endWeekDay);
        weekDayStorage = new EnumMap<>(WeekDay.class);
        populate();
    }

    /**
     * Constructs a Week wrapper for a single WeekDayRule
     * 
     * @param weekDayRule WeekDayRule 
     */
    Week (WeekDayRule weekDayRule, Geolocation geolocation) {
        this(weekDayRule.getDefDate(), weekDayRule.getWeekDay(), geolocation);
        weekDayStorage.put(weekDayRule.getWeekDay(), weekDayRule);
    }

    /** Helper function for constructor */
    private void dissectDefDate(LocalDate defDate) {
        this.year = defDate.getYear();
        this.month = MonthRule.convertMonth(defDate);
        this.weekOfYear = getWeekOfYear(defDate, geolocation.getLocale());
    }

    /**
     * @return the year of this Week
     */
    public int getYear() {
        return year;
    }

    /**
     * @return the Month of this Week
     */
    public Month getMonth() {
        return month;
    }

    /**
     * @return the Week number of stored Year
     */
    public int getWeekOfYear() {
        return weekOfYear;
    }

    /**
     * @return geolocation stored in this Week
     */
    public Geolocation getGeocoder() {
        return geolocation;
    }

    /**
     * @return the previous time spill of this Week
     */
    public List<TimeRange> getPreviousSpill() {
        return previousSpill;
    }

    /**
     * @return the start WeekDay of this Week
     */
    public WeekDay getStartWeekday() {
        return startWeekDay;
    }

    /**
     * @return the end WeekDay of this Week
     */
    public WeekDay getEndWeekDay() {
        return endWeekDay;
    }

    /**
     * @return the start WeekDayRule of this Week
     */
    public WeekDayRule getStartWeekDayRule() {
        return weekDayStorage.get(startWeekDay);
    }

    /**
     * @return the end WeekDayRule of this Week
     */
    public WeekDayRule getEndWeekDayRule() {
        return weekDayStorage.get(endWeekDay);
    }

    /**
     * @return the upper limit WeekDayRule of this Week
     */
    public WeekDayRule getDayAfter() {
        return dayAfter;
    }

    /**
     * @return the lower limit WeekDayRule of this Week
     */
    public WeekDayRule getDayBefore() {
        return dayBefore;
    }

    /**
     * Set the year of this Week
     * 
     * @param year year to be set
     */
    public void setYear(int year) {
        this.year = year;
    }

    /**
     * Set the month of this Week
     * 
     * @param month Month to be set
     */
    public void setMonth(Month month) {
        this.month = month;
    }

    /**
     * Set the week of Year of this Week
     * 
     * @param weekOfYear week of year to be set
     */
    public void setWeekOfYear(int weekOfYear) {
        this.weekOfYear = weekOfYear;
    }

    /**
     * Set the geolocation of this Week
     * 
     * @param geolocation double array {latitude, longitude}
     */
    public void setGeolocation(Geolocation geolocation) {
        this.geolocation = geolocation;
    }

    /**
     * Set the previousSpill of this Week
     * 
     * @param previousSpill previousSpill to be set
     */
    public void setPreviousSpill(List<TimeRange> previousSpill) {
        this.previousSpill = previousSpill;
    }

    /**
     * Set the start WeekDay of this Week
     * 
     * @param startWeekDay startWeekDay to be set
     */
    public void setStartWeekDay(WeekDay startWeekDay) {
        this.startWeekDay = startWeekDay;
    }

    /**
     * Set the end WeekDay of this Week
     * 
     * @param endWeekDay endWeekDay to be set
     */
    public void setEndWeekDay(WeekDay endWeekDay) {
        if (startWeekDay == null) {
            throw new IllegalArgumentException("Start day has not been set");
        }
        if(startWeekDay.ordinal() > endWeekDay.ordinal()) {
            throw new IllegalArgumentException("Start weekday cannot be after end weekday");
        }
        this.endWeekDay = endWeekDay;
    }

    /**
     * Set the dayBefore of this Week
     * 
     * @param dayBefore dayBefore to be set
     */
    public void setDayBefore(WeekDayRule dayBefore) {
        this.dayBefore = dayBefore;
    }

    /**
     * Set the dayAfter of this Week
     * 
     * @param dayAfter dayAfter to be set
     */
    public void setDayAfter(WeekDayRule dayAfter) {
        this.dayAfter = dayAfter;
    }    

    /**
     * Add a WeekDayRule to this Week. If there is any WeekDayRule already
     * present in same WeekDay of WeekDayRule, the new WeekDayRule will replace
     * the old WeekDayRule
     * 
     * @param wdr weekDa
     */
    public void addWeekDayRule(WeekDayRule wdr) {
        WeekDayRule old = weekDayStorage.remove(wdr.getWeekDay());
        if (old != null) {
            wdr.setLastDayRule(old.getLastDayRule());
            wdr.setNextDayRule(old.getNextDayRule());
        }
        weekDayStorage.put(wdr.getWeekDay(), wdr);
    }

    /**
     * Build Week with an input rule
     * 
     * @param rule a Rule
     * @throws OpeningHoursEvaluationException when there's problem during evaluation
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
     * @throws OpeningHoursEvaluationException when there's problem during evaluation
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
     * @param restriction a WeekDayRange restriction
     * @throws OpeningHoursEvaluationException when there's problem during evaluation
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
            // restriction taken from previous YearRange, WeekRange, DateRange
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
                    otherResult.setStartDay(getWeekDayByInt(overlap.get(0)));
                    otherResult.setEndDay(getWeekDayByInt(overlap.get(1)));
                }
            }   
        } else {
            endRange = startRange;
        }
        List<Integer> overlap = Utils.getOverlap(startRange, endRange, 
                                                    startRes, endRes);
        if (overlap != null) {
            result = range.copy(); 
            result.setStartDay(getWeekDayByInt(overlap.get(0)));
            result.setEndDay(getWeekDayByInt(overlap.get(1)));
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
    private void updateWithRange(Rule rule, WeekDayRange weekdays)
            throws OpeningHoursEvaluationException {
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
     * @throws OpeningHoursEvaluationException
     */
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
     * @param isNext true to get next differing event, false to get last
     * @return next differing event of the input time (status different from
     *      status of the evaluation of inputTime against the stored rules)
     */
    @Nullable
    Result getDifferingEventThisWeek(LocalDateTime inputTime, Status status, boolean isNext) {
        WeekDay weekday = convertWeekDay(inputTime.getDayOfWeek());
        int time = Utils.timeInMinute(inputTime);
        WeekDayRule dayToCheck = weekDayStorage.get(weekday);
        TimeRange check = dayToCheck.getDifferingEventToday(time, isNext);
        if (check != null) {
            return processDifferingEvent(dayToCheck, check, isNext);
        } else {
            WeekDayRule toCheck = (isNext) ? dayToCheck.getNextDayRule() 
                                           : dayToCheck.getLastDayRule();
            return getDifferingEvent(toCheck, status, isNext); 
        }
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
     * @param isNext true to look next differing event, false otherwise
     * @return next differing event of the input time (status different from
     *      status of the evaluation of inputTime against the stored rules)
     */
    @Nullable
    Result getDifferingEvent(WeekDayRule start, Status status, boolean isNext) {
        while (!start.isDummy()) {
            TimeRange check = start.getDifferingEvent(status, isNext);
            if (check != null) {
                return processDifferingEvent(start, check, isNext);
            }
            start = (isNext) ? start.getNextDayRule() : start.getLastDayRule();
        }
        return null;
    }

    /**
     * Process to return a type of special Result containing open/close next
     * event, used in getting next differing event
     * 
     * @param day a desired WeekDayRule
     * @param timerange a desired TimeRange
     * @param isNext true if look for next differing event, false otherwise
     * @return Result that can be rad
     */
    private Result processDifferingEvent(WeekDayRule day, TimeRange timerange, boolean isNext) {
        Result result = new Result(timerange);
        LocalDate date = day.getDefDate();
        int timestamp;
        if (isNext) {
            timestamp = timerange.getStart();
        } else {
            timestamp = (timerange.getEnd() != TimeRange.MAX_TIME)
                        ? timerange.getEnd()
                        : timerange.getEnd() - 1;
            // to prevent Invalid value for HourOfDay exception
        }
        LocalTime time = LocalTime.of(timestamp / 60, timestamp % 60);
        LocalDateTime differingEvent = LocalDateTime.of(date, time);

        if (isNext) {
            result.setNextEventTime(differingEvent);
        } else {
            result.setLastEventTime(differingEvent);
        }
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
        WeekDayRule current = weekDayStorage.get(startWeekDay);
        WeekDay target = getNextWeekDay(endWeekDay);
        do {
            current.clean();
            current = current.getNextDayRule();
        } while (current.getWeekDay() != target);
    }

    /**
     * Reset this Week by removing all current WeekDayRule and filling it with
     * empty ones
     * 
     */
    public void reset() {
        weekDayStorage = new EnumMap<>(WeekDay.class);
    }

    /**
     * Populate this WeekRule with empty WeekDayRule.
     * 
     */
    public void populate() {
        populateHelper(startWeekDay);
        // set previous bound
        WeekDayRule startWeekRule = getStartWeekDayRule();
        LocalDate dateBefore 
            = DateManager.getOffsetDate(startWeekRule.getDefDate(), -1);
        dayBefore = new WeekDayRule(dateBefore, geolocation);
        dayBefore.setDummy(true);
        startWeekRule.setLastDayRule(dayBefore);
        dayBefore.setNextDayRule(startWeekRule);

        // set next bound
        WeekDayRule endWeekRule = getEndWeekDayRule();
        LocalDate dateAfter 
            = DateManager.getOffsetDate(endWeekRule.getDefDate(), 1);
        dayAfter = new WeekDayRule(dateAfter, geolocation);
        dayAfter.setDummy(true);
        endWeekRule.setNextDayRule(dayAfter);

        // apply previous spill
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
        WeekDayRule newWeekDay = new WeekDayRule(dateOfCurrent, geolocation);
        weekDayStorage.put(current, newWeekDay);

        // create next weekday rule
        WeekDay nextDay = getNextWeekDay(current);
        if(current != endWeekDay) {
            populateHelper(nextDay);
            newWeekDay.setNextDayRule(weekDayStorage.get(nextDay));
            weekDayStorage.get(nextDay).setLastDayRule(newWeekDay);
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
        WeekDayRule nextStartDay = other.getStartWeekDayRule();
        WeekDayRule lastEndDay = getEndWeekDayRule();
        lastEndDay.setNextDayRule(nextStartDay);
        nextStartDay.setLastDayRule(lastEndDay);
        setDayAfter(nextStartDay);
        other.setDayBefore(lastEndDay);
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
     * Create and return a List Week created from an input date. The weekday data
     * is extracted from the week of input date. If there's a cutoff (a week between
     * months), List Week will cotain two
     * 
     * @param date a LocalDate to be built around
     * @param geolocation a geolocation {latidue, longitude} where the Week is based
     *      around
     * @return a List Week built around LocalDate
     */
    public static List<Week> createEmptyWeek(LocalDate date, Geolocation geolocation) {
        List<Week> result = new ArrayList<>();
        LocalDate firstDayOfWeek = WeekManager.getFirstDayOfWeek(date);
        LocalDate lastDayOfWeek = WeekManager.getLastDayOfWeek(date);
        LocalDate cutoffDate = null;
        // check for cutoff between months
        Week first = null;
        Week second = null;
        if (firstDayOfWeek.getMonth() != date.getMonth()) {
            // handles when input week of date is split between previous and this month
            cutoffDate = MonthRule.getLastDayOfMonth(firstDayOfWeek);
            WeekDay cutoff = convertWeekDay(cutoffDate.getDayOfWeek());
            first = new Week(cutoffDate, WeekDay.MO, cutoff, geolocation);
            second = new Week(date, getNextWeekDay(cutoff), WeekDay.SU, geolocation);

        } else if (lastDayOfWeek.getMonth() != date.getMonth()) {
            // handles when input week of date is split between this and next month
            cutoffDate = MonthRule.getFirstDayOfMonth(lastDayOfWeek);
            WeekDay cutoff = convertWeekDay(cutoffDate.getDayOfWeek());
            first = new Week(date, WeekDay.MO, getPreviousWeekDay(cutoff), geolocation);
            second = new Week(cutoffDate, cutoff, WeekDay.SU, geolocation);

        } else {
            // handles when input week of date is wholly in a month
            Week week = new Week(date, geolocation);
            result.add(week);
            return result;
        }
        first.connect(second);
        result.add(first);
        result.add(second);
        return result;
    }

    /**
     * Convert an integer from 0-6 that corresponds to weekday of a week, with
     * Monday being the start of the week
     * 
     * @param i an integer from 0-6 that corresponds to weekday of a week
     * @return a WeekDay corresponding to the integer
     */
    public static WeekDay getWeekDayByInt(int i) {
        return WeekDay.values()[i % 7];
    }

    /**
     * Calculates the week of year of input date w.r.t. a Locale.
     * 
     * @param date a date to find the week of year of
     * @param locale a Locale
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
