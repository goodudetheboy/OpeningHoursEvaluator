package openinghoursevaluator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.TimeSpan;
import ch.poole.openinghoursparser.WeekDay;
import ch.poole.openinghoursparser.WeekDayRange;

public class WeekDayRule {
    Rule            currentRule     = null;
    List<Rule>      offRule         = null; // TODO: to be supported later
    List<Rule>      unknownRule     = null;
    List<Rule>      additiveRule    = null; // TODO: to be supported later
    WeekDay         weekday         = null;
    List<TimeRange> openingTimes    = null;
    WeekDayRule     nextDayRule     = null;
    List<TimeRange> yesterdaySpill = null;

    /** Default constructor, setting current to null and weekday to Monday */
    public WeekDayRule() {
        // nothing here
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
    public WeekDay getCurrentWeekDay() {
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

    /** Constructor with Weekday */    
    public WeekDayRule(WeekDay weekday) {
        this.weekday = weekday;
        this.offRule = new ArrayList<>();
        this.unknownRule = new ArrayList<>();
        this.additiveRule = new ArrayList<>();
        this.openingTimes = new ArrayList<>();
        this.yesterdaySpill = new ArrayList<>();
    }

    /** Constructor with a WeekDay and a next WeekDayRule */
    public WeekDayRule(WeekDay weekday, WeekDayRule nextDayRule) {
        this(weekday);
        this.nextDayRule = nextDayRule;
    }

    /** Build the opening times of this weekday with the current rule */
    public void build() {
        build(currentRule);
    }

    /**
     * Build the opening times of this weekday with a rule. This also resets whatever the current affecting rule is to the new rule
     * and also clears the opening times (if not fallback), if exists. Closed and Unknown time does not clear days
     * 
     * @param rule rule to be used in building
     */
    public void build(Rule rule) {
        if (rule.isEmpty()) {
            throw new IllegalArgumentException("There's an empty rule, please remove it");
        }
        if(rule.isAdditive()) {
            additiveRule.add(rule);
        } else {
            switch(Status.convert(rule.getModifier())) {
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
     * A soft version of build(), where opening rule does not overwrite current day.
     * This also doesn't apply any time spill from previous day
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
            addTime(allDay, status, comment);
            return;
        }
        for (TimeSpan timespan : rule.getTimes()) {
            if(timespan.isOpenEnded()) {
                int openEndStart = 0;
                if(timespan.getEnd() != TimeSpan.UNDEFINED_TIME) {
                    openEndStart = timespan.getEnd();
                    addTime(timespan, status, comment);
                } else {
                    openEndStart = timespan.getStart();
                }
                addTime(new TimeRange(openEndStart, TimeRange.MAX_TIME, Status.UNKNOWN, "open ended time"));
                int firstCutOff = 17*60;
                int secondCutOff = 22*60;
                if(openEndStart >= firstCutOff) {
                    int timespill = openEndStart - TimeRange.MAX_TIME;
                    timespill += (openEndStart >= secondCutOff) ? 8*60 : 10*60;
                    nextDayRule.addSpill(new TimeRange(0, timespill, Status.UNKNOWN, "open ended time"));
                }
            } else {
                addTime(timespan, status, comment);
            }
        }
    }

    /**
     * Add a TimeSpan with a Status and an optional comment to the current
     * WeekDayRule
     * 
     * @param timespan TimeSpan to add
     * @param status desired Status
     * @param comment optional comment
     */
    public void addTime(TimeSpan timespan, Status status, String comment) {
        int interval = timespan.getInterval();
        if (interval != 0) {
            int current = timespan.getStart();
            int end = timespan.getEnd();
            TimeRange timepoint = null;
            do {
                timepoint = new TimeRange(current, status, comment);
                addTime(timepoint);
                current = current + interval;
            } while (current <= end && current < TimeRange.MAX_TIME);
            while(current <= end) {
                timepoint = new TimeRange(current - TimeRange.MAX_TIME, status, comment);
                nextDayRule.addSpill(timepoint);
                current = current + interval;
            }
        } else {
            addTime(new TimeRange(timespan, status, comment));
            TimeSpan spilledTime = TimeRange.checkTimeSpill(timespan);
            if(spilledTime != null) {
                nextDayRule.addSpill(new TimeRange(spilledTime, status, comment));
            }
        }
    }

    /**
     * Add a TimeRange to this WeekDayRule
     * 
     * @param timerange TimeRange to add
     */
    public void addTime(TimeRange timerange) {
        List<TimeRange> newOpeningTimes = new ArrayList<>();
        for (TimeRange openingTime : openingTimes) {
            newOpeningTimes.addAll(TimeRange.cut(openingTime, timerange));
        }
        switch(timerange.getStatus()) {
            case CLOSED:
                if(!timerange.hasComment()) {
                    break;
                }
            case UNKNOWN:
            case OPEN:
                newOpeningTimes.add(timerange);
                break;
            default:
        }
        openingTimes = newOpeningTimes;
    }

    /**
     * Apply all time spills added from previous day. This is used in build()
     * 
     */
    public void flushSpill() {
        while(!yesterdaySpill.isEmpty()) {
            addTime(yesterdaySpill.remove(0));
        }
    }

    public boolean checkIfApplicableWeekDayRange(WeekDayRange weekDayRange) {
        int start = weekDayRange.getStartDay().ordinal();
        WeekDay endDay = weekDayRange.getEndDay();
        int current = weekday.ordinal();
        return (endDay != null) ?
                    (current >= start && current <= endDay.ordinal()
                    || current <= start + 7 && current >= endDay.ordinal()) :
                    (current == start);

    }

    /**
     * Override the current rule with a new rule. Also return the old rule for other purpose
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
     * Check if the input time evaluates to the avenue being opened in the current rule
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
        yesterdaySpill.add(timerange);
    }

    /** 
     * Sort the TimeRange of this WeekDayRule by order of start time
     */
    public void sort() {
        Collections.sort(openingTimes);
    }

    /** Clean by sorting and removing duplicates of this WeekDay in this WeekDayRule. */
    public void clean() {
        sort();
        int i = 0;
        while(i < openingTimes.size()-1) {
            TimeRange merge = openingTimes.get(i).merge(openingTimes.get(i+1));
            if(merge != null) {
                openingTimes.set(i, merge);
                openingTimes.remove(i+1);
                i--;
            }
            i++;
        }
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (TimeRange openingTime : openingTimes) {
            b.append(openingTime.toString());
            b.append(" ");
        }
        return b.toString();
    }
}
