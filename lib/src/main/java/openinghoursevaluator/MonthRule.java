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
        year = time.getYear();
        month = convertMonth(time.toLocalDate());
        populate(time);
        for (Rule rule : rules) {
            List<TimeRange> previousSpill = Week.simulateSpill(weekStorage.get(0), rule);
            weekStorage.get(0).setPreviousSpill(previousSpill);
            weekStorage.get(0).update(rule);
            if (weekStorage.size() > 1) {
                weekStorage.get(1).update(rule);
            }
            // System.out.println(toWeekString());
        }
    }

    /**
     * Evaluate the stored OH string with a time to see if it's opening or closed
     * 
     * @param time input LocalDateTime
     * @return the result of the evaluation
     */
    public Result checkStatus(LocalDateTime time) {
        if (weekStorage.get(0).hasWeekDay(Week.convertWeekDay(time.getDayOfWeek()))) {
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
        weekStorage = Week.createEmptyWeek(time.toLocalDate());
    }

    /**
     * Return the first day of a month, based on an input time
     *  
     * @param date input time
     * @return the first day of a month, based on an input time
     */
    public static LocalDate getFirstDayOfMonth(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    /**
     * Return the last day of a month, based on an input time
     *  
     * @param date input time
     * @return the last day of a month, based on an input time
     */
    public static LocalDate getLastDayOfMonth(LocalDate date) {
       return date.withDayOfMonth(date.lengthOfMonth());
    }

    /**
     * Convert the Month class in the LocalDateTime to Month class in the OpeningHoursParser
     * 
     * @param time
     * @return
     */
    public static Month convertMonth(LocalDate date) {
        return Month.values()[date.getMonth().ordinal()];
    }

    /**
     * Return the week of a month, based on a time and a Locale
     * 
     * @param date
     * @return
     */
    public static int getNthWeekOfMonth(LocalDate date, Locale locale) {
        Calendar c = Calendar.getInstance(locale);
        c.set(date.getYear(), date.getMonthValue()-1, date.getDayOfMonth());
        c.setMinimalDaysInFirstWeek(1);
        return c.get(Calendar.WEEK_OF_MONTH);
    }

    /**
     * Return the order of a week of the input time, in reverse order of the month.
     * For example: normally, in a 5-week month, the 2nd week would be the 3rd week
     * in reverse of the month
     * 
     * @param date input time
     * @return order of a week of the input time, in reverse order of the month
     */
    public static int getReverseNthWeekOfMonth(LocalDate date, Locale locale) {
        return getNthWeekOfMonth(date, locale) - 1 - getNumOfWeekOfMonth(date, locale);
    }

    /**
     * Return the number of week of a month of an input time
     * 
     * @param time input time
     * @return number of week of a month of an input time
     */
    public static int getNumOfWeekOfMonth(LocalDate date, Locale locale) {
        return getNthWeekOfMonth(getLastDayOfMonth(date), locale);
    }

    /**
     * Return the number of the previous week of a week of a month
     * 
     * @param weekOfMonth input week of month
     * @return the number of the previous week of a week of a month
     */
    public static int getPreviousWeekOfMonth(int weekOfMonth) {
        // TODO: Refactor this to be more dynamic
        return (weekOfMonth == 1) ? 5 : weekOfMonth-1;
    }

    /**
     * Return the number of the previous week of a week of a month
     * 
     * @param weekOfMonth input week of month
     * @return the number of the previous week of a week of a month
     */
    public static int getPreviousReverseWeekOfMonth(int weekOfMonth) {
        // TODO: Refactor this to be more dynamic
        return (weekOfMonth == -5) ? -1 : weekOfMonth-1;
    }

    /**
     * @return the toString of the Week stored in this MonthRule
     */
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
