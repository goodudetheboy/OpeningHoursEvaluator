package openinghoursevaluator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.RuleModifier;
import ch.poole.openinghoursparser.TimeSpan;
import ch.poole.openinghoursparser.WeekDay;
import ch.poole.openinghoursparser.WeekDayRange;
import ch.poole.openinghoursparser.RuleModifier.Modifier;

public class WeekDayRule {
    Rule        currentRule     = null;
    List<Rule>  offRule         = null; // TODO: to be supported later
    List<Rule>  unknownRule     = null;
    List<Rule>  fallbackRule    = null; // TODO: to be supported later
    WeekDay     weekday         = null;
    List<TimeRange> openingTimes = null;

    /** Default constructor, setting current to null and weekday to Monday */
    public WeekDayRule() {
        // nothing here
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

    /** Constructor with Rule and Weekday */    
    public WeekDayRule(Rule rule, WeekDay weekday) {
        this.currentRule = rule;
        this.weekday = weekday;
        this.offRule = new ArrayList<>();
        this.unknownRule = new ArrayList<>();
        this.openingTimes = new ArrayList<>();
        build();
    }

    /** Build the opening times of this weekday with the current rule */
    public void build() {
        build(currentRule);
    }

    /**
     * Build the opening times of this weekday with a rule. This also resets whatever the current affecting rule is to the new rule
     * and also clears the opening times (if not fallback), if exists.
     * 
     * @param rule rule to be used in building
     */
    public void build(Rule rule) {
        if(rule.isEmpty()) {
            // TODO: produce a warning here
            return;
        }
        if(rule.getModifier() == null) {
            buildOpen(rule);
            return;
        }
        switch(rule.getModifier().getModifier()) {
            case CLOSED:
            case OFF:
                offRule.add(rule);
                for(TimeSpan timespan : rule.getTimes()) {
                    List<TimeRange> newOpeningTimes = new ArrayList<>();
                    for(TimeRange openingTime: openingTimes) {
                        for(TimeRange newTime : TimeRange.cut(openingTime, new TimeRange(timespan, null))) {
                            newOpeningTimes.add(newTime);
                        }
                    }
                    openingTimes = newOpeningTimes;
                }
                return;
            case UNKNOWN:
                unknownRule.add(rule);
                for(TimeSpan timespan : rule.getTimes()) {
                    List<TimeRange> newOpeningTimes = new ArrayList<>();
                    for(TimeRange openingTime: openingTimes) {
                        for(TimeRange newTime : TimeRange.cut(openingTime, new TimeRange(timespan, null))) {
                            newOpeningTimes.add(newTime);
                        }
                    }
                    newOpeningTimes.add(new TimeRange(timespan, Status.UNKNOWN));
                    openingTimes = newOpeningTimes;
                }               
                return;
            case OPEN:
                buildOpen(rule);
                return;
            default:
        }
    }

    /** Helper function for build(), to build opening times only */
    void buildOpen(Rule rule) {
        clearAllRules();
        clearOpeningHours();
        currentRule = rule;
        if( rule.isTwentyfourseven() ||
            rule.getTimes() == null) {
            TimeRange timerange = new TimeRange(0, 1440, Status.OPEN);
            openingTimes.add(timerange);
            return;
        }
        for(TimeSpan timespan : rule.getTimes()) openingTimes.add(new TimeRange(timespan, Status.OPEN));
    }

    public boolean checkIfApplicableWeekDayRange(WeekDayRange weekDayRange) {
        int start = weekdayInNum(weekDayRange.getStartDay());
        WeekDay endDay = weekDayRange.getEndDay();
        int current = weekdayInNum(weekday);
        return (endDay != null) ?
                    (current >= start && current <= weekdayInNum(endDay) || current <= start + 7 && current >= weekdayInNum(endDay)) :
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

    public void clearAllRules() {
        currentRule = null;       
        offRule = new ArrayList<>();
    }

    /**
     * Check if the input time evaluates to the avenue being opened in the current rule
     * 
     * @param inputTime input date and time to be checked
     * @return true if opening; false if closed
     */
    public Status checkStatus(LocalDateTime inputTime) {
        int timepoint = timeInMinute(inputTime); 
        for(TimeRange openingTime : openingTimes) {
            if(openingTime.getEnd() == Integer.MIN_VALUE && timepoint == openingTime.getStart() ||
               timepoint >= openingTime.getStart() && timepoint <= openingTime.getEnd()) {
                   return openingTime.getStatus();
               }
        }
        // return CLOSED if no fitting opening times is detected
        return Status.CLOSED;
    }

    /**
     * Helper function
     * Convert hour and minute of a LocalDateTime instance to minutes
     *  
     * @param time a LocalDateTime instance
    */
    int timeInMinute(LocalDateTime time) {
        return time.getHour()*60 + time.getMinute();
    }

    /**
     * Helper function
     * Conver OpeningHourParser.WeekDay to int
     * 
     * @param weekday
     * @return 
     */

    int weekdayInNum(WeekDay weekday) {
        int result = 0;
        for(WeekDay weekdayEnum : WeekDay.values()) {
            if(weekday.equals(weekdayEnum))
                break;
            else
                result++;
        }
        return ++result;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for(TimeRange openingTime : openingTimes) {
            b.append(openingTime.toString());
            b.append(" ");
        }
        return b.toString();
    }
}
