package openinghoursevaluator;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import ch.poole.openinghoursparser.Rule;
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
    private static final int    HOURS_24           = 1440;
    public static final int     UNDEFINED_TIME     = Integer.MIN_VALUE;
    public static final int     MIN_TIME           = 0;
    public static final int     MAX_TIME           = HOURS_24;
    public static final String  DEFAULT_OPEN_ENDED_COMMENT = "open ended time";

    int         start       = UNDEFINED_TIME;
    int         end         = UNDEFINED_TIME;
    Status      status      = null;
    String      comment     = null;
    Rule        defRule     = null;
    boolean     isFallback  = false;

    /**
     * Default constructor
     */
    public TimeRange() {
        // empty on purpose
    }

    /** Constructor for copying TimeRange */
    public TimeRange(TimeRange other) {
        setStart(other.start);
        setEnd(other.end);
        setStatus(other.status);
        setComment(other.comment);
        setDefiningRule(other.defRule);
        setFallback(other.isFallback);
    }

    /**
     * Constructor for creating a TimePoint with a Status, the result
     * will be a TimeRange with timepoint-timepoint+1.
     * <p>
     * Will throw error if timepoint = MAX_TIME aka 1440 aka at 24:00
     * 
     * @param timepoint a point in time
     * @param status Status to be set
     */
    public TimeRange(int timepoint, Status status) {
        this(timepoint, ++timepoint, status);
    }

    /**
     * Constructor for creating a TimePoint with a Status and an optional comment,
     * the result will be a TimeRange with timepoint-timepoint+1.
     * <p>
     * Will throw error if timepoint = MAX_TIME aka 1440 aka at 24:00
     * 
     * @param timepoint timepoint
     * @param status Status to be set
     * @param comment
     */
    public TimeRange(TimeSpan timespan, Status status) {
        this(timespan.getStart(),
            (timespan.getEnd() != TimeSpan.UNDEFINED_TIME) 
                ? timespan.getEnd()
                : timespan.getStart(),
            status);
    }

    /**
     * Constructor for creating a TimeRange with a Status.
     * 
     * @param start start time, must be less than end time
     * @param end end time
     * @param status Status to be set
     */
    public TimeRange(int start, int end, Status status) {
        // TODO: sort out logic in this constructor
        if (start == end && start == MAX_TIME) {
            throw new IllegalArgumentException("Start and end cannot be both at " + MAX_TIME);
        }
        if (start > end) {
            throw new IllegalArgumentException("Start cannot be after end");
        }
        setStart(start);
        setEnd((start == end) ? ++end : end);
        setStatus(status);
        setDefiningRule(defRule);
    }

    /**
     * Constructor for creating a TimePoint with a Status and an optional comment,
     * the result will be a TimeRange with timepoint-timepoint+1.
     * <p>
     * Will throw error if timepoint = MAX_TIME aka 1440 aka at 24:00
     * 
     * @param timepoint timepoint
     * @param status Status to be set
     * @param comment
     */
    public TimeRange(int timepoint, Status status, @Nullable String comment) {
        this(timepoint, ++timepoint, status, comment);
    }

    /**
     * Constructor with TimeSpan and a comment
     * 
     * @param timespan a TimeSpan that this TimeRange will be created from
     * @param comment optional comment
     * @param status Status to be set
     */
    public TimeRange(TimeSpan timespan, Status status, @Nullable String comment) {
        this(timespan.getStart(),
            (timespan.getEnd() != TimeSpan.UNDEFINED_TIME)  ? timespan.getEnd()
                                                            : timespan.getStart(),
            status, comment);
    }

    /**
     * Constructor for creating a TimeRange with a Status and a comment
     * 
     * @param start start time, must be less than end time
     * @param end end time
     * @param status Status to be set
     */
    public TimeRange(int start, int end, Status status, @Nullable String comment) {
        this(start, end, status);
        setComment(comment);
    }
    
    /**
     * @return start time of this TimeRange
     */
    public int getStart() {
        return start;
    }

    /**
     * @return end time of this TimeRange
     */
    public int getEnd() {
        return end;
    }

    /**
     * @return Status of this TimeRange
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @return comment of this TimeRange
     */
    @Nullable
    public String getComment() {
        return comment;
    }

    /**
     * @return defining rule of this TimeRange
     */
    @Nullable
    public Rule getDefiningRule() {
        return defRule;
    }

    /**
     * @return true if this TimeRange is made from a fallback rule
     */
    public boolean isFallback() {
        return isFallback;
    }

    /**
     * Set start time in minutes of this TimeRange
     * 
     * @param start start time to be set
     */
    public void setStart(int start) {
        if (start > MAX_TIME || start < MIN_TIME) {
            throw new IllegalArgumentException("Start time " + start + " is outside current day");
        }
        this.start = start;
    }

    /**
     * Set the end time in minutes of this TimeRange
     * 
     * @param end end time to be set
     */
    public void setEnd(int end) {
        if (end > MAX_TIME || end < MIN_TIME) {
            throw new IllegalArgumentException("Invalid time" + end + ", please keep time in 24 hours");
        }
        this.end = end;
    }

    /**
     * Set the Status of this TimeRange
     * 
     * @param status Status to be set
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Set the comment of this TimeRange
     * 
     * @param comment comment to be set
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Set the defining rule of this TimeRange. (optional)
     * 
     * @param definingRule Rule that defines this TimeRange
     */
    public void setDefiningRule(Rule defRule) {
        this.defRule = defRule;
    }

    /**
     * Set the fallback status of this TimeRange.
     * 
     * @param isFallback true to set as fallback
     */
    public void setFallback(boolean isFallback) {
        this.isFallback = isFallback;
    }

    /**
     * Check if this TimeRange has a comment
     */
    public boolean hasComment() {
        return comment != null;
    }

    /**
     * Returns a code indicating how the other TimeRange is overlapped
     * with this TimeRange, adhering to the following:
     * <ul>
     * <li>0: Doesn't overlap
     * <li>1: This TimeRange is inside other TimeRange
     * <li>2: This TimeRange has start inside the other TimeRange and
     * end outside
     * <li>3: This TimeRange has start outside the other TimeRange and
     * end inside
     * <li>4: The other TimeRange is inside this TimeRange
     * </ul>
     * <p>
     * @param other the other TimeRange
     * @return a number (0-4) adhering to the above specification
     */
    public int overlapsCode(TimeRange other) {
        int otherStart = other.getStart();
        int otherEnd = other.getEnd();
        if (start > otherEnd || end < otherStart) {
            return 0;
        }
        if (Utils.isBetween(start, otherStart, otherEnd)) {
            return (Utils.isBetween(end, otherStart, otherEnd)) ? 1 : 2;
        } else {
            return (Utils.isBetween(end, otherStart, otherEnd)) ? 3 : 4;
        }
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
        int overlapsCode = this.overlapsCode(other);
        if (overlapsCode == 0) {
            return null;
        }

        int otherStart = other.getStart();
        int otherEnd = other.getEnd();
        int startOverlap = -1;
        int endOverlap = -1;

        switch (overlapsCode) {
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
        TimeRange result = new TimeRange();
        result.setStart((startOverlap > endOverlap) ? endOverlap : startOverlap);
        result.setEnd((endOverlap < startOverlap) ? startOverlap : endOverlap);
        return result;
    }

    /**
     * Cut the TimeRange t with the TimeRange other
     * 
     * @param t the TimeRange to be cut
     * @param other the TimeRange that will cut t
     * @return a List containing TimeRange(s) resulting from the cut
     */
    public List<TimeRange> cut(TimeRange other) {
        List<TimeRange> result = new ArrayList<>();
        TimeRange overlap = overlapWith(other);
        Status oldStatus = status;
        if (overlap != null) {
            if (overlap.getStart() > start) {
                TimeRange startRange = new TimeRange(start, overlap.getStart(),
                                            oldStatus, comment);
                startRange.setDefiningRule(defRule);
                startRange.setFallback(isFallback);
                result.add(startRange);
            } 
            if (overlap.getEnd() < end) {
                TimeRange endRange = new TimeRange(overlap.getEnd(), end,
                                            oldStatus, comment);
                endRange.setDefiningRule(defRule);
                endRange.setFallback(isFallback);
                result.add(endRange);
            }
        } else {
            result.add(this);
        }
        return result;
    }

    /**
     * This TimeRange is mergeable with the other TimeRange when:
     * both's Status is equal, and either they both have comments
     * and the comments are equal or neither of them have comments
     * 
     * @param other the other TimeRange to be checked for merge
     * @return true if mergeable, false otherwise
     */
    public boolean isMergeable(TimeRange other) {
        // TODO: consider adding overlapCode instead of inside the merge
        return status.equals(other.getStatus())
                    && ((hasComment() && other.hasComment()
                            && comment.equals(other.getComment()))
                        || !hasComment() && !other.hasComment());
    }                   

    /**
     * Merge two TimeRanges into one TimeRange if there is an overlap and with similar Status,
     * if not this will return null
     * 
     * @param other second TimeRange
     * @return a TimeRange that is a merge of this two t1 and t2, null otherwise
     */
    public TimeRange merge(TimeRange other) {
        int overlapCode = overlapsCode(other);
        if (!this.isMergeable(other) || overlapCode == 0) {
            return null;
        }
        TimeRange result = new TimeRange();
        result.setComment(comment);
        result.setStatus(status);
        switch (overlapCode) {
        case 1:
            return other;
        case 2:
            result.setStart(other.getStart());
            result.setEnd(end);
            return result;
        case 3:
            result.setStart(start);
            result.setEnd(other.getEnd());
            return result;
        case 4:
            return this;            
        default:
            return null;
        }
    }

    public static TimeSpan checkTimeSpill(TimeSpan timespan) {
        TimeSpan spilledTime = null;
        if (timespan.getEnd() > TimeRange.MAX_TIME) {
            spilledTime = new TimeSpan();
            spilledTime.setStart(0);
            spilledTime.setEnd(timespan.getEnd() - TimeRange.MAX_TIME); 
            spilledTime.setInterval(timespan.getInterval());
        }
        return spilledTime;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        TimeSpan timespan = new TimeSpan();
        timespan.setStart(start);
        timespan.setEnd(end);
        b.append(timespan.toString() + "(" + status);
        if(hasComment()) {
            b.append(" - \"" + comment +"\"");
        }
        b.append(")");
        return b.toString();
    }

    /**
     * Similar to toString(), but this also includes defining Rule from which
     * this TimeRange was created
     * 
     * @return
     */
    public String toDebugString() {
        return toString() + " [" + defRule + "]";
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
