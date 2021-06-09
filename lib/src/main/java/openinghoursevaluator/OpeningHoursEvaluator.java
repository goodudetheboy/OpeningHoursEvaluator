package openinghoursevaluator;

import java.io.ByteArrayInputStream;
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

    /** Constructor with input time string according to opening hours specification */
    public OpeningHoursEvaluator(String openingHours, boolean isStrict) {
        
        OpeningHoursParser parser = new OpeningHoursParser(new ByteArrayInputStream(openingHours.getBytes()));
        try {
            rules = parser.rules(isStrict);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }
    }

    public boolean checkStatus(String inputTime, boolean isStrict) {
        OpeningHoursParser inputParser = new OpeningHoursParser(new ByteArrayInputStream(inputTime.getBytes()));
        List<Rule> inputRules;
        try {
            inputRules = inputParser.rules(isStrict);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
        
        for(Rule inputRule : inputRules) {
            if(inputRule.getTimes() != null) {
                List<TimeSpan> times = inputRule.getTimes();
                for(TimeSpan time : times) {
                    if(time.getEnd() == Integer.MIN_VALUE) {
                        if(!checkStatusWithTimePoint(time.getStart())) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public boolean checkStatusWithTimePoint(int timepoint) {
        for(Rule rule : rules) {
            if(rule.getTimes() != null) {
                List<TimeSpan> times = rule.getTimes();
                for(TimeSpan time : times) {
                    if(time.getEnd() == Integer.MIN_VALUE) {
                        if(timepoint == time.getStart()) {
                            return true;
                        }
                    } else if(timepoint >= time.getStart() && timepoint <= time.getEnd()) {
                            return true;
                    }
                }
            }
        }
        // Return false if no TimeSpan rules contains timepoint
        return false;
    }
}
