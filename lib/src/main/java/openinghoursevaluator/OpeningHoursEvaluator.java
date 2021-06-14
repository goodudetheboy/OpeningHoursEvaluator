package openinghoursevaluator;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.ParseException;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.TimeSpan;

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

    /** Constructor with input time string according to opening hours specification */
    public OpeningHoursEvaluator(String openingHours, boolean isStrict) {
        this.openingHours = openingHours;
    }

    /**
     * Check if avenue is closed or not according to an input time string in accordance with
     * LocalDateTime parser
     * 
     * @param inputTime input time string in the form of "yyyy-mm-ddThh:mm"
     * @param isStrict
     */
    public Status checkStatus(String inputTime) {
        LocalDateTime time = LocalDateTime.parse(inputTime);
        Week weekRule = new Week(openingHours, time);
        return weekRule.checkStatus(time);
    }

    /** Print the Week created by inputTime, to be used for debugging wrong test case */
    public void printWithInputTime(String inputTime) {
        LocalDateTime time = LocalDateTime.parse(inputTime);
        Week weekRule = new Week(openingHours, time);
        System.out.println(weekRule);
    }
}
