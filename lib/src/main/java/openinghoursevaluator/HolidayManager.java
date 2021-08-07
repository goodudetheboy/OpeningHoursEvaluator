package openinghoursevaluator;

import java.time.LocalDate;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ch.poole.openinghoursparser.Holiday.Type;
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
     * @param holidayRule name of the holiday to check
     * @return the {@link Holiday} of that date if the date is a holiday, null otherwise
     */
    @Nullable
    public Holiday processHoliday(LocalDate defDate, ch.poole.openinghoursparser.Holiday holidayRule) {
        Country country = holidayData.getCountry(geoloc.getCountry());
        if (country != null) {
            // apply offset and retrieve defining month and day
            LocalDate offsetDate = DateManager.getOffsetDate(defDate, holidayRule.getOffset() * -1);
            int year = offsetDate.getYear();

            List<Holiday> holidays = country.getDays();
            for (Holiday h : holidays) {
                int[] years = { year-1, year, year+1 };
                for (int yearToCheck : years) {
                    if (h.calculateDate(yearToCheck).equals(offsetDate)
                     && checkType(h, holidayRule.getType())) {
                        return h;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks the type of holiday rule and type of found holiday. Returns true if:
     * <ol>
     * <li> holiday rule is PH and found holiday is of type public, bank,
     *      optional, observance, or unknown </li>
     * <li> holiday rule is SH and found holiday is of type school </li>
     * </ol> 
     * 
     * @param holiday found holiday
     * @param ruleType type of holiday rule, PH or SH
     * @return true if the holiday rule and found holiday match
     */
    private boolean checkType(Holiday holiday, @Nonnull Type ruleType) {
        String hType = holiday.getType();
        if (hType == null) {
            return ruleType.equals(Type.PH);
        }
        switch (ruleType) {
            case PH:
                return hType.equals("public")
                    || hType.equals("bank")
                    || hType.equals("optional")
                    || hType.equals("observance");
            case SH:
                return hType.equals("school");
            default:
                return false;
        }
    }
}
