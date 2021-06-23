package openinghoursevaluator;

import java.time.LocalDateTime;

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
}
