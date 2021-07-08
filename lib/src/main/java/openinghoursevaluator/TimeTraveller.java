package openinghoursevaluator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import ch.poole.openinghoursparser.Rule;

/**
 * A class used to travel into time to get next differing event, used to find
 * open next
 * 
 */
public class TimeTraveller {
    // a limit to lookahead into the future when checking open next
    public static final int MAX_FUTURE_WEEKS = 100;

    List<Rule>  rules   = null;

    public TimeTraveller() {
        //empty
    }

    public TimeTraveller(List<Rule> rules) {
        this.rules = rules;
    }

    /**
     * Return next differing event of the input time (status different
     * from status of the evaluation of inputTime against the stored rules)
     * 
     * @param inputTime time to be checked
     * @return next differing event of the input time (status different from
     *      status of the evaluation of inputTime against the stored rules)
     * @throws OpeningHoursEvaluationException
     */
    public Result getNextDifferingEvent(LocalDateTime inputTime)
            throws OpeningHoursEvaluationException {
        MonthRule monthRule = new MonthRule(rules);

        // checking in current week in monthRule first
        monthRule.buildWeek(inputTime);
        Status statusToCheck = monthRule.checkStatus(inputTime).getStatus();
        Result result = monthRule.getNextDifferingEvent(inputTime, statusToCheck);

        // if nothing could be found, go to the future!
        if (result != null) {
            return result;
        } else {
            LocalDate toTheFuture = inputTime.toLocalDate();
            for (int i=0; i < MAX_FUTURE_WEEKS; i++) {
                toTheFuture = DateManager.getOffsetDate(toTheFuture, 7);
                monthRule.buildWeek(toTheFuture.atStartOfDay());
                result = monthRule.getNextDifferingEvent(statusToCheck);
                if (result != null) {
                    return result;
                }
            }
        }
        Result always = new Result(statusToCheck, null, null);
        always.setAlways(true);
        return always;
    }
}
