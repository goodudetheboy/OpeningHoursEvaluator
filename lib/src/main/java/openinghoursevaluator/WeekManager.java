package openinghoursevaluator;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;

import ch.poole.openinghoursparser.WeekDay;
import ch.poole.openinghoursparser.WeekRange;

public class WeekManager {
    
    public WeekManager() {
        // empty
    }

    public boolean processWeekRange(WeekRange weekRange, Week week) {
        int start = weekRange.getStartWeek();
        int end = weekRange.getEndWeek();
        int weekNum = week.getWeekOfYear();

        if (end != WeekRange.UNDEFINED_WEEK) {
            return (end < start) ? weekNum <= end || weekNum >= start
                                 : Utils.isBetween(weekNum, start, end);
        } else {
            return start == weekNum;
        }
    }

    /**
     * 
     * @param time the LocalDateTime of desired week
     * @return the LocalDate of the first weekday in the week of input date
     */
    public static LocalDate getFirstDayOfWeek(LocalDate date) {
        return getWeekDayOfWeek(date, WeekDay.MO);
    }

    /**
     * 
     * @param time the LocalDateTime of desired week
     * @return the LocalDate of the last weekday in the week of input date
     */
    public static LocalDate getLastDayOfWeek(LocalDate date) {
        return getWeekDayOfWeek(date, WeekDay.SU);
    }

    /**
     * 
     * @param date
     * @param weekday
     * @return the LocalDate of an input weekday in the week of input date
     */
    public static LocalDate getWeekDayOfWeek(LocalDate date, WeekDay weekday) {
        // ordinal starts with 0, so need this to make up for date.with()
        int n = weekday.ordinal() + 1;
        return date.with(WeekFields.ISO.dayOfWeek(), n);
    }
}
