package openinghoursevaluator;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.ParseException;
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

    /** Constructor with input time string according to opening hours specification */
    public OpeningHoursEvaluator(String openingHours, boolean isStrict) {
        this.openingHours = openingHours;
        this.isStrict = isStrict;
        OpeningHoursParser parser = new OpeningHoursParser(new ByteArrayInputStream(openingHours.getBytes()));
        try {
            rules = parser.rules(isStrict);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Illegal opening hours input");
        }
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
        LocalDateTime time = LocalDateTime.parse(inputTime);
        MonthRule monthRule = new MonthRule(rules);
        monthRule.build(time);
        return monthRule.checkStatus(time);
    }

    /** Print the Week created by inputTime, to be used for debugging wrong test case */
    public String toString(String inputTime) {
        LocalDateTime time = LocalDateTime.parse(inputTime);
        MonthRule monthRule = new MonthRule(rules);
        try {
            monthRule.build(time);
        } catch (OpeningHoursEvaluationException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        return monthRule.toWeekString();
    }
}
