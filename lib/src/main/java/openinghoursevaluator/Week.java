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
    int year;
    int month;
    // to be supported later, default weekNum = 0;
    int weekNum;

    HashMap<WeekDay, WeekDayRule> weekRule;
    List<Rule> rules;

    public Week(String openingHours, LocalDateTime time) {
        year = time.getYear();
        month = time.getMonthValue();
        weekNum = (int) (time.getDayOfYear() / 7 + 1); // this is temporary
        weekRule = new HashMap<>();

        OpeningHoursParser parser = new OpeningHoursParser(new ByteArrayInputStream(openingHours.getBytes()));
        try {
            rules = parser.rules(true);
            for(Rule rule : rules) update(rule);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void update(Rule rule) {
        if(rule.isTwentyfourseven() || rule.getDays() == null) {
            for(WeekDay weekday : WeekDay.values()) {
                WeekDayRule oldRule = weekRule.get(weekday);
                if(oldRule != null) oldRule.build(rule);
                else                weekRule.put(weekday, new WeekDayRule(rule, weekday));
            }
        } else {
            for(WeekDayRange weekdays : rule.getDays()) {
                boolean isGood = false;
                for(WeekDay weekday : WeekDay.values()) {
                    if(weekday.equals(weekdays.getStartDay())) {
                        isGood = true;
                    }
                    if(isGood) {
                        weekRule.put(weekday, new WeekDayRule(rule, weekday));
                        if(weekday.equals(weekdays.getEndDay())) break;
                    }
                }
            }
        }

    }

    public boolean checkStatus(LocalDateTime time) {
        WeekDay weekdayToCheck = toWeekDay(time.getDayOfWeek());
        if(weekRule.get(weekdayToCheck).checkStatus(time)) return true;
        return false;
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
