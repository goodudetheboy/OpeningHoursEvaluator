package openinghoursevaluator;

import java.time.LocalDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ch.poole.openinghoursparser.Rule;

public class Result {
    Status  status      = null;
    String  comment     = null;
    Rule    defRule     = null;

    // reserved for when asking for open/close next
    LocalDateTime   nextEventTime   = null;
    LocalDateTime   lastEventTime   = null;
    boolean         isAlways        = false;

    public Result() {
        // empty
    }

    public Result(@Nonnull Status status, @Nullable String comment, @Nullable Rule defRule) {
        setStatus(status);
        setComment(comment);
        setDefiningRule(defRule);
    }

    public Result(TimeRange timerange) {
        this(timerange.getStatus(), timerange.getComment(), timerange.getDefiningRule());
    }

    public Status getStatus() {
        return status;
    }

    public String getComment() {
        return comment;
    }

    public Rule getDefiningRule() {
        return defRule;
    }

    @Nullable
    public LocalDateTime getNextEventTime() {
        return nextEventTime;
    }

    @Nullable
    public LocalDateTime getLastEventTime() {
        return lastEventTime;
    }

    public boolean isAlways() {
        return isAlways;
    }

    public boolean hasComment() {
        return comment != null;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setDefiningRule(Rule defRule) {
        this.defRule = defRule;
    }
 
    public void setNextEventTime(LocalDateTime nextEventTime) {
        this.nextEventTime = nextEventTime;
    }

    public void setLastEventTime(LocalDateTime lastEventTime) {
        this.lastEventTime = lastEventTime;
    }

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
