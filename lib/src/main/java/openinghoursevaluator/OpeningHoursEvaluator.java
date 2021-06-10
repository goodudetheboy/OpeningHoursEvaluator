package openinghoursevaluator;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
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

    /**
     * Check if avenue is closed or not according to an input time string in accordance with
     * LocalDateTime parser
     * 
     * @param inputTime input time string in the form of "yyyy-mm-ddThh:mm"
     * @param isStrict
     */
    public boolean checkStatus(String inputTime) {
        LocalDateTime time = LocalDateTime.parse(inputTime);
        Rule ruleGood = null;
        // to be expanded later for checking weekday, day, month, year
        for(Rule rule : rules) {
            if(ruleGood != null && checkSameDay(ruleGood, rule, time)) ruleGood = null;
            if(ruleGood == null && checkStatusWithTimePoint(timeInMinute(time), rule)) ruleGood = rule;
        }
        return ruleGood != null;
    }

    /**
     * Check if avenue is closed or not according to an input time string in accordance with
     * LocalDateTime parser with respect to a Rule and a timepoint
     * 
     * @param inputpoint input timepoint in minutes form
     * @param rule input rule
     */
    public boolean checkStatusWithTimePoint(int timepoint, Rule rule) {
        if(rule.getTimes() != null) {
            List<TimeSpan> times = rule.getTimes();
            for(TimeSpan time : times) {
                if(time.getEnd() == Integer.MIN_VALUE && timepoint == time.getStart()) return true;
                else if(timepoint >= time.getStart() && timepoint <= time.getEnd()) return true;
            }
        } else {
            return true;
        }
        // Return false if no timespan in input Rule contains timepoint
        return false;
    }

    /**
     * Check if two input rules are affecting the same day
     * 
     * @param rule1 rule 1
     * @param rule2 rule 2
     */
    public boolean checkSameDay(Rule rule1, Rule rule2, LocalDateTime time) {
        return rule1.getDays() == null && rule2.getDays() == null;
    }

    /** Convert hour and minute of a LocalDateTime instance to minutes 
     *  
     * @param time a LocalDateTime instance
    */
    public int timeInMinute(LocalDateTime time) {
        return time.getHour()*60 + time.getMinute();
    }
}
