package openinghoursevaluator;

import java.time.LocalDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ch.poole.openinghoursparser.Rule;

/**
 * A custom result class for the OpeningHoursEvaluator. This is also used for
 * storing getting next event (open/close next)
 */
public class Result {
    Status  status      = null;
    String  comment     = null;
    Rule    defRule     = null;

    // reserved for when asking for open/close next
    LocalDateTime   nextEventTime   = null;
    LocalDateTime   lastEventTime   = null;
    boolean         isAlways        = false;

    /**
     * Default constructor
     */
    public Result() {
        // empty
    }

    /**
     * Constructor for a Result with a Status, an optional comment and an
     * optional defining Rule
     * 
     * @param status a Status
     * @param comment an optional comment
     * @param defRule an optional defining Rule
     */
    public Result(@Nonnull Status status, @Nullable String comment, @Nullable Rule defRule) {
        setStatus(status);
        setComment(comment);
        setDefiningRule(defRule);
    }

    /**
     * Constructor for a Result with a TimeRange
     * 
     * @param timerange
     */
    public Result(TimeRange timerange) {
        this(timerange.getStatus(), timerange.getComment(), timerange.getDefiningRule());
    }

    /**
     * Get the Status in this Result. If this result is used for getting
     * open/close next, this Status will reflect the status of those event
     * 
     * @return the Status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @return the defining Rule
     */
    public Rule getDefiningRule() {
        return defRule;
    }

    /**
     * @return get next differing event time
     */
    @Nullable
    public LocalDateTime getNextEventTime() {
        return nextEventTime;
    }

    /**
     * @return get last differing event time
     */
    @Nullable
    public LocalDateTime getLastEventTime() {
        return lastEventTime;
    }

    /**
     * @return check if next/last event is always in this.status
     */
    public boolean isAlways() {
        return isAlways;
    }

    /**
     * @return true if this Result has a comment
     */
    public boolean hasComment() {
        return comment != null;
    }

    /**
     * Set the status of this Result. 
     * 
     * @param status status to be set
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Set the comment of this Result
     * 
     * @param comment comment to be set
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Set the defining Rule of this Result
     * 
     * @param defRule defining Rule to be set
     */
    public void setDefiningRule(Rule defRule) {
        this.defRule = defRule;
    }
 
    /**
     * Set next differing event. A Status should also be set alongside this
     * 
     * @param nextEventTime next differing event time
     */
    public void setNextEventTime(LocalDateTime nextEventTime) {
        this.nextEventTime = nextEventTime;
    }

    /**
     * Set last differing event. A Status should also be set alongside this
     * 
     * @param lastEventTime last differing event time
     */
    public void setLastEventTime(LocalDateTime lastEventTime) {
        this.lastEventTime = lastEventTime;
    }

    /**
     * Set if this.status is always happening in near future
     * 
     * @param isAlways check if always to be set
     */
    public void setAlways(boolean isAlways) {
        this.isAlways = isAlways;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Status: " + status);
        b.append(Utils.LINE_SEPARATOR);
        b.append("Comment: " + comment);
        b.append(Utils.LINE_SEPARATOR);
        b.append("Defining Rule: " + defRule);
        if (nextEventTime != null) {
            b.append(Utils.LINE_SEPARATOR);
            b.append("Time of next event: " + nextEventTime);
            
        }
        if (lastEventTime != null) {
            b.append(Utils.LINE_SEPARATOR);
            b.append("Time of last event: " + lastEventTime);
            
        }
        if (isAlways) {
            b.append("Is always " + status);
        }
        return b.toString();
    }
}
