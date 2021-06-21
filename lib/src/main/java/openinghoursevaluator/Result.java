package openinghoursevaluator;

public class Result {
    Status  status      = null;
    String  comment     = null;

    public Result(Status status, String comment) {
        this.status = status;
        this.comment = comment;
    }

    public Result(TimeRange timerange) {
        this.status = timerange.getStatus();
        this.comment = (timerange.hasComment()) ? timerange.getComment() : null;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Status getStatus() {
        return status;
    }

    public String getComment() {
        return comment;
    }

    public boolean hasComment() {
        return comment != null;
    }
}
