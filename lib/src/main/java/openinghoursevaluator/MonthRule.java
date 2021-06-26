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

    /**
     * Build a Week of this MonthRule on an input of time, complete with week number.
     * Also supports Week that is split between two months.
     * 
     * @param time input LocalDateTime
     */
    public void build(LocalDateTime time) {
        populate(time);
        for (Rule rule : rules) {
            List<TimeRange> previousSpill = Week.simulateSpill(weekStorage.get(0), rule);
            weekStorage.get(0).setPreviousSpill(previousSpill);
            weekStorage.get(0).update(rule);
            if (weekStorage.size() > 1) {
                weekStorage.get(1).update(rule);
            }
        }
    }

    /**
     * Evaluate the stored OH string with a time to see if it's opening or closed
     * 
     * @param time input LocalDateTime
     * @return the result of the evaluation
     */
    public Result checkStatus(LocalDateTime time) {
        year = time.getYear();
        month = monthConvert(time);
        if (weekStorage.get(0).hasWeekDay(Week.toWeekDay(time.getDayOfWeek()))) {
            return weekStorage.get(0).checkStatus(time);
        } else {
            return weekStorage.get(1).checkStatus(time);
        }
    }

    /**
     * Populate a Week of this MonthRule on an input time.
     * 
     * @param time input LocalDateTime
     */
    public void populate(LocalDateTime time) {
        int weekOfMonth = getWeekOfMonth(time);
        int lastWeekNum = getNumOfWeekOfMonth(time);
        WeekDay cutoff = null;
        if (weekOfMonth == 1) {
            cutoff = Week.toWeekDay(getFirstDayOfMonth(time).getDayOfWeek());
            if (cutoff != WeekDay.MO) {
                int previousWeekOfMonth = getWeekOfMonth(Week.getFirstDayOfWeek(time));
                weekStorage = Week.createEmptyWeek(previousWeekOfMonth, Week.getPreviousWeekDay(cutoff), weekOfMonth);
                return;
            }
        } else if (weekOfMonth == lastWeekNum) {
            cutoff = Week.toWeekDay(getLastDayOfMonth(time).getDayOfWeek());
            if(cutoff != WeekDay.SU) {
                weekStorage = Week.createEmptyWeek(weekOfMonth, cutoff, 1);                    
                return;
            }            
        }
        // case 2, 3, 4
        weekStorage = Week.createEmptyWeek(weekOfMonth);
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

    public static int getWeekOfMonth(LocalDateTime time) {
        Calendar c = Calendar.getInstance(Locale.FRANCE);
        c.set(time.getYear(), time.getMonthValue()-1, time.getDayOfMonth());
        c.setMinimalDaysInFirstWeek(1);
        return c.get(Calendar.WEEK_OF_MONTH);
        // return date.get(WeekFields.ISO.weekOfMonth());
    }

    public static int getNumOfWeekOfMonth(LocalDateTime time) {
        return getWeekOfMonth(getLastDayOfMonth(time));
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
