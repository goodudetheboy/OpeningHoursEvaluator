package openinghoursevaluator;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.WeekDay;
import ch.poole.openinghoursparser.WeekDayRange;

public class Week {
    int     year;
    int     month;
    int     weekNum; // TODO: to be supported later
    EnumMap<WeekDay, WeekDayRule>   weekRule;
    List<Rule>                      rules;

    public Week(List<Rule> rules, LocalDateTime time) {
        year = time.getYear();
        month = time.getMonthValue();
        weekNum = time.getDayOfYear() / 7 + 1; // this is temporary
        this.rules = rules;
        weekRule = new EnumMap<>(WeekDay.class);
    }

    /**
     * Build Week with the current openingHours string
     * 
     * @param isStrict strict or not
     */
    public void build(boolean isStrict) {
        fillEmpty();
        for (Rule rule : rules) {
            update(rule);
        }
        clean();
    }

    /**
     * Update Week with a rule
     * 
     * @param rule a Rule
     */
    public void update(Rule rule) {
        List<WeekDayRange> weekdayRange;
        if (rule.getDays() != null) {
            weekdayRange = rule.getDays();
        } else {
            WeekDayRange allWeekDays = new WeekDayRange();
            allWeekDays.setStartDay(WeekDay.MO);
            allWeekDays.setEndDay(WeekDay.SU);
            weekdayRange = new ArrayList<>();
            weekdayRange.add(allWeekDays);
        }
        for (WeekDayRange weekdays : weekdayRange) {
            WeekDay current = weekdays.getStartDay();
            WeekDay end = (weekdays.getEndDay() != null)
                            ? weekdays.getEndDay()
                            : current;
            do {
                weekRule.get(current).build(rule);
            } while((current = getNextWeekDay(current)) != getNextWeekDay(end));
            weekRule.get(current).flushSpill();
        }
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
        WeekDay weekdayToCheck = toWeekDay(time.getDayOfWeek());
        return weekRule.get(weekdayToCheck).checkStatus(time);
    }

    /**
     * Convert java.time.DayOfWeek enum to OpeningHoursParser.WeekDay enum
     * 
     * @param dayOfWeek an enum of DayOfWeek to be converted
     * @return an equivalent weekday in WeekDay enum
     */
    public static WeekDay toWeekDay(DayOfWeek dayOfWeek) {
        int dayOfWeekNth = dayOfWeek.ordinal();
        for (WeekDay weekday : WeekDay.values()) {
            if (weekday.ordinal() == dayOfWeekNth) {
                return weekday;
            }
        }
        return null;
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

    /** Sort all WeekDayRule in this Week */
    public void clean() {
        for (WeekDay weekday : WeekDay.values()) {
            if(weekRule.get(weekday) != null) {
                weekRule.get(weekday).clean();
            }
        }
    }

    /** Reset this Week by removing all current WeekDayRule and filling it with empty ones*/
    public void reset() {
        weekRule = new EnumMap<>(WeekDay.class);
        fillEmpty();
    }

    /** Fill all empty WeekDay in weekDayRule with empty List */
    public void fillEmpty() {
        fillEmptyHelper(WeekDay.MO);
    }

    private void fillEmptyHelper(WeekDay weekday) {
        weekRule.put(weekday, new WeekDayRule(weekday));
        WeekDay nextDay = getNextWeekDay(weekday);
        if(weekRule.get(nextDay) == null) {
            fillEmptyHelper(nextDay);
        }
        weekRule.get(weekday).setNextDayRule(weekRule.get(nextDay));
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (WeekDay weekday : WeekDay.values()) {
            b.append(weekday);
            b.append(" : ");
            if (weekRule.get(weekday) != null) {
                b.append(weekRule.get(weekday).toString());
            }
            b.append(System.getProperty("line.separator"));
        }
        return b.toString();
    }
}
