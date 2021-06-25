package openinghoursevaluator;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

import ch.poole.openinghoursparser.Nth;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.WeekDay;
import ch.poole.openinghoursparser.WeekDayRange;

public class Week {
    int     weekOfYear;
    int     weekOfMonth;
    EnumMap<WeekDay, WeekDayRule>   weekDayStorage;
    List<Rule>                      rules;
    List<TimeRange> previousSpill   = null;
    WeekDayRule dayAfter        = null;
    // this is for custom week
    WeekDay startWeekDay        = null;
    WeekDay endWeekDay          = null;

    public Week() {
        // nothing here
    }

    public Week(List<Rule> rules, int weekOfMonth) {
        // setting weekOfYear and weekOfMonth
        this(rules, weekOfMonth, WeekDay.MO, WeekDay.SU);
    }

    public Week(List<Rule> rules, int weekOfMonth, WeekDay oneWeekDay) {
        this(rules, weekOfMonth, oneWeekDay, oneWeekDay);
    }

    public Week(List<Rule> rules, int weekOfMonth, WeekDay startWeekDay, WeekDay endWeekDay) {
        if(startWeekDay.ordinal() > endWeekDay.ordinal()) {
            throw new IllegalArgumentException("Start weekday cannot be after end weekday");
        }
        this.weekOfMonth = weekOfMonth;
        weekDayStorage = new EnumMap<>(WeekDay.class);
        this.rules = rules;
        this.startWeekDay = startWeekDay;
        this.endWeekDay = endWeekDay;
        populate();
    }

    public void setWeekOfMonth(int weekOfMonth) {
        this.weekOfMonth = weekOfMonth;
    }

    public void setPreviousSpill(List<TimeRange> previousSpill) {
        this.previousSpill = previousSpill;
    }

    public void setStartWeekDay(WeekDay startWeekDay) {
        this.startWeekDay = startWeekDay;
    }

    public void setEndWeekDay(WeekDay endWeekDay) {
        this.endWeekDay = endWeekDay;
    }

    public int getWeekOfMonth() {
        return weekOfMonth;
    }

    public List<TimeRange> getPreviousSpill() {
        return previousSpill;
    }

    public WeekDay getStartWeekday() {
        return startWeekDay;
    }

    public WeekDay getEndWeekDay() {
        return endWeekDay;
    }

    public WeekDayRule getStartWeekDayRule() {
        return weekDayStorage.get(startWeekDay);
    }

    /**
     * Build Week with the current openingHours string
     * 
     * @param isStrict strict or not
     */
    public void build() {
        populate();
        for (Rule rule : rules) {
            update(rule);
        }
    }

    /**
     * Update Week with a rule
     * 
     * @param rule a Rule
     */
    public void update(Rule rule) {
        applyPreviousSpill();
        List<WeekDayRange> weekdayRange;
        if (rule.getDays() != null) {
            weekdayRange = rule.getDays();
        } else {
            WeekDayRange allWeek = new WeekDayRange();
            allWeek.setStartDay(WeekDay.MO);
            allWeek.setEndDay(WeekDay.SU);
            weekdayRange = new ArrayList<>();
            weekdayRange.add(allWeek);
        }
        for (WeekDayRange weekdays : weekdayRange) {
            updateWithWeekDay(rule, weekdays);
        }
        clean();
    }

    /** update() helper */
    private void updateWithWeekDay(Rule rule, WeekDayRange weekdays) {
        // WeekDayRange range = getApplicableWeekDay(weekdays);
        WeekDayRange range = weekdays;
        if(range != null) {
            List<Nth> nths = range.getNths();
            if(nths == null || isApplicableNth(nths, weekOfMonth)) {
                WeekDay current = range.getStartDay();
                WeekDay end = (range.getEndDay() != null)
                                    ? range.getEndDay()
                                    : current;
                do {
                    if (hasWeekDay(current)) {
                        weekDayStorage.get(current).build(rule);
                    }
                } while ((current = getNextWeekDay(current)) 
                            != getNextWeekDay(end));
            }
        }
    }

    private WeekDayRange getApplicableWeekDay(WeekDayRange weekdays) {
        WeekDayRange result = new WeekDayRange();
        WeekDay start = weekdays.getStartDay();
        WeekDay end = weekdays.getEndDay();
        if (end == null) {
            return (hasWeekDay(start)) ? weekdays : null;
        }
        int startInt = start.ordinal();
        int endInt = end.ordinal();
        int startWeekInt = startWeekDay.ordinal();
        int endWeekInt = endWeekDay.ordinal();
        if (startInt > endInt) {
            end = WeekDay.SU;
            endInt = WeekDay.SU.ordinal();
        }
        if (endInt < startWeekInt
                || startInt > endWeekInt) {
            return null;
        }
        result.setStartDay((startInt >= startWeekInt)
                                ? start
                                : startWeekDay);
        result.setEndDay((end.ordinal() <= endWeekDay.ordinal())
                                ? end
                                : endWeekDay);
        result.setNths(weekdays.getNths());
        return result;
    }

    private void updateLastWeekSunday(Rule rule) {
        List<WeekDayRange> weekdayRange;
        if (rule.getDays() != null) {
            weekdayRange = rule.getDays();
        } else {
            WeekDayRange allWeekDays = new WeekDayRange();
            allWeekDays.setStartDay(WeekDay.MO);
            allWeekDays.setEndDay(WeekDay.SU);
            weekdayRange = new ArrayList<>();
            weekdayRange.add(allWeekDays);
        }
        for (WeekDayRange weekdays : weekdayRange) {
            WeekDay current = weekdays.getStartDay();
            WeekDay end = (weekdays.getEndDay() != null)
                            ? weekdays.getEndDay()
                            : current;
            do {
                weekDayStorage.get(current).build(rule);
            } while((current = getNextWeekDay(current)) != getNextWeekDay(end));
        }
    }

