package openinghoursevaluator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import ch.poole.openinghoursparser.DateRange;
import ch.poole.openinghoursparser.DateWithOffset;
import ch.poole.openinghoursparser.Month;
import ch.poole.openinghoursparser.WeekDay;
import ch.poole.openinghoursparser.WeekDayRange;
import ch.poole.openinghoursparser.YearRange;

/**
 * Helper class to process date and weekday-related stuff, including DateRange,
 * DateWithOffset, WeekDayRange and so on from OpeningHoursParser
 */
public class DateManager {
    /** Default constructor */
    public DateManager() {
        // empty on purpose
    }

    /**
     * Process the DateRange inside to a LocalDate range. Used to find applicable
     * WeekDay to which a Rule can apply.
     * 
     * @param dateRange a DateRange
     * @param week a Week where this DateRange will apply
     * @return a LocalDate range processed from DateRange
     * @see https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification#monthday_range, explanation
     */
    public List<List<LocalDate>> processDateRange(DateRange dateRange, Week week) {
        int defaultYear = week.getYear();
        List<List<LocalDate>> result = new ArrayList<>();
        DateWithOffset start = dateRange.getStartDate();
        DateWithOffset end = dateRange.getEndDate();
        checkError(start, end);
        // there is always a start of DateRange
        List<LocalDate> subResult = new ArrayList<>();
        result.add(subResult);
        subResult.add(DateManager.convertToLocalDate(start, defaultYear, start.getMonth()));
        if (end != null) {
            // handle when there is no year specified but there is year wrapping
            // the compare below only check for date and month
            // @see https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification#explain:monthday_range:date_offset:to:date_offset
            if (compareStartAndEnd(start, end) > 0) {
                subResult.add(DateManager.convertToLocalDate(end, defaultYear + 1, start.getMonth()));
                List<LocalDate> otherResult = new ArrayList<>();
                otherResult.add(DateManager.convertToLocalDate(start, defaultYear-1, start.getMonth()));
                otherResult.add(DateManager.convertToLocalDate(end, defaultYear, start.getMonth()));
                result.add(otherResult);
            } else { 
                subResult.add(DateManager.convertToLocalDate(end, defaultYear, start.getMonth()));
            }
        }
        return result;
    }

    /**
     * compareTo() for DateWithOffset (for now), check only the date stored in it
     * 
     * @param start start of a DateRange
     * @param end end of a DateRange
     * @return <0 if d1 is before d2, >0 if d1 is after d2, =0 if d1 is same day as d2
     */
    private int compareStartAndEnd(DateWithOffset start, DateWithOffset end) {
        return ((end.getMonth() == null) || start.getMonth() == end.getMonth())
                ? start.getDay() - end.getDay()
                : start.getMonth().ordinal() - end.getMonth().ordinal();
    }

    public void checkError(DateWithOffset start, DateWithOffset end) {
        String rangeString = "(" + start + " - " + end + ")";
        if (end == null) {
            return;
        }
        // check if start year is not defined and end year is defined
        if (start.getYear() == YearRange.UNDEFINED_YEAR
                && end.getYear() != YearRange.UNDEFINED_YEAR) {
            throw new IllegalArgumentException("Year must be defined at start rather than end, this range "
                                                + rangeString + " is meaningless");
        }
        // check if start is after end
        if (start.getYear() != YearRange.UNDEFINED_YEAR
                && ((compareStartAndEnd(start, end) > 0)
                        || (end.getYear() != YearRange.UNDEFINED_YEAR
                            && start.getYear() > end.getYear()))) {
            throw new IllegalArgumentException("Illegal range " + rangeString + ", please double check");
        }
    }


    /**
     * Special conversion of DateWithOffset to date, with optionalYear and
     * optionalMonth. Supports date with offset.
     * <p>
     * If no year or month is specified inside the DateWithOffset, 
     * the conversion will use optionalYear and optionalMonth instead
     * 
     * @param date input DateWithOffset
     * @param optionalYear optional year value, used in case of date doesn't
     * have year specificed
     * @param optionalMonth optional Month, used in case of date doesn't have
     * Month specified
     * @return a corresponding LocalDateTime
     */
    public static LocalDate convertToLocalDate(DateWithOffset date, int optionalYear, Month optionalMonth) {
        int monthInt = (date.getMonth() == null)
                        ? optionalMonth.ordinal() + 1
                        : date.getMonth().ordinal() + 1;
        int yearInt = (date.getYear() == YearRange.UNDEFINED_YEAR)
                        ? optionalYear
                        : date.getYear();
        int offset = date.getDayOffset();
        return getOffsetDate(LocalDate.of(yearInt, monthInt, date.getDay()), offset);
    }
    
    /**
     * Return the date that is offset by some days from the input date,
     * according to the following rule:
     * <ol>
     * <li> offset < 0: return [offset] days before input date
     * <li> offset > 0: return [offset] days after input date
     * <li> offset = 0: return original date
     * </ol>
     * <p>
     * 
     * @param date an input date
     * @param offset offset, in days
     * @return the date that is offset by some [offset] days from the input date
     */
    public static LocalDate getOffsetDate(LocalDate date, int offset) {
        if (offset > 0) {
            return date.plusDays(offset);
        } 
        if (offset < 0) {
            return date.minusDays(-offset);
        }
        return date;
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
        return Utils.isBetween(value.ordinal(), startO, endO);
    }

    /**
     * Check if input WeekDay is between a start and end in a WeekDayRange
     * Supports WeekDay spanning between two weeks (e.g. Su is between Sa-Tu)
     * 
     * @param value WeekDay to check
     * @param start start WeekDay
     * @param end endWeekDay
     * @return if value WeekDay is between start and end WeekDay
     */
    public static boolean isBetweenWeekDays(WeekDay value, WeekDayRange weekdays) {
        return isBetweenWeekDays(value, weekdays.getStartDay(), weekdays.getEndDay());
    }
}
