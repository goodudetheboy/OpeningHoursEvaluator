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
     * @param inputTime input time string in the form of "yyyy-mm-ddThh:mm"
     * @param isStrict
     * @throws OpeningHoursEvaluationException
     */
    public Result checkStatus(String inputTime) throws OpeningHoursEvaluationException {
        return checkStatus(LocalDateTime.parse(inputTime));
        
    }

    public Result checkStatus(LocalDateTime inputTime) throws OpeningHoursEvaluationException {
        MonthRule monthRule = new MonthRule(rules);
        return monthRule.checkStatus(inputTime);
    }

    /** Print the Week created by inputTime, to be used for debugging wrong test case */
    public String toString(String inputTime) {
        LocalDateTime time = LocalDateTime.parse(inputTime);
        MonthRule monthRule = new MonthRule(rules);
        try {
            monthRule.buildWeek(time);
        } catch (OpeningHoursEvaluationException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        return monthRule.toWeekString();
    }

    public String toDebugString(String inputTime) {
        LocalDateTime time = LocalDateTime.parse(inputTime);
        MonthRule monthRule = new MonthRule(rules);
        try {
            monthRule.buildWeek(time);
        } catch (OpeningHoursEvaluationException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        return monthRule.toDebugWeekString();
    }
}
