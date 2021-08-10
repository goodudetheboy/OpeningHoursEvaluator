package openinghoursevaluator;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

import ch.poole.openinghoursparser.Rule;

/**
 * A structured result class for the OpeningHoursEvaluator. This is also used for
 * storing getting next event (open/close next).
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
     * A list of warnings, including overriding other rules.
     */
    List<String>    warnings        = null;

    /**
     * A list of Rule which is overridden in the day where the Result comes from.
     */
    List<Rule>      overriddenRules = null;

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
        setWarnings(new ArrayList<>());
    }

    /**
     * Constructor for a {@link Result} with a Status.
     * 
     * @param status a {@link Status}
     */
    public Result(@Nonnull Status status) {
        this(status, null, null);
    }
    
    /**
     * Constructor for a Result with a TimeRange
     * 
     * @param timerange a {@link TimeRange}
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
     * @return the list of warnings
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * @return the list of overridden rules
     */
    public List<Rule> getOverriddenRules() {
        return overriddenRules;
    }

    /**
     * Sets the status of this Result. 
     * 
     * @param status status to be set
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Sets the comment of this Result
     * 
     * @param comment comment to be set
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Sets the defining Rule of this Result
     * 
     * @param defRule defining Rule to be set
     */
    public void setDefiningRule(Rule defRule) {
        this.defRule = defRule;
    }
 
    /**
     * Sets next differing event. A Status should also be set alongside this
     * 
     * @param nextEventTime next differing event time
     */
    public void setNextEventTime(LocalDateTime nextEventTime) {
        this.nextEventTime = nextEventTime;
    }

    /**
     * Sets last differing event. A Status should also be set alongside this
     * 
     * @param lastEventTime last differing event time
     */
    public void setLastEventTime(LocalDateTime lastEventTime) {
        this.lastEventTime = lastEventTime;
    }

    /**
     * Sets if this.status is always happening in near future
     * 
     * @param isAlways check if always to be set
     */
    public void setAlways(boolean isAlways) {
        this.isAlways = isAlways;
    }

    /**
     * Sets the list of warnings
     * 
     * @param warnings the list of warnings
     */
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    /**
     * Sets the list of overridden rules
     * 
     * @param overriddenRules the list of overridden rules
     */
    public void setOverriddenRules(List<Rule> overriddenRules) {
        this.overriddenRules = new ArrayList<>(overriddenRules);
    }

    /**
     * Checks is there's any overriden rules in this {@link WeekDayRule} during
     * building. If yes, then build according warning String.
     * 
     * @return warning string, if there are any overriden rules
     */
    @Nullable
    String generateOverridenWarning(LocalDate defDate) {
        if (overriddenRules != null && !overriddenRules.isEmpty()) {
            StringBuilder b = new StringBuilder();
            b.append("Rules overridden on " + defDate.toString() + ": ");
            
            for (Rule rule : overriddenRules) {
                b.append(rule.toString() + "; ");
            }
            return b.toString();
        }
        return null;
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
