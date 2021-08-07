package openinghoursevaluator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import io.github.goodudetheboy.worldholidaydates.holidaydata.Country;
import io.github.goodudetheboy.worldholidaydates.holidaydata.Holiday;
import io.github.goodudetheboy.worldholidaydates.holidaydata.HolidayData;

/**
 * Helper class to process Holiday, and also parse holiday JSON data
 */
public class HolidayManager {
    public static final HolidayData holidayData = HolidayData.initializeData();
    public static final String DEFAULT_HOLIDAY_COMMENT = "Unnamed holiday";

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
     * Checks if {@link defDate} is a holiday in the {@link holidayData}.
     * 
     * @param defDate date to check
     * @param holiday name of the holiday to check
     * @return the {@link Holiday} of that date if the date is a holiday, null otherwise
     */
    @Nullable
    public Holiday processHoliday(LocalDate defDate, ch.poole.openinghoursparser.Holiday holiday) {
        Country country = holidayData.getCountry(geoloc.getCountry());
        if (country != null) {
            // apply offset and retrieve defining month and day
            LocalDate offsetDate = DateManager.getOffsetDate(defDate, holiday.getOffset() * -1);
            int year = offsetDate.getYear();

            List<Holiday> holidays = country.getDays();
            for (Holiday h : holidays) {
                int[] years = { year-1, year, year+1 };
                for (int yearToCheck : years) {
                    if (h.calculateDate(yearToCheck).equals(offsetDate)) {
                        return h;
                    }
                }
            }
        }
        return null;
    }
}
