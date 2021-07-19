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
    // Geocoding of Ho Chi Minh City, Vietnam, taken from Google
    public static final double DEFAULT_LATITUDE     = 10.8231;
    public static final double DEFAULT_LONGITUDE    = 106.6297;

    // List to store rules from the parser
    List<Rule>      rules           = null;
    String          openingHours    = null;
    boolean         isStrict        = false;
    TimeTraveller   timeTraveller   = null;

    // geocoding
    double          latitude = DEFAULT_LATITUDE;
    double          longitude = DEFAULT_LONGITUDE;

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
     */
    private OpeningHoursEvaluator(String openingHours, boolean isStrict, String country) {
        // TODO: create a lookup table for country's geocode and populate self
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
     * @return the latitude of this evaluator
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * @return the longitude of this evaluator
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * @return the geocoding (in form of{latitude, longitude}) of this evaluator
     */
    public double[] getGeocode() {
        return new double[]{ latitude, longitude };
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
     * Set the latitude of this evaluator, which will influence calculation of
     * events of day like dawn, dusk, sunrise, sunset
     *  
     * @param latitude double value of a latitude
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Set the longitude of this evaluator, which will influence calculation of
     * events of day like dawn, dusk, sunrise, sunset
     *  
     * @param latitude double value of a longitude
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * Set the geocoding of this evalutor, which will influence calculation of
     * events of day like dawn, dusk, sunrise, sunset
     * 
     * @param latitude double value of a longitude
     * @param longitude double value of a latitude
     */
    public void setGeocode(double latitude, double longitude) {
        setLatitude(latitude);
        setLongitude(longitude);
    }

    /**
     * Check if avenue is closed or not according to an input time string in accordance with
     * LocalDateTime parser
     * 
     * @param inputTime a LocalDateTime instance
     * @param isStrict
     * @throws OpeningHoursEvaluationException
     */
    public Result checkStatus(LocalDateTime inputTime)
            throws OpeningHoursEvaluationException {
        MonthRule monthRule = new MonthRule(rules);
        return monthRule.checkStatus(inputTime, getGeocode());
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
        return timeTraveller.getDifferingEvent(inputTime, true, getGeocode());
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
        return timeTraveller.getDifferingEvent(inputTime, false, getGeocode());
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
            monthRule.buildWeek(inputTime, getGeocode());
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
            monthRule.buildWeek(inputTime, getGeocode());
        } catch (OpeningHoursEvaluationException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        return monthRule.toDebugWeekString();
    }
}
