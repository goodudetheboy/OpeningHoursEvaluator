package openinghoursevaluator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.shredzone.commons.suncalc.SunTimes;

import ch.poole.openinghoursparser.Month;
import ch.poole.openinghoursparser.Nth;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.TimeSpan;
import ch.poole.openinghoursparser.VariableTime;
import ch.poole.openinghoursparser.WeekDay;
import ch.poole.openinghoursparser.WeekDayRange;

public class WeekDayRule {
    public static final int INVALID_NUM = Integer.MIN_VALUE;

    // store the defining LocalDate of this WeekDayRule
    LocalDate       defDate         = null;

    // used used during eval
    int             year            = INVALID_NUM;
    Month           month           = null;
    int             date            = INVALID_NUM;
    WeekDay         weekday         = null;
    int             nthWeekDay      = INVALID_NUM;
    int             reverseNth      = INVALID_NUM;

    // used for connecting between days
    WeekDayRule     nextDayRule     = null;
    WeekDayRule     lastDayRule     = null;
    List<TimeRange> yesterdaySpill  = null;

    // opening times storage
    List<TimeRange> openingTimes    = null;

    // used when need to traverse through a Week, and also in creation of Week
    boolean         isDummy         = false;

    private boolean isFallbackLast = false;

    /** Default constructor, setting current to null and weekday to Monday */
    public WeekDayRule() {
        // nothing here
    }
    
    /** Constructor with Weekday */    
    public WeekDayRule(LocalDate defDate) {
        this.defDate = defDate;
        dissectDefDate(defDate);
        openingTimes = new ArrayList<>();
        yesterdaySpill = new ArrayList<>();
    }

    /**
     * Helper function for constructor, used for gathering info from
     * defining date
     */
    private void dissectDefDate(LocalDate defDate) {
        year = defDate.getYear();
        month = MonthRule.convertMonth(defDate);
        date = defDate.getDayOfMonth();
        weekday = Week.convertWeekDay(defDate.getDayOfWeek());
        nthWeekDay = getNthWeekDayOfMonth(defDate);
        reverseNth = getReverseNthWeekOfMonth(defDate);
    }

    /**
     * @return the current weekday of this weekday
     */
    public WeekDay getWeekDay() {
        return weekday;
    }

    /**
     * @return the next day rule
     */
    public WeekDayRule getNextDayRule() {
        return nextDayRule;
    }

    /**
     * @return the last day rule
     */
    public WeekDayRule getLastDayRule() {
        return lastDayRule;
    }

    /**
     * @return the time spill from previous day
     */
    public List<TimeRange> getSpilledTime() {
        return yesterdaySpill;
    }

    /**
     * @return the defining LocalDate upon which this WeeDayRule is built
     */
    public LocalDate getDefDate() {
        return defDate;
    }

    /**
     * A dummy WeekDayRule is used for a Week, where there is a need to set a
     * nextDayRule to the endWeekDay's WeekDayRule
     * 
     * @return check if this Week is dummy, useful during traversal
     */
    public boolean isDummy() {
        return isDummy;
    }

    /**
     * Set the current weekday of this weekday
     * 
     * @param weekday weekday to be set current
     */
    public void setWeekDay(WeekDay weekday) {
        this.weekday = weekday;
    }

    /**
     * Set the next WeekDayRule of this WeekDayRule
     * 
     * @param nextDayRule WeekDayRule to be set next
     */
    public void setNextDayRule(WeekDayRule nextDayRule) {
        this.nextDayRule = nextDayRule;
    }

    /**
     * Set the last WeekDayRule of this WeekDayRule
     * 
     * @param lastDayRule WeekDayRule to be set last
     */
    public void setLastDayRule(WeekDayRule lastDayRule) {
        this.lastDayRule = lastDayRule;
    }

    /**
     * Set this WeekDayRule's time spill with yesterdaySpill
     * 
     * @param yesterdaySpill time spill to be set
     */
    public void setSpilledTime(List<TimeRange> yesterdaySpill) {
        this.yesterdaySpill = yesterdaySpill;
    }

    /**
     * @param defDate set the defining date of this WeekDayRule
     */
    public void setDefiningDate(LocalDate defDate) {
        this.defDate = defDate;
    }

