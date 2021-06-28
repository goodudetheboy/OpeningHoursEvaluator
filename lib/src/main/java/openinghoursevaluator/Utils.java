package openinghoursevaluator;

import java.time.LocalDateTime;

import ch.poole.openinghoursparser.WeekDay;

public class Utils {
    /** Default constructor */
    private Utils() {
        // empty on purpose
    }

    /**
     * Check if input value is in between the two start and end value i.e. start <= value <= end
     * 
     * @param value value to be checked 
     * @param start start value
     * @param end end value
     * @return whether start <= value <= end
     */
    public static boolean isBetween(Integer value, Integer start, Integer end) {
        return value.compareTo(start) >= 0 && value.compareTo(end) <= 0;
    }



    /**
     * Convert hour and minute of a LocalDateTime instance to minutes
     *  
     * @param time a LocalDateTime instance
    */
    public static int timeInMinute(LocalDateTime time) {
        return time.getHour()*60 + time.getMinute();
    }

    /**
     * Check if input WeekDay is between a start Weekday and a end WeekDay.
     * Supports WeekDay spanning between two weeks (e.g. Su is between Sa-Tu)
     * 
     * @param value WeekDay to check
     * @param start start WeekDay
     * @param end endWeekDay
     * @return if value WeekDay is between start and end WeekDay
     */
    public static boolean isBetweenWeekDays(WeekDay value, WeekDay start, WeekDay end) {
        int startO = start.ordinal();
        int endO = (end.ordinal() < startO) ? end.ordinal() + 7 : end.ordinal();
        return isBetween(value.ordinal(), startO, endO);
    }

}
