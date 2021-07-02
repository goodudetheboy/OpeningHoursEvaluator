package openinghoursevaluator;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.checkerframework.checker.nullness.qual.NonNull;

import ch.poole.openinghoursparser.DateRange;
import ch.poole.openinghoursparser.DateWithOffset;
import ch.poole.openinghoursparser.Month;
import ch.poole.openinghoursparser.VarDate;
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
        checkError(start, end, defaultYear);
        // there is always a start of DateRange
        List<LocalDate> subResult = new ArrayList<>();
        result.add(subResult);
        subResult.add(convertToLocalDate(start, defaultYear, start.getMonth(), true));
        if (end != null) {
            // handle when there is no year specified but there is year wrapping
            // the compare below only check for date and month
            // @see https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification#explain:monthday_range:date_offset:to:date_offset
            if (compareStartAndEnd(start, end, defaultYear) > 0) {
                subResult.add(convertToLocalDate(end, defaultYear + 1, start.getMonth(), false));
                // get year wraping from previous year
                List<LocalDate> otherResult = new ArrayList<>();
                otherResult.add(convertToLocalDate(start, defaultYear-1, start.getMonth(), true));
                otherResult.add(convertToLocalDate(end, defaultYear, start.getMonth(), false));
                result.add(otherResult);
            } else { 
                subResult.add(convertToLocalDate(end, defaultYear, start.getMonth(), false));
            }
        } else {
            if (start.getDay() == DateWithOffset.UNDEFINED_MONTH_DAY) {
                // set isStart as false here to get the last day of the month
                subResult.add(convertToLocalDate(start, defaultYear, start.getMonth(), false));
            }
        }
        return result;
    }

    /**
     * compareTo() for DateWithOffset (for now), check only the date stored in it
     * 
     * @param start start of a DateRange
     * @param end end of a DateRange
     * @param easterYear optional year, used if there's easter in one of the ends
     * @return <0 if d1 is before d2, >0 if d1 is after d2, =0 if d1 is same day as d2
     */
    private int compareStartAndEnd(DateWithOffset start, DateWithOffset end, int easterYear) {
        if (isEaster(start)) {
            int year = (start.getYear() != YearRange.UNDEFINED_YEAR)
                        ? start.getYear()
                        : easterYear;
            fillEaster(start, year);
        }
        if (isEaster(end)) {
            int year = (end.getYear() != YearRange.UNDEFINED_YEAR)
                        ? end.getYear()
                        : easterYear;
            fillEaster(end, year);
        }
        return ((end.getMonth() == null) || start.getMonth() == end.getMonth())
                ? start.getDay() - end.getDay()
                : start.getMonth().ordinal() - end.getMonth().ordinal();
    }

    /**
     * Helper function for compareStartAndEnd().
     * <p>
     * Populate input DateWithOffset with month and day off easter, for 
     * checking day and month in compareStartAndEnd().
     * 
     * */
    private void fillEaster(DateWithOffset date, int easterYear) {
        LocalDate easter = getEasterDate(easterYear);
        date.setDay(easter.getDayOfMonth());
        date.setMonth(MonthRule.convertMonth(easter));
    }

    /**
     * Error checking between a start date and an end date of DateRange.
     * <p>
     * Failing condition TBU.
     * 
     * @param start start DateWithOffset of a DateRange
     * @param end end DateWithOffset of a DateRange
     * @param easterYear optional easter year, TODO: temporary, needs removing
     */
    public void checkError(DateWithOffset start, DateWithOffset end, int easterYear) {
        String rangeString = "(" + start + " - " + end + ")";
        if (end == null) {
            return;
        }
        // check if start year is not defined and end year is defined
        if (start.getYear() == YearRange.UNDEFINED_YEAR
                && end.getYear() != YearRange.UNDEFINED_YEAR) {
            throw new IllegalArgumentException("Year must be defined at start rather than end,"
                                            + " this range" + rangeString + " is meaningless");
        }
        // check if start is after end
        if (start.getYear() != YearRange.UNDEFINED_YEAR
                && ((compareStartAndEnd(start, end, easterYear) > 0)
                        || (end.getYear() != YearRange.UNDEFINED_YEAR
                            && start.getYear() > end.getYear()))) {
            throw new IllegalArgumentException("Illegal range " + rangeString
                                                + ", please double check");
        }
    }

    /**
     * Check if input DateWithOffset is an Easter day
     * 
     * @param date input DateWithOffset
     * @return if its variable date is easter
     */
    public static boolean isEaster(DateWithOffset date) {
        return date.getVarDate() != null && date.getVarDate() == VarDate.EASTER;
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
     *      have year specificed
     * @param optionalMonth optional Month, used in case of date doesn't have
     *      Month specified
     * @param isStart true if is start date offset, false if is end date offset
     *      useful when day is not specified. In case of start date, set day to
     *      1; in case of end date, set day to max days of specified month
     * @return a corresponding LocalDateTime
     */
    public static LocalDate convertToLocalDate(DateWithOffset date, int optionalYear, Month optionalMonth, boolean isStart) {
        int yearInt = (date.getYear() == YearRange.UNDEFINED_YEAR)
                    ? optionalYear
                    : date.getYear();
        LocalDate pending = null;
        // check for variable date
        if (date.getVarDate() != null) {
            // to be updated when more variable date is added
            if (date.getVarDate() == VarDate.EASTER) {
                pending = getEasterDate(yearInt);
            }
        } else {
            int monthInt = (date.getMonth() == null)
                        ? optionalMonth.ordinal() + 1
                        : date.getMonth().ordinal() + 1;
            int dayInt = date.getDay();
            // check for in case of undefined day of month
            if (dayInt == DateWithOffset.UNDEFINED_MONTH_DAY) {
                dayInt = (isStart) ? 1 : YearMonth.of(yearInt, monthInt).lengthOfMonth();
            }
            pending = LocalDate.of(yearInt, monthInt, dayInt);
        }
        // handle weekday offset
        if (date.getWeekDayOffset() != null && pending != null) {
            pending = findNextWeekDay(pending, date.isWeekDayOffsetPositive(),
                                        date.getWeekDayOffset());
        }
        // handle day offset
        return getOffsetDate(pending, date.getDayOffset());
    }
    
    /**
     * Returns the easter day of an input year.
     * <p>
     * This function uses the  "Meeus/Jones/Butcher" algorithm. For more
     * information check the link provided below.
     * 
     * @param year valid Gregorian year
     * @return the date of Easter of input year
     * @author Bernhard Seebass, from https://stackoverflow.com/a/55278990/10154717
     * @see "Meeus/Jones/Butcher" algorithm
     * https://en.wikipedia.org/wiki/Date_of_Easter#Anonymous_Gregorian_algorithm
     */
    public static LocalDate getEasterDate(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int easterMonth = (h + l - 7 * m + 114) / 31;
        int p = (h + l - 7 * m + 114) % 31;
        int easterDay = p + 1;
        return LocalDate.of(year, easterMonth, easterDay);
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
     * Return the first date of the specified week day after or before an
     * input date. 
     * 
     * @param date a LocalDate to be searched from
     * @param isForward 
     * @param weekday
     * @return
     */
    @NonNull
    public static LocalDate findNextWeekDay(@NonNull LocalDate date, boolean isAfter, WeekDay weekday) {
        if (Week.convertWeekDay(date.getDayOfWeek()) == weekday) {
            return date;
        }
        return findNextWeekDay(getOffsetDate(date, (isAfter) ? 1 : -1), isAfter, weekday);
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
