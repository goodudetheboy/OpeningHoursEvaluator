package openinghoursevaluator;

public class Tools {
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
}
