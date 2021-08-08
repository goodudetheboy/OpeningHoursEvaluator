package openinghoursevaluator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

            // check national holiday
            Holiday nationalHoliday = processHolidayHelper(offsetDate, country, holidayRule.getType());
            if (nationalHoliday != null) {
                return nationalHoliday;
            }

            // if nothing is found, check regional holiday
            if (geoloc.getSubRegion() != null) {
                Country subRegion = getSubRegionFromCountry(country, geoloc.getSubRegion());
                if (subRegion != null) {
                    Holiday subRegionHoliday = processHolidayHelper(offsetDate, subRegion, holidayRule.getType());
                    if (subRegionHoliday != null) {
                        return subRegionHoliday;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets a subregion from a {@link Country}.
     * <p> 
     * TODO: use getRegionByName() in the next release of WorldHolidayDates
     * 
     * @param c {@link Country} to get the subregion from
     * @param subRegionName name of the subregion to get
     * @return {@link Country} of the subregion, null if none found
     */
    @Nullable
    private Country getSubRegionFromCountry(Country c, String subRegionName) {
        if (subRegionName != null) {
            List<Map<String, Country>> toCheck = new ArrayList<>();
            toCheck.add(c.getStates());
            toCheck.add(c.getRegions());
            for (Map<String, Country> check : toCheck) {
                if (check != null && check.containsKey(subRegionName)) {
                    return check.get(subRegionName);
                }
            }
        }
        return null;
    }
    /**
     * Checks if {@link offsetDate} is a holiday in the holiday data of a
     * {@link region}, w.r.t. to a {@link holidayRuleType}.
     * 
     * @param offsetDate date to check
     * @param region {@link Country} to check
     * @param holidayRuleType {@link Type} of the holiday to check
     * @return the {@link Holiday} of that date if the date is a holiday, null otherwise
     */
    @Nullable
    private Holiday processHolidayHelper(LocalDate offsetDate, Country region, Type holidayRuleType) {
        int year = offsetDate.getYear();
        List<Holiday> holidays = region.getDays();
        for (Holiday h : holidays) {
            int[] years = { year-1, year, year+1 };
            for (int yearToCheck : years) {
                LocalDate date = h.calculateDate(yearToCheck);
                if (date != null && date.equals(offsetDate)
                    && checkType(h, holidayRuleType)) {
                    return h;
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
