package openinghoursevaluator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.TimeSpan;
import ch.poole.openinghoursparser.WeekDay;
import ch.poole.openinghoursparser.WeekDayRange;

public class WeekDayRule {
    Rule currentRule;
    List<Rule> fallbackRule; // to be supported later
    WeekDay weekday;
    List<TimeRange> openingTimes;

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
        this.openingTimes = new ArrayList<>();
        build();
    }

    /** Build the opening times of this weekday with the current rule */
    public void build() {
        build(currentRule);
    }

    /**
     * Build the opening times of this weekday with a rule. This also resets whatever the current affecting rule is to the new rule
     * and also clears the opening times (if not fallback), if exists
     * 
     * @param rule rule to be used in building
     */

    public void build(Rule rule) {
        currentRule = rule;
        clearOpeningHours();
        if(rule.isTwentyfourseven()) {
            TimeRange timerange = new TimeRange();
            timerange.setStart(0);
            timerange.setEnd(1440);
            openingTimes.add(timerange);
            return;
        }
        for(TimeSpan timespan : rule.getTimes()) openingTimes.add(new TimeRange(timespan));
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

    /**
     * Check if the input time evaluates to the avenue being opened in the current rule
     * 
     * @param inputTime input date and time to be checked
     * @return true if opening; false if closed
     */
    public boolean checkStatus(LocalDateTime inputTime) {
        int timepoint = timeInMinute(inputTime); 
        for(TimeRange openingTime : openingTimes) {
            if(openingTime.getEnd() == Integer.MIN_VALUE && timepoint == openingTime.getStart()) return true;
            else if(timepoint >= openingTime.getStart() && timepoint <= openingTime.getEnd()) return true;
        }
        // return false if no fitting opening times is detected
        return false;
    }

    /** Convert hour and minute of a LocalDateTime instance to minutes 
     *  
     * @param time a LocalDateTime instance
    */
    public int timeInMinute(LocalDateTime time) {
        return time.getHour()*60 + time.getMinute();
    }

    /**
     * Conver OpeningHourParser.WeekDay to int
     * 
     * @param weekday
     * @return 
     */

    public int weekdayInNum(WeekDay weekday) {
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
