package openinghoursevaluator;

import java.io.ByteArrayInputStream;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import ch.poole.openinghoursparser.ParseException;
import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.RuleModifier;
import ch.poole.openinghoursparser.TimeSpan;
import ch.poole.openinghoursparser.WeekDay;
import ch.poole.openinghoursparser.WeekDayRange;

public class Week {
    int     year;
    int     month;
    int     weekNum; // TODO: to be supported later
    String openingHours;  
    EnumMap<WeekDay, WeekDayRule>   weekRule;
    List<Rule>                      rules;

    public Week(String openingHours, LocalDateTime time) {
        year = time.getYear();
        month = time.getMonthValue();
        weekNum = time.getDayOfYear() / 7 + 1; // this is temporary
        this.openingHours = openingHours;
        weekRule = new EnumMap<>(WeekDay.class);
    }

    /**
     * Build Week with the current openingHours string
     * 
     * @param isStrict strict or not
     */
    public void build(boolean isStrict) {
        OpeningHoursParser parser = new OpeningHoursParser(new ByteArrayInputStream(openingHours.getBytes()));
        try {
            rules = parser.rules(isStrict);
            for(Rule rule : rules) update(rule);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update Week with a rule
     * 
     * @param rule a Rule
     */
    public void update(Rule rule) {
        if(isUniversal(rule)){
            for(WeekDay weekday : WeekDay.values()) {
                updateHelper(rule, weekday, null);
            }
            return;
        }
        Rule spilledRule = getSpilledRule(rule);
        List<WeekDayRange> weekdayRange;
        if(rule.getDays() != null) {
            weekdayRange = rule.getDays();
        } else {
            WeekDayRange allWeekDays = new WeekDayRange();
            allWeekDays.setStartDay(WeekDay.MO);
            allWeekDays.setEndDay(WeekDay.SU);
            weekdayRange = new ArrayList<>();
            weekdayRange.add(allWeekDays);
        }
        for(WeekDayRange weekdays : weekdayRange) {
            WeekDay start = weekdays.getStartDay();
            WeekDay end = (weekdays.getEndDay() != null) ? weekdays.getEndDay() : weekdays.getStartDay();
            if(end.ordinal() < start.ordinal()) {
                end = WeekDay.SU;
            }
            // TimeSpan spilling to the next day current handling method (subject to changes):
            // Enforce TimeRange created from spilling TimeSpan to create only in 24 hours
            // which means that if an input TimeSpan is 23:00-2:00, for example, the resulting
            // TimeRange created is only 23:00-24:00, and all spilled rule are saved and added
            // afterwards
            for(int i=start.ordinal(); i <= end.ordinal(); i++) {
                updateHelper(rule, start, spilledRule);
                spilledRule = getSpilledRule(rule);
                start = (start != WeekDay.SU) ? getNextWeekDay(start) : WeekDay.SU;
            }
        }
    }
    /** Helper for update(), check if a rule has been built, if not create new*/
    void updateHelper(Rule rule, WeekDay weekday, Rule spilledRule) {
        WeekDayRule oldRule = weekRule.get(weekday);
        if(oldRule != null)
            oldRule.build(rule);
        else
            weekRule.put(weekday, new WeekDayRule(rule, weekday));
        if(spilledRule != null)
            weekRule.get(weekday).add(spilledRule);
    }

    Rule getSpilledRule(Rule rule) {
        Rule spilledRule = new Rule();
        spilledRule.setModifier(rule.getModifier());
        List<TimeSpan> spilledTimeList = new ArrayList<>();
        for(TimeSpan timespan : rule.getTimes()) {
            if(timespan.getEnd() > TimeRange.MAX_TIME) {
                TimeSpan spilledTime = new TimeSpan();
                spilledTime.setStart(0);
                spilledTime.setEnd(timespan.getEnd() - TimeRange.MAX_TIME); 
                spilledTimeList.add(spilledTime);
            }
        }
        spilledRule.setTimes(spilledTimeList);
        return spilledRule;
    }

    /**
     * @return true if input Rule can be applied to any day in the week
     */
    boolean isUniversal(Rule rule) {
        return rule.isTwentyfourseven() || (rule.getDays() == null && rule.getTimes() == null);
    }

    /**
     * Return the next WeekDay wrt a current WeekDay
     * 
     * @param current the current WeekDay
     * @return the following WeekDay
     */
    WeekDay getNextWeekDay(WeekDay current) {
        WeekDay[] weekdays = WeekDay.values();
        int next = (current.ordinal()+1) % weekdays.length;
        return weekdays[next];
    }

    public Status checkStatus(LocalDateTime time) {
        WeekDay weekdayToCheck = toWeekDay(time.getDayOfWeek());
        return weekRule.get(weekdayToCheck).checkStatus(time);
    }

    /**
     * Convert java.time.DayOfWeek enum to OpeningHoursParser.WeekDay enum
     * 
     * @param dayOfWeek an enum of DayOfWeek to be converted
     * @return an equivalent weekday in WeekDay enum
     */
    public WeekDay toWeekDay(DayOfWeek dayOfWeek) {
        int dayOfWeekNth = dayOfWeek.ordinal();
        for(WeekDay weekday : WeekDay.values()) {
            if(weekday.ordinal() == dayOfWeekNth) return weekday;
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for(WeekDay weekday : WeekDay.values()) {
            b.append(weekday);
            b.append(" : ");
            b.append(weekRule.get(weekday).toString());
            b.append(System.getProperty("line.separator"));
        }
        return b.toString();
    }
}