    /**
     * A dummy WeekDayRule is used for a Week, where there is a need to set a
     * nextDayRule to the endWeekDay's WeekDayRule
     * 
     * @param isDummy set this as a dummy WeekDayRule, useful during
     *      Week traversal
     */
    public void setDummy(boolean isDummy) {
        this.isDummy = isDummy;
    }

    /**
     * Build the opening times of this weekday with a rule. A new Rule will
     * override (clear all current opening hours in this day) when all of the
     * below condition is all satisfied:
     * <ol>
     * <li> it is a normal Rule (which means it is neither additive nor fallback)
     * <li> its status is not closed
     * </ol>
     * <p>
     * Check the links below to learn more about when this does not clear/
     * override current day
     * 
     * <p>
     * Another feature is that any additive Rule followed by a fallback Rule is
     * considered also a fallback Rule (it will be added as a fallback instead
     * of an additive)
     * 
     * @param rule Rule to be used in building
     * @throws OpeningHoursEvaluationException
     * @see https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification#explain:rule_modifier:closed
     * @see https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification#explain:additional_rule_separator
     * @see https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification#explain:fallback_rule_separator
     */
    public void build(Rule rule, Geolocation geolocation) throws OpeningHoursEvaluationException {
        if (rule.isEmpty()) {
            throw new OpeningHoursEvaluationException("There's an empty rule, please remove it");
        }
        if (!rule.isAdditive() && !rule.isFallBack()
                && Status.convert(rule.getModifier()) != Status.CLOSED) {
            clearOpeningHours();
        }
        flushSpill();
        addRule(rule, geolocation);
    }

    /**
     * A soft version of build(), where opening rule does not overwrite
     * current day. This also doesn't apply any time spill from previous day
     * 
     * @param rule Rule to be added
     * @throws OpeningHoursEvaluationException
     */
    public void addRule(Rule rule, Geolocation geolocation) throws OpeningHoursEvaluationException {
        String comment = (rule.getModifier() != null)
                            ? rule.getModifier().getComment()
                            : null;
        Status status = Status.convert(rule.getModifier());
        boolean isFallback = rule.isFallBack() || (rule.isAdditive() && isFallbackLast);

        if (rule.isTwentyfourseven() || rule.getTimes() == null) {
            TimeSpan allDay = new TimeSpan();
            allDay.setStart(0);
            allDay.setEnd(1440);
            addTime(allDay, status, comment, rule, isFallback);
            return;
        }
        for (TimeSpan timespan : rule.getTimes()) {
            TimeSpan processed = processEventOfDay(timespan, geolocation);
            addTime(processed, status, comment, rule, isFallback);
        }
    }

    /**
     * Process in the case the TimeSpan has events of day (dawn, dusk, sunrise,
     * sunset) defined as one the points.
     * 
     * @param timespan TimeSpan to be processed
     * @param geolocation geolocation {latitude, longitude}
     * @return processed TimeSpan with start and end well-defined and filled
     *      time of events of day, if any
     * @throws OpeningHoursEvaluationException
     */
    private TimeSpan processEventOfDay(TimeSpan timespan, Geolocation geolocation) 
            throws OpeningHoursEvaluationException {
        TimeSpan processed = timespan.copy();
        VariableTime startEvent = timespan.getStartEvent();
        VariableTime endEvent = timespan.getEndEvent();
        int startEventTime = (startEvent != null)
                        ? getTimeOfEvent(startEvent, geolocation, 0)
                        : timespan.getStart();
        int endEventTime = (endEvent != null)
                        ? getTimeOfEvent(endEvent, geolocation, 0)
                        : timespan.getEnd();
        // error handling
        checkErrorTimeSpan(startEventTime, endEventTime, timespan);
        // end event time padding, in case like (sunset-sunset)
        if (endEvent != null && endEventTime < startEventTime) {
            endEventTime = getTimeOfEvent(endEvent, geolocation, 1) + TimeRange.MAX_TIME;
        }
        processed.setStart(startEventTime);
        processed.setEnd(endEventTime);
        return processed;
    }

