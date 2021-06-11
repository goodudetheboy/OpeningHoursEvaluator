package openinghoursevaluator;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import ch.poole.openinghoursparser.ParseException;
import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.WeekDay;

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
        for(WeekDay weekday : WeekDay.values()) {
            weekRule.put(weekday, new WeekDayRule(rule, weekday));
        }
    }

    public boolean checkStatus(LocalDateTime time) {
        for(WeekDay weekday : WeekDay.values()) {
            if(weekRule.get(weekday).checkStatus(time)) {
                return true;
            }
        }
        return false;
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
