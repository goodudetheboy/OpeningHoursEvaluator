package openinghoursevaluator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.poole.openinghoursparser.Month;
import ch.poole.openinghoursparser.Nth;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.TimeSpan;
import ch.poole.openinghoursparser.WeekDay;
import ch.poole.openinghoursparser.WeekDayRange;

public class WeekDayRule {
    public static final int INVALID_NUM = Integer.MIN_VALUE;

    // TODO: This rule storage is to be dealt later
    Rule            currentRule     = null;
    List<Rule>      offRule         = null;
    List<Rule>      unknownRule     = null;
    List<Rule>      additiveRule    = null;
    List<Rule>      fallbackRule    = null;

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
    List<TimeRange> yesterdaySpill  = null;

    // opening times storage
    List<TimeRange> openingTimes    = null;

    /** Default constructor, setting current to null and weekday to Monday */
    public WeekDayRule() {
        // nothing here
    }
    
    /** Constructor with Weekday */    
    public WeekDayRule(LocalDate defDate) {
        this.defDate = defDate;
        dissectDefDate(defDate);
        offRule = new ArrayList<>();
        unknownRule = new ArrayList<>();
        additiveRule = new ArrayList<>();
        fallbackRule = new ArrayList<>();
        openingTimes = new ArrayList<>();
        yesterdaySpill = new ArrayList<>();
    }

    /** Constructor with a WeekDay and a next WeekDayRule */
    public WeekDayRule(LocalDate defDate, WeekDayRule nextDayRule) {
        this(defDate);
        this.nextDayRule = nextDayRule;
    }


    /**
     * Helper function for constructor, used for gathering info from
     * defining date
     */
    private void dissectDefDate(LocalDate defDate) {
        year = defDate.getYear();
        month = MonthRule.convertMonth(defDate);
        date = defDate.getDayOfYear();
        weekday = Week.convertWeekDay(defDate.getDayOfWeek());
        nthWeekDay = getNthWeekDayOfMonth(defDate);
        reverseNth = getReverseNthWeekOfMonth(defDate);
    }