    /**
     * Get time in minutes of event of day defined in varTime, offset included
     * The returned time is plus 1 minutes compared to the calculated time
     * 
     * @param varTime VariableTime
     * @param geolocation 
     * @return time in minutes of event specified in VariableTime
     */
    private int getTimeOfEvent(VariableTime varTime, Geolocation geolocation, int dateOffset) {
        int option = 1;
        SunTimes events = null;
        ZoneId zoneId = geolocation.getTimeZone();
        LocalDate adjusted = DateManager.getOffsetDate(defDate, dateOffset);
        ZonedDateTime zonedDate = adjusted.atStartOfDay(zoneId);
        double[] coor = geolocation.getCoordinates();
        switch (varTime.getEvent()) {
            case SUNRISE:
                option = 0;
            case SUNSET:
                events = SunTimes.compute()
                    .on(zonedDate)   // set a date
                    .at(coor[0], coor[1])   // set a location
                    .execute();     // get the results
                break;
            case DAWN:
                option = 0;
            case DUSK:
                events = SunTimes.compute()
                    .twilight(SunTimes.Twilight.CIVIL)
                    .on(zonedDate)
                    .at(coor[0], coor[1])
                    .execute();
                break;
        }
        if (events == null) {
            throw new IllegalArgumentException("Event of day calculator not initialized, unexpected");
        }
        ZonedDateTime time = (option == 1) ? events.getSet()
                                           : events.getRise();
        return Utils.timeInMinute(time.toLocalDateTime()) + varTime.getOffset() + 1;
    }

    /**
     * Error handling for checking variable time
     * 
     * @param startTime start time of a TimeSpan
     * @param endTime end time of a TimeSpan
     * @throws OpeningHoursEvaluationException
     */
    private void checkErrorTimeSpan(int startTime, int endTime, TimeSpan timespan)
            throws OpeningHoursEvaluationException {
        // check if start time less than 0
        if (startTime < TimeRange.MIN_TIME) {
            throw new OpeningHoursEvaluationException("Start time of "
                                + timespan + " cannot be less than 0");
        }
        // check if end time has
        if (endTime == TimeSpan.UNDEFINED_TIME) {
            return;
        }
        // check end time less than 0
        if (endTime < TimeRange.MIN_TIME) {
            throw new OpeningHoursEvaluationException("End time of "
                                + timespan + " cannot be less than 0");
        }
    }

