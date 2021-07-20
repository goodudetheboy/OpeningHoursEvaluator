package openinghoursevaluator;

import java.time.ZoneId;
import java.util.Optional;

import javax.annotation.Nullable;

import net.iakovlev.timeshape.TimeZoneEngine;

/**
 * A Geocoder class to provide geolocation information for use through the
 * evaluator, for example, for getting location-dependent variable time
 */
public class Geocoder {
    // TimeZoneEngine takes time to init, so it will only be done once
    public static final TimeZoneEngine engine = TimeZoneEngine.initialize();
    // Geocoding of Ho Chi Minh City, Vietnam, taken from Google
    public static final double DEFAULT_LATITUDE     = 10.8231;
    public static final double DEFAULT_LONGITUDE    = 106.6297;

    double  lat         = DEFAULT_LATITUDE;
    double  lng         = DEFAULT_LONGITUDE;
    ZoneId  timezone    = ZoneId.of("Asia/Ho_Chi_Minh");

    /**
     * Constructor for a default geocoder, with geolocation set to Ho Chi
     * Minh City
     */
    public Geocoder() {
        // empty
    }

    /**
     * Constructor for a geocoder, with input latitude and longitude
     * 
     * @param lat latitude
     * @param lng longitude
     */
    public Geocoder(double lat, double lng) {
        setLatitude(lat);
        setLongitude(lng);
        refreshTimeZone();
    }

    /**
     * @return the latitude of this geocoder
     */
    public double getLatitude() {
        return lat;
    }

    /**
     * @return the longitude of this geocoder
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
     * @return the ZoneId created from the stored coordinates in this geocoder
     */
    public ZoneId getTimeZone() {
        return timezone;
    }

    /**
     * Set the latitude of this geocoder, which will influence calculation of
     * events of day like dawn, dusk, sunrise, sunset in the evaluator
     *  
     * @param lat double value of a latitude
     */
    public void setLatitude(double lat) {
        this.lat = lat;
    }

    /**
     * Set the longitude of this geocoder, which will influence calculation of
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
     * Set the coordinates of this geocoder, which will influence calculation of
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
     * Refresh the timezone of this geocoder with stored coordinates
     */
    public void refreshTimeZone() {
        timezone = getTimeZoneFromCoor(lat, lng);
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
    public static ZoneId getTimeZoneFromCoor(double lat, double lng) {
        Optional<ZoneId> query = engine.query(lat, lng);
        return (query.isPresent()) ? query.get() : null;
    }
}
