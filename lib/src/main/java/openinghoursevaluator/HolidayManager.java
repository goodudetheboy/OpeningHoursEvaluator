package openinghoursevaluator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.json.JSONException;
import org.json.JSONObject;

import ch.poole.openinghoursparser.Holiday;

/**
 * Helper class to process Holiday, and also parse holiday JSON data
 */
public class HolidayManager {
    Geolocation     geoloc          = null;
    String          holidayName  = null;    
    
    /**
     * Constructor to initialize the holiday manager with a Geolocation
     * 
     * @param geoloc input Geolocation
     */
    public HolidayManager(Geolocation geoloc) {
        this.geoloc = geoloc;
    }

    /**
     * @return a String containing the holiday comment
     */
    public String getHolidayName() {
        return holidayName;
    }

    /**
     * Set the holiday comment
     * 
     * @param holidayName a String containing the holiday comment
     */
    private void setHolidayName(String holidayName) {
        this.holidayName = holidayName;
    }

    public boolean processHoliday(LocalDate defDate, Holiday holiday) throws OpeningHoursEvaluationException {
        // check for data availability
        String country = geoloc.getCountry();
        String holidayDataStr;
        try {
            String path = System.getProperty("user.dir") + "/src/main/resources/holiday-data/" + country + ".json";
            holidayDataStr = Utils.readFile(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // TODO: consider returning false instead of returning exception
            throw new OpeningHoursEvaluationException("Some problem has occured with reading holiday data");
        }

        // retrieve holiday data
        JSONObject data = new JSONObject(holidayDataStr);
        JSONObject holidayData = data.getJSONObject("holidays")
                                     .getJSONObject(country)
                                     .getJSONObject("days");
        String[] holidayDates = JSONObject.getNames(holidayData);

        // apply offset and retrieve defining month and day
        LocalDate offsetDate = DateManager.getOffsetDate(defDate, holiday.getOffset() * -1);
        int defMonth = offsetDate.getMonthValue();
        int defDay = offsetDate.getDayOfMonth();

        // TODO: check for PH and SH

        // check holiday data for defining month and day
        for (String dateString : holidayDates) {
            int holidayMonth;
            int holidayDay;
            try {
                holidayMonth = Integer.parseInt(dateString.substring(0, 2));
                holidayDay = Integer.parseInt(dateString.substring(3, 5));
                if (holidayMonth == defMonth && holidayDay == defDay) {
                    JSONObject date = holidayData.getJSONObject(dateString);
                    setHolidayName(getCommentFromJSON(date));
                    return true;
                }
            } catch (NumberFormatException e) {
                // ignore
            }
        }                
        return false;
    }

    /**
     * Retrieve name of the holiday from the holiday date JSON object, if there
     * is a "name" key in the JSON object, then the holiday name of the "en"
     * key of the "name" JSON Object is returned, else null is returned.
     * 
     * @param holidayDate a holiday date JSON object
     * @return a string with the name of the holiday, "en" only if has "name"
     *      key
     */
    private String getCommentFromJSON(JSONObject holidayDate) {
        String result = null;
        try {
            JSONObject name = holidayDate.getJSONObject("name");
            result = name.getString("en");
        } catch (JSONException e) {
            // ignore
        }
        return result;
    }
}
