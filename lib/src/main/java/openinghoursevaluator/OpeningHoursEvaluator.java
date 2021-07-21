package openinghoursevaluator;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import ch.poole.openinghoursparser.OpeningHoursParseException;
import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.Rule;

/**
 * Implementation of the OpeningHoursEvaluator
 *
 */
public class OpeningHoursEvaluator {
    // List to store rules from the parser
    List<Rule>      rules           = null;
    String          openingHours    = null;
    boolean         isStrict        = false;
    TimeTraveller   timeTraveller   = null;

    // geocoding, set default to default geocoder
    Geocoder        geocoder        = new Geocoder();

    /**
     * Constructor with input time string according to opening hours
     * specification and an option to set strict/non-strict parsing
     * 
     * @param openingHours an opening hours tag
     * @param isStrict parsing mode of evaluator, true to turn on strict
     * @throws OpeningHoursParseException
     */
    public OpeningHoursEvaluator(String openingHours, boolean isStrict)
            throws OpeningHoursParseException {
        this.isStrict = isStrict;
        setOpeningHoursTag(openingHours);
    }

    /**
     * Constructor with input time string according to opening hours
     * specification. The parsing mode is default set to false
     * 
     * @param openingHours an opening hours tag
     * @throws OpeningHoursParseException
     */
    public OpeningHoursEvaluator(String openingHours) throws OpeningHoursParseException {
        this(openingHours, false);
    }

    /**
     * Constructor with input time string according to opening hours
     * specification, an option to set strict/non-strict parsing, and a country
     * name to populate geocoding
     * 
     * @param openingHours an opening hours tag
     * @param isStrict parsing mode of evaluator, true to turn on strict
     * @param country
     * @throws OpeningHoursParseException
     */
    public OpeningHoursEvaluator(String openingHours, boolean isStrict, double lat, double lng)
            throws OpeningHoursParseException {
        this(openingHours, isStrict);
        geocoder = new Geocoder(lat, lng);
    }

    /**
     * @return the current Rules stored in this evaluator
     */
    public List<Rule> getRules() {
        return rules;
    }

    /**
     * @return the opening hours tag stored in this evaluator
     */
    public String getOpeningHoursTag() {
        return openingHours;
    }

    /**
     * @return true if this evaluator is in strict parsing
     */
    public boolean isStrictParsing() {
        return isStrict;
    }

    /**
     * @return the geocoder of this evaluator
     */
    public Geocoder getGeocoder() {
        return geocoder;
    }

    /**
     * Set the current opening hours tag of this evaluator. This will also
     * reset the list of Rule stored in this evaluator, and any subsequent
     * use of this evaluator will rely on this Rules created from input opening
     * hours
     * 
     * @param openingHours opening hours tag to be set
     */
    public void setOpeningHoursTag(String openingHours)
            throws OpeningHoursParseException {
        this.openingHours = openingHours;
        OpeningHoursParser parser = new OpeningHoursParser(new ByteArrayInputStream(openingHours.getBytes()));
        setRules(parser.rules(isStrict));
    }

    /**
     * Set the current Rules of this evaluator, and any subsequent use of this
     * evaluator will rely on this opening hours
     * 
     * @param rules
     */
    public void setRules(List<Rule> rules) {
        this.rules = rules;
        timeTraveller = new TimeTraveller(rules);
    }

    /**
     * Set the parsing mode (strict/non-strict) of this evaluator, and any
     * subsequent resetting of opening hours tag will use this input mode
     * 
     * @param isStrict parsing mode to be set
     */
    public void setStrictParsing(boolean isStrict) {
        this.isStrict = isStrict;
    }



    /**
     * Evaluate and return a structured Result based on the input time with the
     * stored opening hours. See {@link Result} for more details on how to work
     * with this
     * 
     * @param inputTime a LocalDateTime instance
     * @return result of the evaluation
     * @throws OpeningHoursEvaluationException
     */
    public Result evaluate(LocalDateTime inputTime)
            throws OpeningHoursEvaluationException {
        MonthRule monthRule = new MonthRule(rules);
        return monthRule.checkStatus(inputTime, geocoder);
    }

    /**
     * Evaluate and return a structured Result based on the input time string
     * with the stored opening hours. The input string must be parsable by
     * LocalDateTime. See {@link Result} for more details on how to work with
     * this.
     * 
     * @param inputTimeString a time string parsable LocalDateTime
     * @return result of the evaluation
     * @throws OpeningHoursEvaluationException
     */
    public Result evaluate(String inputTimeString) 
            throws OpeningHoursEvaluationException {
        return evaluate(LocalDateTime.parse(inputTimeString));
    }

    /**
     * Get the Status of the current opening hours tag at the input time
     * 
     * @param inputTime a LocalDateTime instance
     * @return a Status instance
     * @throws OpeningHoursEvaluationException
     */
    public Status checkStatus(LocalDateTime inputTime)
            throws OpeningHoursEvaluationException {
        return evaluate(inputTime).getStatus();
    }

    /**
     * Get the Status of the current opening hours tag at the input time string.
     * The input time string must be parsable by LocalDateTime.
     * 
     * @param inputTimeString a time string parsable LocalDateTime
     * @return a Status instance
     * @throws OpeningHoursEvaluationException
     */
    public Status checkStatus(String inputTimeString)
            throws OpeningHoursEvaluationException {
        return checkStatus(LocalDateTime.parse(inputTimeString));
    }

    /**
     * Return next differing event of the input time (status different
     * from status of the evaluation of inputTime against the stored rules).
     * 
     * @param inputTime time to be checked
     * @return next differing event of the input time (status different from
     *      status of the evaluation of inputTime against the stored rules)
     */
    public Result getNextEvent(LocalDateTime inputTime)
            throws OpeningHoursEvaluationException {
        return timeTraveller.getDifferingEvent(inputTime, true, geocoder);
    }

    /**
     * Return last differing event of the input time (status different
     * from status of the evaluation of inputTime against the stored rules).
     * 
     * @param inputTime time to be checked
     * @return last differing event of the input time (status different from
     *      status of the evaluation of inputTime against the stored rules)
     */
    public Result getLastEvent(LocalDateTime inputTime)
            throws OpeningHoursEvaluationException {
        return timeTraveller.getDifferingEvent(inputTime, false, geocoder);
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
            monthRule.buildWeek(inputTime, geocoder);
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
            monthRule.buildWeek(inputTime, geocoder);
        } catch (OpeningHoursEvaluationException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        return monthRule.toDebugWeekString();
    }
}
