package openinghoursevaluator;

import java.io.ByteArrayInputStream;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import ch.poole.openinghoursparser.ParseException;
import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.WeekDay;
import ch.poole.openinghoursparser.WeekDayRange;

public class Week {
    int     year;
    int     month;
    // to be supported later, default weekNum = 0;
    int     weekNum;
    String openingHours;  
    HashMap<WeekDay, WeekDayRule>   weekRule;
    List<Rule>                      rules;

    public Week(String openingHours, LocalDateTime time) {
        year = time.getYear();
        month = time.getMonthValue();
        weekNum = (int) (time.getDayOfYear() / 7 + 1); // this is temporary
        this.openingHours = openingHours;
        weekRule = new HashMap<>();
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
        if(rule.isTwentyfourseven() || rule.getDays() == null) {
            for(WeekDay weekday : WeekDay.values()) {
                updateHelper(rule, weekday);
            }
            return;
        }
        for(WeekDayRange weekdays : rule.getDays()) {
            boolean isGood = false;
            for(WeekDay weekday : WeekDay.values()) {
                if(weekday.equals(weekdays.getStartDay())) {
                    isGood = true;
                }
                if(isGood) {
                    updateHelper(rule, weekday);
                    if(weekday.equals(weekdays.getEndDay())) break;
                }
            }
        }
    }
    /** Helper for update(), check if a rule has been built, if not create new*/
    private void updateHelper(Rule rule, WeekDay weekday) {
        WeekDayRule oldRule = weekRule.get(weekday);
        if(oldRule != null) oldRule.build(rule);
        else                weekRule.put(weekday, new WeekDayRule(rule, weekday));
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
