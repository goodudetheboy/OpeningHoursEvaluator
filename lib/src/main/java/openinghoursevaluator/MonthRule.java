package openinghoursevaluator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ch.poole.openinghoursparser.Month;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.WeekDay;

public class MonthRule {
    int         year;
    Month       month;
    List<Rule>  rules = null;
    List<Week>  weekStorage = null;

    public MonthRule() {
        // nothing here
    }

    public MonthRule(List<Rule> rules) {
        weekStorage = new ArrayList<>();
        this.rules = rules;
    }

    public void build(LocalDateTime time) {
        populate(time);
        for (Rule rule : rules) {
            List<TimeRange> previousSpill = Week.simulatePreviousSpill(weekStorage.get(0), rule);
            weekStorage.get(0).setPreviousSpill(previousSpill);
            weekStorage.get(0).update(rule);
            if (weekStorage.size() > 1) {
                weekStorage.get(1).update(rule);
            }
            // System.out.println(rule);
            // System.out.println(toWeekString());
        }
    }

    public void update() {
        
    }

    public Result checkStatus(LocalDateTime time) {
        year = time.getYear();
        month = monthConvert(time);
        if (weekStorage.get(0).hasWeekDay(Week.toWeekDay(time.getDayOfWeek()))) {
            return weekStorage.get(0).checkStatus(time);
        } else {
            return weekStorage.get(1).checkStatus(time);
        }
    }

    public void populate(LocalDateTime time) {
        int weekOfMonth = getWeekOfMonth(time);
        switch (weekOfMonth) {  
            case 1:
                WeekDay cutoff = Week.toWeekDay(getFirstDayOfMonth(time).getDayOfWeek());
                if (cutoff == WeekDay.MO) {
                    weekStorage = buildWeek(weekOfMonth);
                } else {
                    int previousWeekOfMonth = getWeekOfMonth(getFirstDayOfWeek(time));
                    weekStorage = buildWeek(previousWeekOfMonth, Week.getPreviousWeekDay(cutoff), weekOfMonth);
                }
                break;
            case 5:
                WeekDay cutoff1 = Week.toWeekDay(getLastDayOfMonth(time).getDayOfWeek());
                if(cutoff1 == WeekDay.SU) {
                    weekStorage = buildWeek(weekOfMonth);
                } else {
                    weekStorage = buildWeek(weekOfMonth, cutoff1, 1);                    
                }
                
                break;
            default:
                weekStorage = buildWeek(weekOfMonth);
        }
    }



    public List<Week> buildWeek(int weekOfMonth) {
        return buildWeek(weekOfMonth, null, 0);
    }

    public List<Week> buildWeek(int weekOfMonth, WeekDay cutoff, int otherWeekOfMonth) {
        List<Week> result = new ArrayList<>();
        if(cutoff == null) {
            Week week = new Week(null, weekOfMonth);
            result.add(week);
        } else {
            Week first = new Week(null, weekOfMonth, WeekDay.MO, cutoff);
            Week second = new Week(null, otherWeekOfMonth, Week.getNextWeekDay(cutoff), WeekDay.SU);
            first.connect(second);
            result.add(first);
            result.add(second);
        }
        return result;
    }

    public static LocalDateTime getFirstDayOfWeek(LocalDateTime time) {
        return time.with(WeekFields.ISO.dayOfWeek(), 1);
    }

    public static LocalDateTime getLastDayOfWeek(LocalDateTime time) {
        return time.with(WeekFields.ISO.dayOfWeek(), 7);
    }

    public static LocalDateTime getFirstDayOfMonth(LocalDateTime time) {
        return time.withDayOfMonth(1);
    }

    public static LocalDateTime getLastDayOfMonth(LocalDateTime time) {
        YearMonth month = YearMonth.from(time);
        return month.atEndOfMonth().atStartOfDay();
    }

    public static Month monthConvert(LocalDateTime time) {
        return Month.values()[time.getMonth().ordinal()];
    }

    public static int getWeekOfMonth(LocalDateTime date) {
        Calendar c = Calendar.getInstance(Locale.FRANCE);
        c.set(date.getYear(), date.getMonthValue()-1, date.getDayOfMonth());
        c.setMinimalDaysInFirstWeek(1);
        return c.get(Calendar.WEEK_OF_MONTH);
        // return date.get(WeekFields.ISO.weekOfMonth());
    }

    public static int getPreviousWeekOfMonth(int weekOfMonth) {
        return (weekOfMonth == 1) ? 5 : weekOfMonth-1;
    }

    public String toWeekString() {
        StringBuilder b = new StringBuilder();
        if(weekStorage.size() == 1) {
            b.append(weekStorage.get(0).toString());
        } else {
            b.append(weekStorage.get(0).toString());
            b.append("Next month\n");
            b.append(weekStorage.get(1).toString());
        }
        return b.toString();
    }
}
