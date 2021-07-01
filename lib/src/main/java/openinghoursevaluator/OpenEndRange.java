package openinghoursevaluator;

import ch.poole.openinghoursparser.TimeSpan;

/**
 * A TimeRange but is open ended. This is created as a stop gap measure for 
 * dealing with open end time interrupting. For more information, check See also.
 * <p>
 * An OpenEndRange may connect with another OpenEndRange which is the spill of
 * the next day, if any. If this is tagged to be removed (by setting true to
 * needsRemoving), it will not be taken account during rule adding and will be
 * cleaned when the next clean() cycle comes through.
 * 
 * @see https://github.com/opening-hours/opening_hours.js#time-ranges, last
 *      section of open end time
 */
public class OpenEndRange extends TimeRange {
    public static final String  DEFAULT_COMMENT = "open ended time";
    
    boolean         needsRemoving   = false;
    OpenEndRange    nextDaySpill    = null;


    public OpenEndRange() {
        // empty
    }

    /** Constructor for copying OpenEndRange */
    public OpenEndRange(OpenEndRange another) {
        this(another.getStart(), another.getEnd(), another.getStatus(), another.getComment());
    }

    /**
     * Constructor for creating a TimePoint with a Status, the result
     * will be a OpenEndRange with timepoint-timepoint+1.
     * Will throw error if timepoint = MAX_TIME aka 1440 aka at 24:00
     * 
     * @param timepoint a point in time
     * @param status Status to be set
     */
    public OpenEndRange(int timepoint, Status status) {
        super(timepoint, status, DEFAULT_COMMENT);
    }

    // Constructor with TimeSpan
    public OpenEndRange(TimeSpan timespan, Status status) {
        super(timespan, status, DEFAULT_COMMENT);
    }

    /**
     * Constructor for creating a OpenEndRange with a Status.
     * 
     * @param start start time, must be less than end time
     * @param end end time
     * @param status Status to be set
     */
    public OpenEndRange(int start, int end, Status status) {
        super(start, end, status, DEFAULT_COMMENT);
    }

    /**
     * Constructor for creating a TimePoint with a Status and a comment, the result
     * will be a OpenEndRange with timepoint-timepoint+1.
     * Will throw error if timepoint = MAX_TIME aka 1440 aka at 24:00
     * 
     * @param timepoint timepoint
     * @param status Status to be set
     * @param comment
     */
    public OpenEndRange(int timepoint, Status status, String comment) {
        super(timepoint, status, comment);
    }

    /** Constructor with TimeSpan and a comment */
    public OpenEndRange(TimeSpan timespan, Status status, String comment) {
        super(timespan, status, comment);
    }

    /**
     * Constructor for creating a OpenEndRange with a Status and a comment
     * 
     * @param start start time, must be less than end time
     * @param end end time
     * @param status Status to be set
     */
    public OpenEndRange(int start, int end, Status status, String comment) {
        super(start, end, status, comment);
    }

    public boolean isNeededRemoving() {
        return needsRemoving;
    }

    public OpenEndRange getNextDaySpill() {
        return nextDaySpill;
    }

    public void setNeededRemoving(boolean needsRemoving) {
        this.needsRemoving = needsRemoving;
    }

    public void setNextDaySpill(OpenEndRange nextDaySpill) {
        this.nextDaySpill = nextDaySpill;
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 37 * result + start;
        result = 37 * result + end;
        return result;
    }
}