    /**
     * @return the current rule affecting this weekday
     */
    public Rule getCurrentRule() {
        return currentRule;
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
     * @return the time spill from previous day
     */
    public List<TimeRange> getSpilledTime() {
        return yesterdaySpill;
    }

    public LocalDate getDefDate() {
        return defDate;
    }

    /**
     * Set the current rule of this weekday
     * 
     * @param rule rule to be set current
     */
    public void setCurrentRule(Rule rule) {
        this.currentRule = rule;
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
     * @param nextWeekDay WeekDayRule to be set next
     */
    public void setNextDayRule(WeekDayRule nextDayRule) {
        this.nextDayRule = nextDayRule;
    }

    /**
     * Set this WeekDayRule's time spill with yesterdaySpill
     * 
     * @param yesterdaySpill time spill to be set
     */
    public void setSpilledTime(List<TimeRange> yesterdaySpill) {
        this.yesterdaySpill = yesterdaySpill;
    }

    public void setDefiningDate(LocalDate defDate) {
        this.defDate = defDate;
    }

    /** Build the opening times of this weekday with the current rule */
    public void build() {
        build(currentRule);
    }

    /**
     * Build the opening times of this weekday with a rule. This also resets
     * whatever the current affecting rule is to the new rule and also clears
     * the opening times (if not fallback), if exists. Closed time does not
     * clear days.
     * 
     * @param rule rule to be used in building
     */
    public void build(Rule rule) {
        if (rule.isEmpty()) {
            throw new IllegalArgumentException("There's an empty rule, please remove it");
        }
        if (rule.isAdditive()) {
            additiveRule.add(rule);
        } else if (rule.isFallBack()) {
            fallbackRule.add(rule);
        } else {
            switch (Status.convert(rule.getModifier())) {
            case CLOSED:
                offRule.add(rule);
                break;
            case UNKNOWN:
                clearAllRules();
                clearOpeningHours();
                unknownRule.add(rule);
                break;
            case OPEN:
                clearAllRules();
                clearOpeningHours();
                currentRule = rule;
                break;
            default:
            }
        }
        flushSpill();
        addRule(rule);
    }

    /**
     * A soft version of build(), where opening rule does not overwrite
     * current day. This also doesn't apply any time spill from previous
     * day.
     * 
     * @param rule Rule to be added
     */
    public void addRule(Rule rule) {
        String comment = (rule.getModifier() != null)
                            ? rule.getModifier().getComment()
                            : null;
        Status status = Status.convert(rule.getModifier());
        if (rule.isTwentyfourseven() || rule.getTimes() == null) {
            TimeSpan allDay = new TimeSpan();
            allDay.setStart(0);
            allDay.setEnd(1440);
            addTime(allDay, status, comment, rule.isFallBack());
            return;
        }
        for (TimeSpan timespan : rule.getTimes()) {
            addTime(timespan, status, comment, rule.isFallBack());
        }
    }

    /**
     * Check if input weekOfMonth is within Nth range. Supports negative nth.
     * 
     * @param nth input Nth
     * @param weekOfMonth week of month (max 5)
     * @return if applicable or not
     */
    public boolean isApplicableNth(List<Nth> nths) {
        if (nths == null) {
            return true;
        }
        for (Nth nth : nths) {
            int startNth = nth.getStartNth();
            int endNth = nth.getEndNth();
            if ((endNth == Nth.INVALID_NTH)) {
                if ((startNth > 0 && nthWeekDay == startNth) // positive Nth
                        || (startNth < 0 && reverseNth == startNth)) { // negative Nth
                return true;
                }
            } else {
                // handle illegal cases
                if (endNth < startNth) {
                    throw new IllegalArgumentException("Start nth " + startNth
                                                    + " cannot be less than "
                                                    + endNth);
                } else if (startNth < 0 && endNth > 0) {
                    throw new IllegalArgumentException("Illegal range: "
                                                    + startNth + " - " + endNth);
                }
                if (Utils.isBetween(nthWeekDay, startNth, endNth)
                        || Utils.isBetween(reverseNth, startNth, endNth)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if this WeekDayRule is affected by any day offset
     * 
     * @return
     */
    public boolean isApplicableOffset(WeekDayRange weekdays) {
        int offset = weekdays.getOffset();
        if (offset == 0) {
            throw new IllegalArgumentException("Offset not 0 should have been "
                                    + "checked before this function is called");
        } else if (weekdays.getEndDay() != null) {
            throw new IllegalArgumentException("There cannot be an end day in "
                                    + " a WeekDayRange with an offset");
        }
        LocalDate offsetDate = DateManager.getOffsetDate(defDate, -offset);
        WeekDayRule offsetDay = new WeekDayRule(offsetDate);
        WeekDay offsetWeekDay = offsetDay.getWeekDay();
        return offsetWeekDay == weekdays.getStartDay()
                && offsetDay.isApplicableNth(weekdays.getNths());
    }

    /**
     * Add a TimeSpan with a Status and an optional comment to the current
     * WeekDayRule
     * 
     * @param timespan TimeSpan to add
     * @param status desired Status
     * @param comment optional comment
     * @param isFallback if time added is fallback rule
     */
    public void addTime(TimeSpan timespan, Status status, String comment,
                            boolean isFallback) {
        if (timespan.getInterval() != 0) {
            addInterval(timespan, status, comment, isFallback);
        } else if (timespan.isOpenEnded()) {
            addOpenEnd(timespan, status, comment, isFallback);
        } else {
            addTime(timespan.getStart(), timespan.getEnd(), status, comment,
                    isFallback);
        }
    }

    /** Helper function to add TimeSpan with interval to this WeekDayRule */
    private void addInterval(TimeSpan timespan, Status status, String comment,
                                boolean isFallback) {
        int interval = timespan.getInterval();
        int current = timespan.getStart();
        int end = timespan.getEnd();
        TimeRange timepoint = null;
        do {
            timepoint = new TimeRange(current, status, comment);
            addTime(timepoint, isFallback);
            current = current + interval;
        } while (current <= end && current < TimeRange.MAX_TIME);
        while(current <= end) {
            timepoint = new TimeRange(current - TimeRange.MAX_TIME, status, 
                                        comment);
            nextDayRule.addSpill(timepoint);
            current = current + interval;
        }
    }

    /** 
     * Helper function to add TimeSpan with open end to this WeekDayRule.
     * <p>
     * Open-ended time is evaluated similar to the opening_hours.js (the
     * javascript version of the evaluator), according to the following
     * rule (extracted from opening_hours.js
     * {@link https://github.com/opening-hours/opening_hours.js#time-ranges}):
     * <ul>
     * <li>Open end applies until the end of the day if the opening time is
     * before 17:00. If the opening time is between 17:00 and 21:59 the open
     * end time ends 10 hours after the opening. And if the opening time is
     * after 22:00 (including 22:00) the closing time will be interpreted as
     * 8 hours after the opening time.
     * </ul>
     * <p>
     * @see https://github.com/opening-hours/opening_hours.js#time-ranges,
     * open-ended time section
     * */
    private void addOpenEnd(TimeSpan timespan, Status status, String comment,
                                boolean isFallback) {
        int openEndStart = 0;
        // if there's a range before it, it will take the status
        if (timespan.getEnd() != TimeSpan.UNDEFINED_TIME) {
            openEndStart = timespan.getEnd();
            addTime(timespan.getStart(), timespan.getEnd(), status, comment,
                    isFallback);
        } else {
            openEndStart = timespan.getStart();
        }
        Status openEndedStatus = (status == Status.CLOSED)
                                    ? status
                                    : Status.UNKNOWN;
        // this is for the start of open ended time in the next day
        int nextDayStart = openEndStart - TimeRange.MAX_TIME; 
        if (nextDayStart < 0) {
            OpenEndRange range = new OpenEndRange(openEndStart,
                                            TimeRange.MAX_TIME,
                                            openEndedStatus,
                                            comment);
            addTime(range, isFallback);
            int firstCutOff = 17*60;
            int secondCutOff = 22*60;
            if (openEndStart >= firstCutOff) {
                int timespill = openEndStart - TimeRange.MAX_TIME;
                timespill += (openEndStart >= secondCutOff) ? 8*60 : 10*60;
                OpenEndRange spillRange = new OpenEndRange(0, timespill,
                                                        openEndedStatus,
                                                        comment);
                range.setNextDaySpill(spillRange);
                nextDayRule.addSpill(spillRange);
            }
        } else if (nextDayStart >= TimeRange.MAX_TIME + 17*60) {
            throw new IllegalArgumentException("Time spanning more than"
                                                + "two days not supported");
        } else {
            OpenEndRange nextDaySpill = new OpenEndRange(nextDayStart,
                                                    nextDayStart + 8*60,
                                                    openEndedStatus,
                                                    comment);
            nextDayRule.addSpill(nextDaySpill);
        }
    }

    /**
     * Add the time with the specified start and end into this WeekDayRule, 
     * along with a Status and an optional comment.
     * This also supports time spilling (end > 24:00).
     * 
     * @param start start time (must be < 24:00 AKA 1440 and also less than
     *      end time)
     * @param end end time
     * @param status desired Status
     * @param comment optional comment
     */
    public void addTime(int start, int end, Status status, String comment,
                            boolean isFallback) {
        int endToday = end; // for the current day
        if (end != TimeSpan.UNDEFINED_TIME) {
            int timespill = end - TimeRange.MAX_TIME;
            if (timespill > 0) {
                endToday = TimeRange.MAX_TIME;
                nextDayRule.addSpill(new TimeRange(0, timespill, status, comment));
            }
        } else {
            endToday = start;
        }
        addTime(new TimeRange(start, endToday, status, comment), isFallback);
    }

    /**
     * Add a TimeRange to this WeekDayRule. As per the specifications of
     * TimeRange, this does not support time spilling (end > 24:00).
     * <p>
     * An additional setting is whether to add as fallback. Adding fallback
     * means the input TimeRange will cut only TimeRange with Status.CLOSED,
     * if any
     * 
     * @param timerange TimeRange to add
     * @param isFallback soft or hard
     */
    public void addTime(TimeRange timerange, boolean isFallback) {
        if (isFallback) {
            addFallback(timerange);
        } else {
            addTime(timerange);
        }
    }

    /**
     * Add a non-fallback TimeRange to this WeekDayRule. Adding also follows the rule
     * listed below (extracted from opening_hours.js
     * {@link https://github.com/opening-hours/opening_hours.js}): 
     * <ol>
     * <li> 07:00+,12:00-16:00: If an open end time is used in a way that the
     * first time range includes the second one (07:00+ is interpreted as
     * 07:00-24:00 and thus includes the complete 12:00-16:00 time selector),
     * the second time selector cuts of the part which would follow after 16:00.
     * </ol>
     * <p>
     * @param timerange
     * @see https://github.com/opening-hours/opening_hours.js#time-ranges
     */
    public void addTime(TimeRange timerange) {
        List<TimeRange> newOpeningTimes = new ArrayList<>();
        for (TimeRange openingTime : openingTimes) {
            // ignore any OpenEndRange marked for removal
            if (!OpenEndRange.checkNeededRemoving(openingTime)) {
                newOpeningTimes.addAll(processTime(timerange, openingTime));
            }
        }
        if (timerange.hasComment() || timerange.getStatus() != Status.CLOSED) {
            newOpeningTimes.add(timerange);
        } 
        openingTimes = newOpeningTimes;
    }

    /**
     * Helper for addTime(). Check a TimeRange to be added against a current 
     * openingTime and process it into addable TimeRange to current
     * openingTimes.
     * 
     * @param timerange timerange to be added
     * @param openingTime a current opening time to be checked against
     * @return processed time to be added to current openingTimes
     */
    private List<TimeRange> processTime(TimeRange timerange,
                                TimeRange openingTime) {
        List<TimeRange> result = new ArrayList<>();
        for (TimeRange cutTime : openingTime.cut(timerange)) {
            if (!(openingTime instanceof OpenEndRange)
                    || cutTime.getStart() == openingTime.getStart()) {
                // add any that is not OpenEndRange or OpenEndRange before
                // the timerange being added
                result.add(cutTime);
            } else {
                // if is OpenEndRange getting cut, then mark for disposal
                OpenEndRange nextDaySpill
                    = ((OpenEndRange) openingTime).getNextDaySpill();
                if (nextDaySpill != null) {
                    nextDaySpill.setNeededRemoving(true);
                }
            }
        }
        return result;
    }

    /**
     * Add a fallback time range to this WeekDayRule. Adding fallback means the
     * input TimeRange will cut only TimeRange with Status.CLOSED, if any
     * 
     * @param timerange
     */
    public void addFallback(TimeRange timerange) {
        List<TimeRange> remains = new ArrayList<>();
        remains.add(timerange);
        // make this fallback TimeRange only cuts closed time range by letting
        // all not closed cut this TimeRange first
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
        // then will cut this later
        for(TimeRange remain : remains) {
            addTime(remain);
        }
    }

    /**
     * Override the current rule with a new rule. Also return the old rule for
     * other purpose
     * 
     * @param newRule rule to be replaced
     * @return the old rule
     * 
    */
    public Rule override(Rule newRule) {
        Rule oldRule = currentRule.copy();
        currentRule = newRule;
        clearOpeningHours();
        build(newRule);
        return oldRule;
    }

    /** Clear the current opening times in this WeekdayRule */
    public void clearOpeningHours() {
        openingTimes = new ArrayList<>();
    }

    /** Clear all current rules of this WeekDayRule */
    public void clearAllRules() {
        currentRule = null;       
        offRule = new ArrayList<>();
        unknownRule = new ArrayList<>();
        additiveRule = new ArrayList<>();
    }

    /**
     * Check if the input time evaluates to the avenue being opened in the
     * current rule
     * 
     * @param inputTime input date and time to be checked
     * @return true if opening; false if closed
     */
    public Result checkStatus(LocalDateTime inputTime) {
        int timepoint = Utils.timeInMinute(inputTime); 
        for (TimeRange openingTime : openingTimes) {
            if (timepoint >= openingTime.getStart()
                    && timepoint < openingTime.getEnd()) {
                return new Result(openingTime);
            }
        }
        // return CLOSED if no fitting opening times is detected
        return new Result(Status.CLOSED, null);
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

    /** Apply all time spills added from previous day. This is used in build()*/
    public void flushSpill() {
        while(!yesterdaySpill.isEmpty()) {
            addTime(yesterdaySpill.remove(0), false);
        }
    }

    /** 
     * Sort the TimeRange of this WeekDayRule by order of start time
     */
    public void sort() {
        sort(openingTimes);
    }

    /**
     * Clean by sorting and removing duplicate TimeRange or any OpenEndRange
     * marked for removal of this WeekDay in this WeekDayRule.
     * This also applies all time spills, if any
     */
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
     * 
     * @param timranges a List of TimeRange
     */
    public static void sort(List<TimeRange> timeranges) {
        Collections.sort(timeranges);
    }

    /**
     * Clean by sorting and removing duplicate TimeRange or any OpenEndRange
     * marked for removal of input List<TimeRange>
     * 
     * @param timeranges a List of TimeRange 
     */
    public static void clean(List<TimeRange> timeranges) {
        sort(timeranges);
        int i = 0;
        while (i < timeranges.size()-1) {
            TimeRange merge = timeranges.get(i).merge(timeranges.get(i+1));
            if (merge != null) {
                timeranges.set(i, merge);
                timeranges.remove(i+1);
                i--;
            }
            i++;
        }
        int k = 0;
        while (k < timeranges.size()) {
            TimeRange checking = timeranges.get(k);
            if (OpenEndRange.checkNeededRemoving(checking)) {
                timeranges.remove(k);
            } else {
                k++;
            }
        }
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
            b.append(openingTime.toString());
            b.append(" ");
        }
        return b.toString();
    }
}
