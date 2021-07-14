package openinghoursevaluator;

import java.time.LocalDate;
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
    public static final String DEFAULT_OPEN_END_COMMENT = "Until further notice";

    int     defaultYear;
    Month   defaultMonth;
    int     easterYear;

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
     * @throws OpeningHoursEvaluationException
     * @see https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification#monthday_range, explanation
     */
    public List<List<LocalDate>> processDateRange(DateRange dateRange, Week week)
            throws OpeningHoursEvaluationException {
        List<List<LocalDate>> result = new ArrayList<>();
        DateWithOffset start = dateRange.getStartDate();
        DateWithOffset end = dateRange.getEndDate();

        // set default to use throughout the process
        this.defaultYear = week.getYear();
        this.defaultMonth = start.getMonth();
        this.easterYear = defaultYear;

        // check for illegal date range
        checkError(dateRange);

        // there is always a start of DateRange
        List<LocalDate> subResult = new ArrayList<>();
        result.add(subResult);
        subResult.add(toDate(start, defaultYear, defaultMonth, true));
        if (end != null) {
            // handle when there is no year specified but there is year wrapping
            // @see https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification#explain:monthday_range:date_offset:to:date_offset
            if (compareStartAndEnd(start, end) > 0) {
                subResult.add(toDate(end, defaultYear+1, defaultMonth, false));
                // get year wraping from previous year
                List<LocalDate> otherResult = new ArrayList<>();
                otherResult.add(toDate(start, defaultYear-1, defaultMonth, true));
                otherResult.add(toDate(end, defaultYear, defaultMonth, false));
                result.add(otherResult);
            } else { 
                subResult.add(toDate(end, defaultYear, defaultMonth, false));
            }
        } else if (start.isOpenEnded()) {
            subResult.add(processOpenEnd(subResult.get(0)));
        } else if (start.getDay() == DateWithOffset.UNDEFINED_MONTH_DAY) {
            // set isStart as false here to get the last day of the month
            subResult.add(toDate(start, defaultYear, defaultMonth, false));
        }
        return result;
    }

    /**
     * Check is start DateWithOffset is before or after end DateWithOffset
     * 
     * @param start start of a DateRange
     * @param end end of a DateRange
     * @return <0 if d1 is before d2, >0 if d1 is after d2, =0 if d1 is same day as d2
     */
    private int compareStartAndEnd(DateWithOffset start, DateWithOffset end) {
        // Help fill start and end with easter year, if necessary
        fillEaster(start);
        fillEaster(end);
        // check year
        if (start.getYear() != YearRange.UNDEFINED_YEAR
                && end.getYear() != YearRange.UNDEFINED_YEAR) {
            return start.getYear() - end.getYear();
        }
        // check month
        if (end.getMonth() != null && start.getMonth() != end.getMonth()) {
            return start.getMonth().ordinal() - end.getMonth().ordinal();
        }
        // check days
        return start.getDay() - end.getDay();
    }

    /**
     * Process in case of open end range. Due to technical hardships, I cannot
     * cover the "the calendar range starts at this date and has no upper limit."
     * part of the definition of open end of monthday range yet, so I will create
     * a hard end to this open end range according to the following rule:
     * <ol>
     * <li> if month of start open end range is <= 6, then end of open end range
     *      is till the end of the specified year
     *      (for example, for "2021 May 15+", the open end range is 2021 May 15
     *      - 2021 Dec 31)
     * <li> if month of start open end range is from 6 to 9 (inclusive 9), then
     *      end of open end range is +8 months from the month of start
     *      (for example, for "2021 Aug+", the open end range is 2021 Aug 1 -
     *      2022 Apr 30)
     * <li> if month of start open end range is > 9, then end of open end range
     *      is +6 months from the month of start
     *      (for example, for "2021 Dec+", the open end range is 2021 Dec 1 -
     *      2022 Jun 30 next year)
     * </ol>
     * <p>
     * The year of the open end range has to be predefined by the DateRange
     * (for example "2019 Aug+"), or else it will be too ambiguous
     * <p>
     * Also, if there's no rule modifier, then the open end range will be
     * default to unknown
     * <p>
     * Check the links below for more info about open end of DateRange
     * 
     * @param startOpenEnd start of open end range
     * @return the end of the open end range w.r.t. to input start open end range
     * @see https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification#explain:monthday_range:date_offset:plus
     */
    private LocalDate processOpenEnd(LocalDate startOpenEnd) {
        int yearStart = startOpenEnd.getYear();
        int monthStart = startOpenEnd.getMonthValue();
        int yearEnd; 
        int monthEnd;
        if (monthStart <= 6) {
            yearEnd = yearStart;
            monthEnd = 12;
        } else {
            yearEnd = yearStart + 1;
            monthEnd = (monthStart > 9) ? monthStart - 12 + 6
                                        : monthStart - 12 + 8;
        }
        return LocalDate.of(yearEnd, monthEnd,
                        MonthRule.getMaxDayOfMonth(monthEnd, yearEnd));
    }

    /**
     * Help fill start and end with easter year, if necessary
     */
    private void fillEaster(DateWithOffset date) {
        if (isEaster(date)) {
            int year = (date.getYear() != YearRange.UNDEFINED_YEAR)
                        ? date.getYear()
                        : easterYear;
            LocalDate easter = getEasterDate(year);
            date.setDay(easter.getDayOfMonth());
            date.setMonth(MonthRule.convertMonth(easter));
        }
    }

    /**
     * Error checking between a start date and an end date of DateRange.
     * <p>
     * Failing condition TBU.
     * 
     * @param start start DateWithOffset of a DateRange
     * @param end end DateWithOffset of a DateRange
     * @throws OpeningHoursEvaluationException
     */
    public void checkError(DateRange dateRange) throws OpeningHoursEvaluationException {
        DateWithOffset start = dateRange.getStartDate();
        DateWithOffset end = dateRange.getEndDate();

        // check if open ended is ambiguous date of month (without a year)
        if (start.isOpenEnded() && start.getYear() == YearRange.UNDEFINED_YEAR) {
            throw new OpeningHoursEvaluationException("Currently open ended"
                        + " with ambiguous date of month without a year is not"
                        + " supported, please provide an exact year for "
                        + start);
        } else if (end == null) {
            return;
        }

        String rangeString = "(" + start + " - " + end + ")";
        // check if start year is not defined and end year is defined
        if (start.getYear() == YearRange.UNDEFINED_YEAR
                && end.getYear() != YearRange.UNDEFINED_YEAR) {
            throw new OpeningHoursEvaluationException("Year must be defined"
                        + " at start rather than end, this range " + rangeString
                        + " is meaningless");
        }
        // check if start is after end by checking date and month
        if (start.getYear() != YearRange.UNDEFINED_YEAR
                && compareStartAndEnd(start, end) > 0) {
            throwIllegalRange(start, end);
        }
        // check if start if after end by checking the year
        if (start.getYear() != YearRange.UNDEFINED_YEAR
                && (end.getYear() != YearRange.UNDEFINED_YEAR
                        && start.getYear() > end.getYear())) {
            throwIllegalRange(start, end);
        }
    }

    private void throwIllegalRange(DateWithOffset start, DateWithOffset end)
            throws OpeningHoursEvaluationException {
        String rangeString = "(" + start + " - " + end + ")";
        throw new OpeningHoursEvaluationException("Illegal range "
                    + rangeString + ", start date cannot be after end date");
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
    public static LocalDate toDate(DateWithOffset date, int optionalYear,
                                Month optionalMonth, boolean isStart) {
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
                dayInt = (isStart) ? 1 : MonthRule.getMaxDayOfMonth(monthInt, yearInt);
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

    /**
     * Check if input DateRange is open-ended
     * 
     * @param dateRange DateRange to be checked
     * @return check if input DateRange is open-ended
     */
    public static boolean isOpenEndDateRange(DateRange dateRange) {
        return dateRange.getStartDate().isOpenEnded();
    }
}
