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
        List<Nth> nths = weekdays.getNths();
        if(nths == null || isApplicableNth(nths, weekOfMonth)) {
            WeekDay current = weekdays.getStartDay();
            WeekDay end = (weekdays.getEndDay() != null)
                                ? weekdays.getEndDay()
                                : current;
            do {
                // TODO: optimize this part
                if (hasWeekDay(current)) {
                    weekDayStorage.get(current).build(rule);
                }
            } while ((current = getNextWeekDay(current)) 
                        != getNextWeekDay(end));
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
     * Check if this WeekDay is between this Week's start WeekDay and end WeekDay
     * 
     * @param weekday WeekDay to check
     * @return true if this WeekDay is between this Week's start WeekDay and end WeekDay, false otherwise
     */
    public boolean hasWeekDay(WeekDay weekday) {
        return Utils.isBetween(weekday.ordinal(), startWeekDay.ordinal(),
                                endWeekDay.ordinal());
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

    /**
     * Return the previous WeekDay wrt a current WeekDay
     * 
     * @param current the current WeekDay
     * @return the previous WeekDay
     */
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

    /** Helper of populate() */
    private void populateHelper(WeekDay current) {
        weekDayStorage.put(current, new WeekDayRule(current));
        WeekDay nextDay = getNextWeekDay(current);
        if(current != endWeekDay) {
            populateHelper(nextDay);
            weekDayStorage.get(current).setNextDayRule(weekDayStorage.get(nextDay));
        }
    }

    /**
     * Connect a partial Week to another partial week by setting the nextDayRule of 
     * the endWeekDay's WeekDayRule of this week to the startWeekDay's WeekDayRule of
     * the other Week
     * 
     * @param other the other Week to be connected
     */
    public void connect(Week other) {
        WeekDayRule nextStartDay = other.weekDayStorage.get(other.getStartWeekday());
        weekDayStorage.get(endWeekDay).setNextDayRule(nextStartDay);
    }

    /**
     * @return the spill of this Week
     */
    public List<TimeRange> getWeekSpill() {
        return dayAfter.getSpilledTime();
    }

    /**
     * Set the spill of the startWeekDay's WeekDayRule of this Week with any previous
     * spill, if any
     */
    public void applyPreviousSpill() {
        if (previousSpill != null) {
            weekDayStorage.get(startWeekDay).setSpilledTime(previousSpill);
            previousSpill = null;
        }
    }

    /**
     * Used during MonthRule build(). This is to get the time spills of previous
     * week
     * 
     * @param week a Week to be simulated
     * @param rule a Rule to be applied
     * @return the simulated spill
     */
    public static List<TimeRange> simulateSpill(Week week, Rule rule) {
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

    /**
     * Create and return a List<Week> containing one full Week (from MO to SU)
     * 
     * @param weekOfMonth weekOfMonth
     * @return a List<Week> containing a full Week
     */
    public static List<Week> createEmptyWeek(int weekOfMonth) {
        return createEmptyWeek(weekOfMonth, null, 0);
    }

    /**
     * Create and return a List<Week> containing two Week, cut by the specified cutoff.
     * If cutoff is null, creates a full Week instead.
     * 
     * @param weekOfMonth week of month of first part
     * @param cutoff a WeekDay cutoff (belongs to the first part of the Week)
     * @param otherWeekOfMonth week of month of second part
     * @return a List<Week> containing two Week, cut by the specified cutoff
     */
    public static List<Week> createEmptyWeek(int weekOfMonth, WeekDay cutoff, int otherWeekOfMonth) {
        List<Week> result = new ArrayList<>();
        if(cutoff == null) {
            Week week = new Week(null, weekOfMonth);
            result.add(week);
        } else {
            Week first = new Week(null, weekOfMonth, WeekDay.MO, cutoff);
            Week second = new Week(null, otherWeekOfMonth, Week.getNextWeekDay(cutoff), WeekDay.SU);
            first.connect(second);
            result.add(first);
            result.add(second);
        }
        return result;
    }

    /**
     * @param time the LocalDateTime of desired week
     * @return the first day of a week
     */
    public static LocalDateTime getFirstDayOfWeek(LocalDateTime time) {
        return time.with(WeekFields.ISO.dayOfWeek(), 1);
    }

    /**
     * @param time the LocalDateTime of desired week
     * @return the last day of a week
     */
    public static LocalDateTime getLastDayOfWeek(LocalDateTime time) {
        return time.with(WeekFields.ISO.dayOfWeek(), 7);
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
