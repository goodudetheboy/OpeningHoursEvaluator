package openinghoursevaluator;

import javax.annotation.Nullable;

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
        this.comment = DEFAULT_COMMENT;
    }

    /**
     * Constructor for copying OpenEndRange
     * 
     * @param another another OpenEndRange to copy
     */
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
     * Constructor for creating a TimePoint with a Status and a comment,
     * the result will be a OpenEndRange with timepoint-timepoint+1.
     * Will throw error if timepoint = MAX_TIME aka 1440 aka at 24:00
     * 
     * @param timepoint timepoint
     * @param status Status to be set
     * @param comment
     */
    public OpenEndRange(int timepoint, Status status, String comment) {
        super(timepoint, status, comment);
        setComment(comment);
    }

    /** 
     * Constructor with TimeSpan,a Status and a comment
     *
     * @param timepoint timespan
     * @param status Status to be set
     * @param comment comment
     */
    public OpenEndRange(TimeSpan timespan, Status status, String comment) {
        super(timespan, status, comment);
        setComment(comment);
    }

    /**
     * Constructor for creating a OpenEndRange with a Status and a comment.
     * If a comment is not specified (== null), the DEFAULT_COMMENT will
     * be set in its place instead.
     * 
     * @param start start time, must be less than end time
     * @param end end time
     * @param status Status to be set
     * @param comment a comment
     */
    public OpenEndRange(int start, int end, Status status, String comment) {
        super(start, end, status, comment);
        setComment(comment);
    }

    public boolean isNeededRemoving() {
        return needsRemoving;
    }

    @Nullable
    public OpenEndRange getNextDaySpill() {
        return nextDaySpill;
    }

    public void setNeededRemoving(boolean needsRemoving) {
        this.needsRemoving = needsRemoving;
    }

    public void setNextDaySpill(OpenEndRange nextDaySpill) {
        this.nextDaySpill = nextDaySpill;
    }

    /**
     * Returns true if input TimeRange is an OpenEndRange and needed removing
     * 
     * @param timerange input TimeRange
     * @return true if input TimeRange is an OpenEndRange and needed removing
     */
    public static boolean checkNeededRemoving(TimeRange timerange) {
        return (timerange instanceof OpenEndRange)
                    && ((OpenEndRange) timerange).isNeededRemoving();
    }

    @Override
    public void setComment(String comment) {
        this.comment = (comment != null) ? comment : DEFAULT_COMMENT;
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
