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
        // to be expanded later for checking weekday, day, month, year
        if(!checkStatusWithTimePoint(timeInMinute(time))) {
            return false;
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

    public int timeInMinute(LocalDateTime time) {
        return time.getHour()*60 + time.getMinute();
    }
}
