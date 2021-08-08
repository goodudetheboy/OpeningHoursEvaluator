package openinghoursevaluator;

import java.time.LocalDate;
import java.time.temporal.WeekFields;

import ch.poole.openinghoursparser.WeekDay;
import ch.poole.openinghoursparser.WeekRange;

public class WeekManager {
    
    public WeekManager() {
        // empty
    }

    /**
     * Process WeekRange to see if it is applicable to input Week 
     * 
     * @param weekRange input WeekRange
     * @param week input Week
     * @return if the week is applicable according to weekRange a.k.a inside
     *      week range
     */
    public boolean processWeekRange(WeekRange weekRange, Week week) {
        int start = weekRange.getStartWeek();
        int end = weekRange.getEndWeek();
        int weekNum = week.getWeekOfYear();

        if (end != WeekRange.UNDEFINED_WEEK) {
            int interval = weekRange.getInterval();
            if (interval == 0) {
                return (end < start) ? weekNum <= end || weekNum >= start
                                     : Utils.isBetween(weekNum, start, end);
            } else {
                return processInterval(start, end, interval, weekNum);
            }
        } else {
            return start == weekNum;
        }
    }

    /**
     * Used in case the WeekRange has an interval. Check if input weekNum is
     * applicable between a start and end with an interval in between
     *  
     * @param start start week number
     * @param end end week number
     * @param interval interval
     * @param weekNum week number to be checked
     * @return if input weekNum is applicable between a start and end with an
     *      interval in between
     */
    private boolean processInterval(int start, int end, int interval, int weekNum) {
        // TODO: add geocoding here, because US only have 52 weeks/year
        int stopper = (end < start) ? 53 : end;
        int check = start;
        while (check <= stopper) {
            if (weekNum == check) {
                return true;
            }
            int temp = check + interval;
            if (temp > 53) {
                // in case of week end < start
                return processInterval(temp-53, end, interval, weekNum);
            } else {
                check = temp;
            }
        }
        return false;
    }

    /**
     * Calculates the first weekday of the week of the input date.
     * 
     * @param date the LocalDate of desired week
     * @return the LocalDate of the first weekday in the week of input date
     */
    public static LocalDate getFirstDayOfWeek(LocalDate date) {
        return getWeekDayOfWeek(date, WeekDay.MO);
    }

    /**
     * Calculates the last weekday of the week of the input date.
     * 
     * @param date the LocalDate of desired week
     * @return the LocalDate of the last weekday in the week of input date
     */
    public static LocalDate getLastDayOfWeek(LocalDate date) {
        return getWeekDayOfWeek(date, WeekDay.SU);
    }

    /**
     * Gets the week day of the week of the input date.
     * 
     * @param date the LocalDate of desired week
     * @param weekday the weekday to find
     * @return the LocalDate of an input weekday in the week of input date
     */
    public static LocalDate getWeekDayOfWeek(LocalDate date, WeekDay weekday) {
        // ordinal starts with 0, so need this to make up for date.with()
        int n = weekday.ordinal() + 1;
        return date.with(WeekFields.ISO.dayOfWeek(), n);
    }
}
