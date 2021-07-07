package openinghoursevaluator;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import ch.poole.openinghoursparser.OpeningHoursParseException;
import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.Rule;

/**
 * Implementation of the OpeningHoursEvaluator, currently act as a placeholder for creating testing
 * file
 * 
 *
 */
public class OpeningHoursEvaluator {
    /** List to store rules from the parser */
    List<Rule> rules;
    String openingHours;
    boolean isStrict = false;

    /** Constructor with input time string according to opening hours specification 
     * @throws OpeningHoursParseException*/
    public OpeningHoursEvaluator(String openingHours, boolean isStrict) throws OpeningHoursParseException {
        this.openingHours = openingHours;
        this.isStrict = isStrict;
        OpeningHoursParser parser = new OpeningHoursParser(new ByteArrayInputStream(openingHours.getBytes()));
        rules = parser.rules(isStrict);
    }

    /**
     * Check if avenue is closed or not according to an input time string in accordance with
     * LocalDateTime parser
     * 
     * @param inputTime a LocalDateTime instance
     * @param isStrict
     * @throws OpeningHoursEvaluationException
     */
    public Result checkStatus(LocalDateTime inputTime) throws OpeningHoursEvaluationException {
        MonthRule monthRule = new MonthRule(rules);
        return monthRule.checkStatus(inputTime);
    }

    /**
     * Print the week schedule created by inputTime using the stored opening hours
     * 
     * @param inputTime LocalDateTime time
     * @return week schedule created by inputTime using the stored opening hours
     */
    public String toString(LocalDateTime inputTime) {
        MonthRule monthRule = new MonthRule(rules);
        try {
            monthRule.buildWeek(inputTime);
        } catch (OpeningHoursEvaluationException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        return monthRule.toWeekString();
    }

    /**
     * Print the week schedule created by inputTime using the stored opening
     * hours, used for debugging purpose. This week includes more details
     * about each time of day
     * 
     * @param inputTime LocalDateTime time
     * @return week schedule created by inputTime using the stored opening hours
     */
    public String toDebugString(LocalDateTime inputTime) {
        MonthRule monthRule = new MonthRule(rules);
        try {
            monthRule.buildWeek(inputTime);
        } catch (OpeningHoursEvaluationException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        return monthRule.toDebugWeekString();
    }
}
