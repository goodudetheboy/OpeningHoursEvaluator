package openinghoursevaluator;

import java.util.ArrayList;
import java.util.List;

import ch.poole.openinghoursparser.TimeSpan;

/**
 * A specially created class that resemebles OpeningHoursParser.java, but resized for
 * specific uses in WeekDayRule. The difference is TimeSpan can support 48 hours, but
 * TimeRange can only support 24 hours, among other things.
 * 
 * If start value is equal to end value, then this TimeRange is considered a TimePoint
 * 
 */
public class TimeRange implements Comparable<TimeRange> {
    private static final int HOURS_24           = 1440;
    public static final int  UNDEFINED_TIME     = Integer.MIN_VALUE;
    public static final int  MIN_TIME          = 0;
    public static final int  MAX_TIME          = HOURS_24;

    int     start   = UNDEFINED_TIME;
    int     end     = UNDEFINED_TIME;
    Status  status  = null;

    /**
     * Default constructor
     */
    public TimeRange() {
        // empty on purpose
    }

    /**
     * Constructor for creating a TimePoint with a Status, the result
     * will be a TimeRange with timepoint-timepoint+1.
     * Will throw error if timepoint = MAX_TIME aka 1440 aka at 24:00
     * 
     * @param start timepoint
     * @param status Status to be set
     */
    public TimeRange(int timepoint, Status status) {
        this(timepoint, ++timepoint, status);
    }

    // Constructor with TimeSpan, TODO: fix later so it will fit into the stuff
    public TimeRange(TimeSpan timespan, Status status) {
        this(timespan.getStart(), (timespan.getEnd() != TimeSpan.UNDEFINED_TIME) ? timespan.getEnd() : timespan.getStart(), status);
    }

    /**
     * Constructor for creating a TimeRange with a Status.
     * If start = end, this will create a TimePoint instead (only start is defined, isTimepoint() will be true)
     * 
     * @param start start time, must be less than end time
     * @param end end time
     * @param status Status to be set
     */
    public TimeRange(int start, int end, Status status) {
        // TODO: sort out logic in this constructor
        if(start == end && start == MAX_TIME) {
            throw new IllegalArgumentException("Start and end cannot be both at " + MAX_TIME);
        }
        setStart(start);
        setEnd((start == end) ? ++end : end);
        this.status = status;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public Status getStatus() {
        return status;
    }

    public void setStart(int start) {
        if (start > MAX_TIME || start < MIN_TIME) {
            throw new IllegalArgumentException("Invalid time " + start + ", please keep time in 24 hours");
        }
        this.start = start;
    }

    public void setEnd(int end) {
        if (end < MIN_TIME) {
            throw new IllegalArgumentException("Invalid time" + end + ", please keep time in 24 hours");
        }
        this.end = (end <= MAX_TIME) ? end : MAX_TIME;
    }

    public void setStatus(Status status) {
        this.status = status;
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

        if (Tools.isBetween(start, otherStart, otherEnd)
                && Tools.isBetween(end, otherStart, otherEnd)) {
            return 1;
        }
        if (Tools.isBetween(start, otherStart, otherEnd)
                && end >= otherEnd) {
            return 2;
        }
        if (Tools.isBetween(end, otherStart, otherEnd)
                && start <= otherStart) {
           return 3;
        }
        if (Tools.isBetween(otherStart, start, end)
                && Tools.isBetween(otherEnd, start, end)) {
            return 4;
        }
        return 0;
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
        // TODO: add support for both timepoint
        int overlapsCode = this.overlapsCode(other);
        if (overlapsCode == 0) {
            return null;
        }

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
        overlap.setStart((startOverlap < endOverlap) ? startOverlap : endOverlap);
        overlap.setEnd((startOverlap > endOverlap) ? startOverlap : endOverlap);
        return overlap;
    }

    /**
     * Cut the TimeRange t with the TimeRange other
     * 
     * @param t the TimeRange to be cut
     * @param other the TimeRange that will cut t
     * @return a List containing TimeRange(s) resulting from the cut
     */
    public static List<TimeRange> cut(TimeRange t, TimeRange other) {
        List<TimeRange> result = new ArrayList<>();
        TimeRange overlap = t.overlapWith(other);
        Status oldStatus = t.getStatus();
        if (overlap != null) {
            TimeRange time1 = null;
            TimeRange time2 = null;
            int start = t.getStart();
            int end = t.getEnd();
            // TODO: add support for t.isTimePoint too
            int overlapS = overlap.getStart();
            int overlapE = overlap.getEnd();
            if (overlapS > start && overlapE < end) {
                time1 = new TimeRange(start, overlapS, oldStatus);
                time2 = new TimeRange(overlapE, end, oldStatus);
            } else if (overlapS == start && overlapE < end) {
                time1 = new TimeRange(overlapE, end, oldStatus);
            } else if (overlapE == end && overlapS > start) {
                time1 = new TimeRange(start, overlapS, oldStatus);
            }
            if (time1 != null) result.add(time1);
            if (time2 != null) result.add(time2);
        } else {
            result.add(t);
        }
        return result;
    }

    /**
     * Merge two TimeRanges into one TimeRange if there is an overlap and with similar Status,
     * if not this will return null
     * 
     * @param t1 first TimeRange
     * @param t2 second TimeRange
     * @return a TimeRange that is a merge of this two t1 and t2, null otherwise
     */
    public static TimeRange merge(TimeRange t1, TimeRange t2) {
        int overlapCode = t1.overlapsCode(t2);
        if(!t1.getStatus().equals(t2.getStatus()) || overlapCode == 0) {
            return null;
        }
        TimeRange result = new TimeRange();
        result.setStatus(t1.getStatus());
        switch(overlapCode) {
        case 1:
            return t2;
        case 2:
            result.setStart(t2.getStart());
            result.setEnd(t1.getEnd());
            return result;
        case 3:
            result.setStart(t1.getStart());
            result.setEnd(t2.getEnd());
            return result;
        case 4:
            return t1;            
        default:
            return null;
        }
    }

    @Override
    public String toString() {
        TimeSpan timespan = new TimeSpan();
        timespan.setStart(start);
        timespan.setEnd(end);
        return timespan.toString() + "(" + status + ")";
    }

    @Override
    public int compareTo(TimeRange o) {
        if (start != o.getStart()) {
            return start - o.getStart();
        }
        return end - o.getEnd();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof TimeRange) {
            TimeRange o = (TimeRange) other;
            return start == o.getStart() && end == o.getEnd() && status.equals(o.getStatus());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 37 * result + start;
        result = 37 * result + end;
        return result;
    }
}
