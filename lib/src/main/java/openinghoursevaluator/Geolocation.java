package openinghoursevaluator;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import net.iakovlev.timeshape.TimeZoneEngine;

/**
 * A Geolocation class to provide geolocation information for use through the
 * evaluator, for example, for getting location-dependent variable time
 */
public class Geolocation {
    // TimeZoneEngine takes time to init, so it will only be done once
    public static final TimeZoneEngine engine = TimeZoneEngine.initialize();
    // Load the ISO 3166-1 alpha-3 to alpha-2 country code map
    private static Map<String, Locale> localeMap = initISOConversionMap();

    // Geocoding of Ho Chi Minh City, Vietnam, taken from Google
    public static final double DEFAULT_LATITUDE     = 10.8231;
    public static final double DEFAULT_LONGITUDE    = 106.6297;


    double  lat         = DEFAULT_LATITUDE;
    double  lng         = DEFAULT_LONGITUDE;
    ZoneId  timezone    = ZoneId.of("Asia/Ho_Chi_Minh");
    String  country     = "VN";
    Locale  locale      = new Locale.Builder().setRegion("VN").build();

    /**
     * Constructor for a default geolocation, with geolocation set to Ho Chi
     * Minh City
     */
    public Geolocation() {
        // empty
    }

    /**
     * Constructor for a geolocation, with input latitude and longitude
     * 
     * @param lat latitude
     * @param lng longitude
     * @param country ISO 3166 2-letter country code (e.g. "VN")
     */
    public Geolocation(double lat, double lng, String country) {
        setLatitude(lat);
        setLongitude(lng);
        setCountry(country);
        refreshTimeZone();
    }

    /**
     * Constructor for a geolocation, with input latitude, longitude, and a locale
     * 
     * @param lat latitude
     * @param lng longitude
     * @param locale a Locale
     */
    public Geolocation(double lat, double lng, Locale locale) {
        setLatitude(lat);
        setLongitude(lng);
        setLocale(locale);
        refreshTimeZone();
    }

    /**
     * @return the latitude of this geolocation
     */
    public double getLatitude() {
        return lat;
    }

    /**
     * @return the longitude of this geolocation
     */
    public double getLongitude() {
        return lng;
    }
    
    /**
     * @return the geocoding (in form of{latitude, longitude}) of this evaluator
     */
    public double[] getCoordinates() {
        return new double[]{ lat, lng };
    }

    /**
     * @return the ZoneId created from the stored coordinates in this geolocation
     */
    public ZoneId getTimeZone() {
        return timezone;
    }

    /**
     * @return the country of this geolocation
     */
    public String getCountry() {
        return country;
    }

    /**
     * Get the locale of this geolocation
     * 
     * @return the locale of this geolocation
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Set the latitude of this geolocation, which will influence calculation of
     * events of day like dawn, dusk, sunrise, sunset in the evaluator
     *  
     * @param lat double value of a latitude
     */
    public void setLatitude(double lat) {
        this.lat = lat;
    }

    /**
     * Set the longitude of this geolocation, which will influence calculation of
     * events of day like dawn, dusk, sunrise, sunset in the evaluator.
     * <p>
     * Make sure to refresh timezone after setting with
     * {@link #refreshTimeZone() refreshTimeZone}
     *  
     * @param lat double value of a longitude
     */
    public void setLongitude(double lng) {
        this.lng = lng;
    }
    
    /**
     * Set the coordinates of this geolocation, which will influence calculation of
     * events of day like dawn, dusk, sunrise, sunset
     * <p>
     * Make sure to refresh timezone after setting with
     * {@link #refreshTimeZone() refreshTimeZone}
     * 
     * @param lat double value of a longitude
     * @param lng double value of a latitude
     */
    public void setCoordinate(double lat, double lng) {
        setLatitude(lat);
        setLongitude(lng);
    }

    /**
     * Set the country code of this geolocation, which will influence calculation of
     * holiday events in the evaluator. This also resets the Locale of this geolocation.
     * 
     * @param country a 2-character ISO country code
     */
    public void setCountry(String country) {
        this.country = country;
        this.locale = new Locale.Builder().setRegion(country).build();
    }

    /**
     * Set the locale of this geolocation, which will influence calculation of
     * week of year, since some countries consider different week number of year.
     * This will also reset the country code stored in this geolocation
     * 
     * @param locale a Locale to be set
     * @see https://en.wikipedia.org/wiki/List_of_ISO_3166_country_codes, alpha-2 codes
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
        this.country = iso3CountryCodeToIso2CountryCode(locale.getISO3Country());
    }

    /**
     * Refresh the timezone of this geolocation with stored coordinates
     */
    public void refreshTimeZone() {
        timezone = getTimeZoneFromCoordinates(lat, lng);
    }

    /**
     * Retrieve time zone based on coordinates. A Wrapper class for the query()
     * of TimeZoneEngine
     * 
     * @param lat latitude
     * @param lng longitude
     * @return timezone from input coordinates, null if none found
     */
    @Nullable
    public static ZoneId getTimeZoneFromCoordinates(double lat, double lng) {
        Optional<ZoneId> query = engine.query(lat, lng);
        return (query.isPresent()) ? query.get() : null;
    }
     
    /**
     * Initialize mapping to convert from ISO 3166-1 alpha-2 country code to
     * ISO 3166-1 alpha-3 country code
     * <p>
     * This function is adapted from: https://blog.oio.de/2010/12/31/mapping-iso2-and-iso3-country-codes-with-java/
     * 
     * @return a map from ISO 3166-1 alpha-2 country code to ISO 3166-1 alpha-3 country code
     * @author Sönke Sothmann
     */
    private static Map<String,Locale> initISOConversionMap() {
        String[] countries = Locale.getISOCountries();
        Map<String, Locale> localeMap = new HashMap<>(countries.length);
        for (String country : countries) {
            Locale locale = new Locale("", country);
            localeMap.put(locale.getISO3Country().toUpperCase(), locale);
        }
        return localeMap;
    }

    /**
     * Convert ISO 3166-1 alpha-3 country code to ISO 3166-1 alpha-2 country code
     * <p>
     * This function is adapted from: https://blog.oio.de/2010/12/31/mapping-iso2-and-iso3-country-codes-with-java/
     * 
     * @param iso3CountryCode a ISO 3166-1 alpha-3 country code
     * @return a ISO 3166-1 alpha-2 country code
     * @author Sönke Sothmann
     */
    private static String iso3CountryCodeToIso2CountryCode(String iso3CountryCode) {
        return localeMap.get(iso3CountryCode).getCountry();
    }
}
