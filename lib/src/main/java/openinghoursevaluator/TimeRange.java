package openinghoursevaluator;

import java.rmi.UnexpectedException;
import java.util.List;

import ch.poole.openinghoursparser.TimeSpan;

/**
 * A specially created class that resemebles OpeningHoursParser.java, but resized for
 * specific uses in WeekDayRule. The difference is TimeSpan can support 48 hours, but
 * TimeRange can only support 24 hours, among other things.
 * 
 * If only the start value is defined, then this TimeRange is considered a TimePoint
 * 
 */
public class TimeRange {
    private static final int HOURS_24           = 1440;
    public static final int  UNDEFINED_TIME     = Integer.MIN_VALUE;
    public static final int  MIN_TIME          = 0;
    public static final int  MAX_TIME          = HOURS_24;

    int start   = UNDEFINED_TIME;
    int end     = UNDEFINED_TIME;

    /**
     * Default constructor
     */
    public TimeRange() {
        // empty on purpose
    }

    /**
     * Constructor for creating a TimePoint
     * 
     * @param start timepoint
     */
    public TimeRange(int start) {
        setStart(start);
    }

    // Constructor with TimeSpan, TODO: fix later so it will fit into the stuff
    public TimeRange(TimeSpan timespan) {
        this(timespan.getStart(), timespan.getEnd());
    }

    /**
     * Constructor for creating a TimeRange. If start is less than end, the two will be switched.
     * 
     * @param start start time
     * @param end end time
     */
    public TimeRange(int start, int end) {
        setStart((start < end) ? start : end);
        setEnd((end > start) ? end : start);
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public void setStart(int start) {
        if(start > MAX_TIME || start < MIN_TIME) {
            throw new IllegalArgumentException("Invalid time, please keep time in 24 hours");
        }
        this.start = start;
    }

    public void setEnd(int end) {
        if(end > MAX_TIME || end < MIN_TIME) {
            throw new IllegalArgumentException("Invalid time, please keep time in 24 hours");
        }
        this.end = end;
    }


    /**
     * Returns a code indicating how the other TimeRange is overlapped with this TimeRange,
     * adhering to the following:
     * 
     * 0: Doesn't overlap
     * 1: This TimeRange is inside other TimeRange
     * 2: This TimeRange has start inside the other TimeRange and end outside
     * 3: This TimeRange has start outside the other TimeRange and end inside
     * 4: The other TimeRange is inside this TimeRange
     * 
     * @param other the other TimeRange
     * @return 1
     */
    public int overlapsCode(TimeRange other) {
        int otherStart = other.getStart();
        int otherEnd = other.getEnd();

        if(Tools.isBetween(start, otherStart, otherEnd) && Tools.isBetween(end, otherStart, otherEnd))  return 1;
        else if(Tools.isBetween(start, otherStart, otherEnd) && end >= otherEnd)                        return 2;
        else if(Tools.isBetween(end, otherStart, otherEnd) && start <= otherStart)                      return 3;
        else if(Tools.isBetween(otherStart, start, end) && Tools.isBetween(otherEnd, start, end))       return 4;
        else                                                                                            return 0;
    }

    /**
     * Check if the other TimeRange overlaps with this TimeRange
     * 
     * @param other the other TimeRange
     * @return true if it is, false otherwise
     */
    public boolean isOverlapped(TimeRange other) {
        return (overlapsCode(other) != 0);
    }

    /**
     * Returns a TimeRange where it is the overlap time between this and the other TimeRange
     * 
     * @param other the other TimeRange
     * @return a TimeRange that overlaps both this and other TimeRange, null if it can't be merged
     */
    public TimeRange overlapWith(TimeRange other) {
        // if((isTimePoint() && !other.isTimePoint()) || (!isTimePoint() && other.isTimePoint()))
        //     return null;
        // else
        //     return new TimeRange(getTimePoint());

        int overlapsCode = this.overlapsCode(other);
        if(overlapsCode == 0) return null;

        int otherStart = other.getStart();
        int otherEnd = other.getEnd();
        int startOverlap = -1;
        int endOverlap = -1;

        switch(overlapsCode) {
            case 1:
                startOverlap = start;
                endOverlap = end;
                break;
            case 2:
                startOverlap = start;
                endOverlap = otherEnd;            
                break;
            case 3:
                startOverlap = end;
                endOverlap = otherStart;
                break;
            case 4:
                startOverlap = otherStart;
                endOverlap = otherEnd;
                break; 
            default: // hopefully this never gets here
        }

        TimeRange overlap = new TimeRange();
        if(startOverlap == endOverlap) {
            overlap.setStart(startOverlap);
        } else {
            overlap.setStart((startOverlap < endOverlap) ? startOverlap : endOverlap);
            overlap.setEnd((startOverlap > endOverlap) ? startOverlap : endOverlap);
        }
        return overlap;
    }

    /**
     * @return true if this TimeRange only has start value, or just a point in time, false otherwise
     */
    public boolean isTimePoint() {
        return end == UNDEFINED_TIME;
    }

    public int getTimePoint() {
        return getStart();
    }

    @Override
    public String toString() {
        TimeSpan timespan = new TimeSpan();
        timespan.setStart(start);
        if(!isTimePoint()) timespan.setEnd(end);
        return timespan.toString();
    }
}
