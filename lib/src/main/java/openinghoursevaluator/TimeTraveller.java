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
    // a limit to lookahead into the future/past when checking open next
    public static final int MAX_FUTURE_WEEKS = 100;
    public static final int MAX_PAST_WEEKS = 100;

    List<Rule>  rules       = null;
    Geolocation    geocoder    = null;

    public TimeTraveller() {
        //empty
    }

    public TimeTraveller(List<Rule> rules, Geolocation geocoder) {
        this.rules = rules;
        this.geocoder = geocoder;
    }

    /**
     * Return next differing event of the input time (status different
     * from status of the evaluation of inputTime against the stored rules)
     * 
     * @param inputTime time to be checked
     * @param isNext true to look next differing event, false to look last
     * @return next differing event of the input time (status different from
     *      status of the evaluation of inputTime against the stored rules)
     * @throws OpeningHoursEvaluationException
     */
    public Result getDifferingEvent(LocalDateTime inputTime, boolean isNext)
            throws OpeningHoursEvaluationException {
        MonthRule monthRule = new MonthRule(rules, geocoder);

        // checking in current week in monthRule first
        monthRule.buildWeek(inputTime);
        Status statusToCheck = monthRule.checkStatus(inputTime).getStatus();
        Result result = monthRule.getDifferingEvent(inputTime, statusToCheck, isNext);

        // if nothing could be found, go to the future!
        if (result != null) {
            return result;
        } else {
            LocalDate lookahead = inputTime.toLocalDate();
            for (int i=0; i < MAX_FUTURE_WEEKS; i++) {
                lookahead = DateManager.getOffsetDate(lookahead, (isNext) ? 7 : -7);
                monthRule.buildWeek(lookahead.atStartOfDay());
                result = monthRule.getDifferingEvent(statusToCheck, isNext);
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