    /**
     * @return true if input Rule can be applied to any day in the week
     */
    private boolean isUniversal(Rule rule) {
        return rule.isTwentyfourseven()
                || (rule.getDays() == null && rule.getTimes() == null);
    }

    /**
     * Evaluate the stored OH string with a time to see if it's opening or closed
     * 
     * @param time input LocalDateTime
     * @return the result of the evaluation
     */
    public Result checkStatus(LocalDateTime time) {
        WeekDay weekday = toWeekDay(time.getDayOfWeek());
        WeekDayRule toCheck = weekDayStorage.get(weekday);
        if (toCheck == null) {
            return null;
        } else {
            return weekDayStorage.get(weekday).checkStatus(time);
        }
    }

    /**
     * Convert java.time.DayOfWeek enum to OpeningHoursParser.WeekDay enum
     * 
     * @param dayOfWeek an enum of DayOfWeek to be converted
     * @return an equivalent weekday in WeekDay enum
     */
    public static WeekDay toWeekDay(DayOfWeek dayOfWeek) {
        int dayOfWeekNth = dayOfWeek.ordinal();
        for (WeekDay weekday : WeekDay.values()) {
            if (weekday.ordinal() == dayOfWeekNth) {
                return weekday;
            }
        }
        return null;
    }
    
    /**
     * Return the next WeekDay wrt a current WeekDay
     * 
     * @param current the current WeekDay
     * @return the following WeekDay
     */
    public static WeekDay getNextWeekDay(WeekDay current) {
        WeekDay[] weekdays = WeekDay.values();
        int next = (current.ordinal()+1) % weekdays.length;
        return weekdays[next];
    }

    public static WeekDay getPreviousWeekDay(WeekDay current) {
        WeekDay[] weekdays = WeekDay.values();
        int previous = (current.ordinal()-1 + weekdays.length) % weekdays.length;
        return weekdays[previous];
    }

    /** Clean all WeekDayRule in this Week */
    public void clean() {
        for (WeekDay weekday : WeekDay.values()) {
            if(weekDayStorage.get(weekday) != null) {
                weekDayStorage.get(weekday).clean();
            }
        }
    }

    /** Reset this Week by removing all current WeekDayRule and filling it with empty ones*/
    public void reset() {
        weekDayStorage = new EnumMap<>(WeekDay.class);
    }

    /** Populate this WeekRule with empty WeekDayRule. */
    public void populate() {
        populateHelper(startWeekDay);
        dayAfter = new WeekDayRule();
        weekDayStorage.get(endWeekDay).setNextDayRule(dayAfter);
        if (previousSpill != null) {
            for (TimeRange spill : previousSpill) {
                weekDayStorage.get(startWeekDay).addSpill(spill);
            }
            // reset spill
            previousSpill = null;
        }
    }

    private void populateHelper(WeekDay current) {
        weekDayStorage.put(current, new WeekDayRule(current));
        WeekDay nextDay = getNextWeekDay(current);
        if(current != endWeekDay) {
            populateHelper(nextDay);
            weekDayStorage.get(current).setNextDayRule(weekDayStorage.get(nextDay));
        }
    }

    public void connect(Week other) {
        WeekDayRule nextStartDay = other.weekDayStorage.get(other.getStartWeekday());
        weekDayStorage.get(endWeekDay).setNextDayRule(nextStartDay);
    }

    public List<TimeRange> getWeekSpill() {
        return dayAfter.getSpilledTime();
    }

    public void applyPreviousSpill() {
        if (previousSpill != null) {
            weekDayStorage.get(startWeekDay).setSpilledTime(previousSpill);
            previousSpill = null;
        }
    }

    public static List<TimeRange> simulatePreviousSpill(Week week, Rule rule) {
        WeekDay previousDay = Week.getPreviousWeekDay(week.getStartWeekday());
        int weekOfMonth = (previousDay == WeekDay.SU)
                            ? MonthRule.getPreviousWeekOfMonth(week.getWeekOfMonth())
                            : week.getWeekOfMonth();
        Week simulatedSunday = new Week(null, weekOfMonth, previousDay);
        simulatedSunday.update(rule);
        return simulatedSunday.getWeekSpill();
    }

    /**
     * Check if input weekOfMonth is within Nth range, or equals to startNth
     * if there's no endNth
     * 
     * @param nth input Nth
     * @param weekOfMonth week of month (max 5)
     * @return if applicable or not
     */
    public static boolean isApplicableNth(List<Nth> nths, int weekOfMonth) {
        for(Nth nth : nths) {
            if (nth.getEndNth() == Nth.INVALID_NTH && weekOfMonth == nth.getStartNth()
                    || Utils.isBetween(weekOfMonth, nth.getStartNth(), nth.getEndNth())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasWeekDay(WeekDay weekday) {
        return Utils.isBetween(weekday.ordinal(), startWeekDay.ordinal(),
                                endWeekDay.ordinal());
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        WeekDay current = startWeekDay;
        do {
            b.append(current);
            b.append(" : ");
            if (weekDayStorage.get(current) != null) {
                b.append(weekDayStorage.get(current).toString());
            }
            b.append(System.getProperty("line.separator"));
        } while ((current = getNextWeekDay(current)) != getNextWeekDay(endWeekDay));
        return b.toString();
    }
}