    /**
     * Check if input weekOfMonth is within Nth range. Supports negative nth
     * 
     * @param nth input Nth
     * @param weekOfMonth week of month (max 5)
     * @return if applicable or not
     * @throws OpeningHoursEvaluationException
     */
    public boolean isApplicableNth(List<Nth> nths) throws OpeningHoursEvaluationException {
        if (nths == null) {
            return true;
        }
        for (Nth nth : nths) {
            int startNth = nth.getStartNth();
            int endNth = nth.getEndNth();
            if (endNth == Nth.INVALID_NTH) {
                if ((startNth > 0 && nthWeekDay == startNth) // positive Nth
                        || (startNth < 0 && reverseNth == startNth)) { // negative Nth
                    return true;
                }
            } else {
                checkNthError(startNth, endNth);
                if (Utils.isBetween(nthWeekDay, startNth, endNth)
                        || Utils.isBetween(reverseNth, startNth, endNth)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper for isApplicableNth(). Check for illegal range in an Nth 
     * 
     * @param startNth start Nth number
     * @param endNth end Nth number
     * @throws OpeningHoursEvaluationException
     */
    private void checkNthError(int startNth, int endNth)
            throws OpeningHoursEvaluationException {
        if (endNth < startNth) {
            throw new OpeningHoursEvaluationException("Start nth "
                + startNth + " cannot be less than " + endNth);
        } else if (startNth < 0 && endNth > 0) {
            throw new OpeningHoursEvaluationException("Illegal range: "
                + startNth + " - " + endNth);
        }
    }

    /**
     * Check if this WeekDayRule is affected by any day offset
     * 
     * @return
     * @throws OpeningHoursEvaluationException
     */
    public boolean isApplicableOffset(WeekDayRange weekdays)
            throws OpeningHoursEvaluationException {
        checkOffsetError(weekdays);
        int offset = weekdays.getOffset();
        LocalDate offsetDate = DateManager.getOffsetDate(defDate, -offset);
        WeekDayRule offsetDay = new WeekDayRule(offsetDate);
        WeekDay offsetWeekDay = offsetDay.getWeekDay();
        return offsetWeekDay == weekdays.getStartDay()
                && offsetDay.isApplicableNth(weekdays.getNths());
    }

    /**
     * Helper for isApplicableOffset(). Check for illegal offset with respect
     * to a WeekDayRange
     * 
     * @param weekdays a WeekDayRange
     * @throws OpeningHoursEvaluationException
     */
    private void checkOffsetError(WeekDayRange weekdays)
            throws OpeningHoursEvaluationException {
        int offset = weekdays.getOffset();
        if (offset == 0) {
            throw new OpeningHoursEvaluationException("Offset not 0 should have been checked before this function is called");
        } else if (weekdays.getEndDay() != null) {
            throw new OpeningHoursEvaluationException("There cannot be a end day in a WeekDayRange with an offset");
        }
    }

    /**
     * Add a TimeSpan with a Status and an optional comment to the current
     * WeekDayRule
     * 
     * @param timespan TimeSpan to add
     * @param status desired Status
     * @param comment optional comment
     * @param isFallback if time added is fallback rule
     * @throws OpeningHoursEvaluationException
     */
    public void addTime(TimeSpan timespan, Status status, String comment,
                        Rule defRule, boolean isFallback)
            throws OpeningHoursEvaluationException {
        if (timespan.getInterval() != 0) {
            addInterval(timespan, status, comment, defRule, isFallback);
        } else if (timespan.isOpenEnded()) {
            addOpenEnd(timespan, status, comment, defRule, isFallback);
        } else {
            addTime(timespan.getStart(), timespan.getEnd(), status, comment, defRule, isFallback);
        }
    }

    /** Helper function to add TimeSpan with interval to this WeekDayRule */
    private void addInterval(TimeSpan timespan, Status status, String comment,
                            Rule defRule, boolean isFallback) {
        int interval = timespan.getInterval();
        int current = timespan.getStart();
        int end = timespan.getEnd();
        TimeRange timepoint = null;
        do {
            timepoint = new TimeRange(current, status, comment);
            timepoint.setDefiningRule(defRule);
            addTime(timepoint, isFallback);
            current = current + interval;
        } while (current <= end && current < TimeRange.MAX_TIME);
        while(current <= end) {
            timepoint = new TimeRange(current - TimeRange.MAX_TIME, status, comment);
            timepoint.setDefiningRule(defRule);
            timepoint.setFallback(isFallback);
            nextDayRule.addSpill(timepoint);
            current = current + interval;
        }
    }

    /** 
     * Helper function to add TimeSpan with open end to this WeekDayRule.
     * <p>
     * Open-ended time is evaluated similar to the opening_hours.js (the javascript
     * version of the evaluator), according to the following rule (extracted from
     * README.md of opening_hours.js):
     * <ul>
     * <li>Open end applies until the end of the day if the opening time is before 17:00.
     * If the opening time is between 17:00 and 21:59 the open end time ends 10 hours after
     * the opening. And if the opening time is after 22:00 (including 22:00) the closing
     * time will be interpreted as 8 hours after the opening time.
     * </ul>
     * <p>
     * @throws OpeningHoursEvaluationException
     * @see https://github.com/opening-hours/opening_hours.js#time-ranges, open-ended time section
     * */
    private void addOpenEnd(TimeSpan timespan, Status status, String comment,
                            Rule defRule, boolean isFallback)
            throws OpeningHoursEvaluationException {
        int openEndStart = 0;
        if (timespan.getEnd() != TimeSpan.UNDEFINED_TIME) {
            openEndStart = timespan.getEnd();
            addTime(timespan.getStart(), timespan.getEnd(),
                status, comment, defRule, isFallback);
        } else {
            openEndStart = timespan.getStart();
        }
        String openEndedComment = (comment == null)
                                    ? TimeRange.DEFAULT_OPEN_ENDED_COMMENT
                                    : comment;
        Status openEndedStatus = (status == Status.CLOSED)
                                    ? status
                                    : Status.UNKNOWN;
        // for start of open ended time in the next day
        int nextDayStart = openEndStart - TimeRange.MAX_TIME;
        if (nextDayStart < 0) {
            TimeRange time = new TimeRange(openEndStart, TimeRange.MAX_TIME,
                                    openEndedStatus, openEndedComment);
            time.setDefiningRule(defRule);
            addTime(time, isFallback);
            int firstCutOff = 17*60;
            int secondCutOff = 22*60;
            if (openEndStart >= firstCutOff) {
                int timespill = openEndStart - TimeRange.MAX_TIME;
                timespill += (openEndStart >= secondCutOff) ? 8*60 : 10*60;
                TimeRange spill
                    = new TimeRange(0, timespill, openEndedStatus, openEndedComment);
                spill.setDefiningRule(defRule);
                spill.setFallback(isFallback);
                nextDayRule.addSpill(spill);
            }
        } else if (nextDayStart >= TimeRange.MAX_TIME + 17*60) {
            throw new OpeningHoursEvaluationException("Time spanning more than two days not supported");
        } else {
            TimeRange extra = new TimeRange(nextDayStart, nextDayStart + 8*60,
                                    openEndedStatus, openEndedComment);
            extra.setDefiningRule(defRule);
            extra.setFallback(isFallback);
            nextDayRule.addSpill(extra);
        }
    }

    /**
     * Add the time with the specified start and end into this WeekDayRule, along with
     * a Status and an optional comment. This supports time spilling (end > 24:00)
     * 
     * @param start start time (must be < 24:00 AKA 1440 and also less than end time)
     * @param end end time
     * @param status desired Status
     * @param comment optional comment
     * @throws OpeningHoursEvaluationException
     */
    public void addTime(int start, int end, Status status, String comment,
                        Rule defRule, boolean isFallback) 
                    throws OpeningHoursEvaluationException {
        if (start > TimeRange.MAX_TIME ||  start < TimeRange.MIN_TIME) {
            throw new OpeningHoursEvaluationException("Start time must be within 24 hours");
        }
        int endToday = end; // for the current day
        if (end != TimeSpan.UNDEFINED_TIME) {
            int timespill = end - TimeRange.MAX_TIME;
            if (timespill > 0) {
                endToday = TimeRange.MAX_TIME;
                TimeRange spill = new TimeRange(0, timespill, status, comment);
                spill.setDefiningRule(defRule);
                spill.setFallback(isFallback);
                nextDayRule.addSpill(spill);
            }
        } else {
            endToday = start;
        }
        TimeRange time = new TimeRange(start, endToday, status, comment);
        time.setDefiningRule(defRule);
        addTime(time, isFallback);
    }

    /**
     * Add a TimeRange to this WeekDayRule. As per the specifications of TimeRange,
     * this does not support time spilling (end > 24:00).
     * An additional setting is whether to add as fallback. Adding fallback means the
     * input TimeRange will cut only TimeRange with Status.CLOSED, if any
     * 
     * @param timerange TimeRange to add
     * @param isFallback soft or hard
     */
    public void addTime(TimeRange timerange, boolean isFallback) {
        if (isFallback) {
            addFallback(timerange);
        } else {
            isFallbackLast = false;
            addTime(timerange);
        }
    }

    /**
     * Add a TimeRange to this WeekDayRule
     * 
     * @param timerange
     */
    public void addTime(TimeRange timerange) {
        List<TimeRange> newOpeningTimes = new ArrayList<>();
        for (TimeRange openingTime : openingTimes) {
            newOpeningTimes.addAll(openingTime.cut(timerange));
        }
        newOpeningTimes.add(timerange);
        openingTimes = newOpeningTimes;
    }

    /**
     * Add a fallback time range to this WeekDayRule. Adding fallback means the
     * input TimeRange will cut only TimeRange with Status.CLOSED, if any
     * 
     * @param timerange
     */
    public void addFallback(TimeRange timerange) {
        isFallbackLast = true;
        List<TimeRange> remains = new ArrayList<>();
        remains.add(timerange);
        for (TimeRange openingTime : openingTimes) {
            List<TimeRange> temp = new ArrayList<>();
            while(!remains.isEmpty()) {
                TimeRange check = remains.remove(0);
                if(openingTime.getStatus() != Status.CLOSED) {
                    temp.addAll(check.cut(openingTime));
                } else {
                    temp.add(check);
                }

            }
            remains = temp;
        }
        clean(remains);
        for(TimeRange remain : remains) {
            remain.setFallback(true);
            addTime(remain);
        }
    }

    /** Clear the current opening times in this WeekdayRule */
    public void clearOpeningHours() {
        openingTimes = new ArrayList<>();
    }

    /**
     * Get the Result of evaluating the inputTime in this WeekDayRule
     * 
     * @param inputTime input LocalDateTime to be checked
     * @return a Result containing info on Status and comment
     */
    public Result checkStatus(LocalDateTime inputTime) {
        return checkStatus(Utils.timeInMinute(inputTime));
    }

    /**
     * Get the Result of evaluating the inputHour in this WeekDayRule
     * 
     * @param inputTime input time in minutes to be checked
     * @return a Result containg info on Status and comment
     */
    Result checkStatus(int inputTime) {
        for (TimeRange openingTime : openingTimes) {
            if (inputTime >= openingTime.getStart()
                    && inputTime < openingTime.getEnd()) {
                return new Result(openingTime);
            }
        }
        // return CLOSED if no fitting opening times is detected
        return new Result(Status.CLOSED, null, null);
    }

        
    /**
     * Return the TimeRange whose Status is different from the TimeRange that
     * the inputHour is within. If none found, return the TimeRange of the
     * inputTime instead.
     * 
     * @param inputTime input time in minutes
     * @param isNext true to look forward, false to look backward in time
     * @return the TimeRange that the inputTime is within whose Status is
     *      different from the TimeRange that the inputTime is within, null
     *      otherwise
     */
    @Nullable
    TimeRange getDifferingEventToday(int inputTime, boolean isNext) {
        Status statusToCheck = null;
        // pad because during build all CLOSED range not defined is not added
        List<TimeRange> paddedTime = closePad(openingTimes);
        int current = (isNext) ? 0 : paddedTime.size()-1;
        int target = (isNext) ? paddedTime.size() : -1;

        while (current != target) {   
            TimeRange checkTime = paddedTime.get(current);
            if (statusToCheck == null) {
                if (inputTime >= checkTime.getStart()
                        && inputTime < checkTime.getEnd()) {
                    statusToCheck = checkTime.getStatus();
                }
            } else if (checkTime.getStatus() != statusToCheck){
                return checkTime;
            }
            current = current + ((isNext) ? 1: -1);
        }   
        return null;
    }

    /**
     * Return the TimeRange whose Status is different from the input Status
     * 
     * @param status input Status
     * @param isNext true to look forward, false to look backward in time
     * @return TimeRange whose Status is different from the input Status,
     *      null otherwise
     */
    @Nullable
    TimeRange getDifferingEvent(Status status, boolean isNext) {
        // pad because during build all CLOSED range not defined is not added
        List<TimeRange> paddedTime = closePad(openingTimes);
        int current = (isNext) ? 0 : paddedTime.size()-1;
        int target = (isNext) ? paddedTime.size() : -1;
        
        while (current != target) {   
            TimeRange checkTime = paddedTime.get(current);
            if (checkTime.getStatus() != status) {
                return checkTime;
            }
            current = current + ((isNext) ? 1: -1);
        }
        return null;
    }

    /**
     * Add time spills to be flushed when build is called for this WeekDayRule
     * 
     * @param timerange
     */
    public void addSpill(TimeRange timerange) {
        if (yesterdaySpill == null) {
            yesterdaySpill = new ArrayList<>();
        }
        yesterdaySpill.add(timerange);
    }

    /**
     * Apply all time spills added from previous day. This is used in build()
     * 
     */
    public void flushSpill() {
        while(!yesterdaySpill.isEmpty()) {
            TimeRange spill = yesterdaySpill.remove(0);
            addTime(spill, spill.isFallback());
        }
    }

    /** 
     * Sort the TimeRange of this WeekDayRule by order of start time
     */
    public void sort() {
        sort(openingTimes);
    }

    /**
     * Clean by sorting and removing duplicates of this WeekDay in this WeekDayRule.
     * This also applies all time spills, if any
     * 
     * */
    public void clean() {
        flushSpill();
        clean(openingTimes);
    }

    /**
     * Similar to a compareTo(), but this is for comparing with a DateWithOffset.
     * Using the defining date stored during construction
     * 
     * @param date input DateWithOffset
     * @return <0 if this day is before, >0 if this day is after, =0 if same day
     */
    public int compareToDate(LocalDate date) {
        return defDate.compareTo(date);
    }

    /** 
     * Sort the TimeRange of input List<TimeRange> by order of start time
     */
    public static void sort(List<TimeRange> timeranges) {
        Collections.sort(timeranges);
    }

    /** Clean by sorting and removing duplicates of input List<TimeRange> */
    public static void clean(List<TimeRange> timeranges) {
        sort(timeranges);
        int i = 0;
        while(i < timeranges.size()-1) {
            TimeRange merge = timeranges.get(i).merge(timeranges.get(i+1));
            if(merge != null) {
                timeranges.set(i, merge);
                timeranges.remove(i+1);
                i--;
            }
            i++;
        }
    }

    /**
     * Pad a List of TimeRange with closed TimeRange, wherever a time slot is
     * not occupied by other existing TimeRange
     * 
     * @param timeList List of TimeRange that needs to padded
     * @return padded List of TimeRange with closed TimeRange, wherever a time
     *      slot is not occupied by other existing TimeRange
     */
    public static List<TimeRange> closePad(List<TimeRange> timeList) {
        List<TimeRange> result = new ArrayList<>(timeList);

        // handle IndexOutOfBounds
        if (timeList.isEmpty()) {
            result.add(new TimeRange(0, 1440, Status.CLOSED));
            return result;    
        }

        TimeRange toAdd;
        // check start of day
        int startClose = 0;
        int endClose = result.get(0).getStart();
        if ((toAdd = closePadHelper(startClose, endClose)) != null) {
            result.add(0, toAdd);
        }

        // check between
        for (int i=0; i < result.size()-1; i++) {
            startClose = result.get(i).getEnd();
            endClose = result.get(i+1).getStart();
            if ((toAdd = closePadHelper(startClose, endClose)) != null) {
                result.add(i+1, toAdd);
            }
        }

        // check end of day
        startClose = result.get(result.size()-1).getEnd();
        endClose = 1440;
        if ((toAdd = closePadHelper(startClose, endClose)) != null) {
            result.add(toAdd);
        }
        return result;
    }

    /**
     * Helper for closePad. Process start and end of a close range and return
     * a CLOSED TimeRange accordingly. If start == end, then returns null
     * 
     * @param startClose start of CLOSED TimeRange
     * @param endClose end of CLOSED TimeRange
     * @return corresponding CLOSED TimeRange, null if startClose == endCLose
     */
    @Nullable
    private static TimeRange closePadHelper(int startClose, int endClose) {
        return (startClose < endClose)
                ? new TimeRange(startClose, endClose, Status.CLOSED)
                : null;
    }

    /**
     * 
     * @param date input LocalDate
     * @return nth weekday of a month from an input LocalDate, from 1 to 5
     */
    public static int getNthWeekDayOfMonth(LocalDate date) {
        return date.get(ChronoField.ALIGNED_WEEK_OF_MONTH);
    }

    /**
     * 
     * @param date input LocalDate
     * @return last date of the last weekday of the month, weekday similar to input LocalDate's
     */
    public static LocalDate getLastWeekDayOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.lastInMonth(date.getDayOfWeek()));
    }

    /**
     * 
     * @param date input LocalDate
     * @return reverse nth of the weekday of a month from an input LocalDate, from -5 to -1
     */
    public static int getReverseNthWeekOfMonth(LocalDate date) {
        int lastWeekDayOfMonthNth = getNthWeekDayOfMonth(getLastWeekDayOfMonth(date));
        return getNthWeekDayOfMonth(date) - 1 - lastWeekDayOfMonthNth;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(weekday + " (" + defDate + ") : ");
        for (TimeRange openingTime : openingTimes) {
            if (openingTime.hasComment()
                   || openingTime.getStatus() != Status.CLOSED) {
                b.append(openingTime.toString());
                b.append(" ");
            }
        }
        return b.toString();
    }

    /**
     * Similar to toString(), but this also return a String with Closed
     * TimeRange in it. Also included the defining Rule with it
     * 
     * @return debug string of this WeekDayRule
     */
    public String toDebugString() {
        StringBuilder b = new StringBuilder();
        b.append(weekday + " (" + defDate + ") : ");
        for (TimeRange openingTime : openingTimes) {
            b.append(openingTime.toDebugString());
            b.append(" ");
        }
        return b.toString();
    }
}
