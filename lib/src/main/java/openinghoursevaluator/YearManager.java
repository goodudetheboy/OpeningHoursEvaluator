package openinghoursevaluator;

import ch.poole.openinghoursparser.YearRange;

public class YearManager {

    public YearManager() {
        // empty
    }

    /**
     * Process YearRange to see if it is applicable to year of input Week 
     * 
     * @param yearRange input YearRange
     * @param week input Week
     * @return if the year of Week is applicable according to weekRange a.k.a
     *      inside the year range
     * @throws OpeningHoursEvaluationException
     */
    public boolean processYearRange(YearRange yearRange, Week week)
            throws OpeningHoursEvaluationException {
        checkError(yearRange);
        int start = yearRange.getStartYear();
        int end = yearRange.getEndYear();
        int yearNum = week.getYear();
        if (end != YearRange.UNDEFINED_YEAR) {
            int interval = yearRange.getInterval();
            if (interval == 0) {
                return Utils.isBetween(yearNum, start, end);
            } else {
                return processInterval(start, end, interval, yearNum);
            }
        } else {
            return (yearRange.isOpenEnded()) ? yearNum >= start : yearNum == start;
        }
    }

    /**
     * Used in case the YearRange has an interval. Check if input yearNum is
     * applicable between a start and end with an interval in between
     *  
     * @param start start year
     * @param end end year number
     * @param interval interval
     * @param yearNum year to be checked
     * @return if input yearNum is applicable between a start and end with an
     *      interval in between
     */
    private boolean processInterval(int start, int end, int interval, int yearNum) {
        int check = start;
        while (check <= end) {
            if (yearNum == check) {
                return true;
            }
            check = check + interval;
        }
        return false;
    }

    /**
     * Check for illegal YearRange
     * 
     * @param yearRange input YearRange
     * @throws OpeningHoursEvaluationException
     */
    public static void checkError(YearRange yearRange)
            throws OpeningHoursEvaluationException {
        if (yearRange.getEndYear() != YearRange.UNDEFINED_YEAR
                && yearRange.getStartYear() > yearRange.getEndYear()) {
            throw new OpeningHoursEvaluationException("Illegal range ("
                + yearRange +"), start year cannot be after end year");
        }
    }
}
