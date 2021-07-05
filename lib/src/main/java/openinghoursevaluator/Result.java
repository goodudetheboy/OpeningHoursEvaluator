package openinghoursevaluator;

import javax.annotation.Nullable;

import ch.poole.openinghoursparser.Rule;

public class Result {
    Status  status      = null;
    String  comment     = null;
    Rule    defRule     = null;

    public Result(Status status, @Nullable String comment, @Nullable Rule defRule) {
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
}
