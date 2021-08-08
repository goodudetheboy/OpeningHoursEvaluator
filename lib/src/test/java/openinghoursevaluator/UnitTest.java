package openinghoursevaluator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.Locale;

import org.junit.Test;

import ch.poole.openinghoursparser.OpeningHoursParseException;
import io.github.goodudetheboy.worldholidaydates.holidaydata.HolidayData;

public class UnitTest {

    /**
     * Test for timepoint, used when main test suite doesn't cover certain cases
     */
    @Test
    public void timepointTest() throws OpeningHoursParseException, OpeningHoursEvaluationException {
        assertTrue(InputTest.evaluateCheck("00:00-02:00,12:00-14:00,17:00-24:00", "2021-06-09T15:00", Status.CLOSED));
        assertTrue(InputTest.evaluateCheck("00:00-02:00,12:00-14:00,17:00-24:00", "2021-06-09T18:00", Status.OPEN));
        assertTrue(InputTest.evaluateCheck("2021 open", "2020-12-31T00:00", Status.CLOSED));
    }

    /**
     * Test for open next, used when main test suite doesn't cover certain cases
     */
    @Test
    public void openNextTest() throws OpeningHoursParseException, OpeningHoursEvaluationException {
        LocalDateTime time = LocalDateTime.parse("2021-07-01T12:00");
        LocalDateTime answerTime = LocalDateTime.parse("2021-07-01T14:00");
        assertEquals(answerTime, InputTest.getNextEvent("14:00-18:00 unknown", time).getNextEventTime());
    }

    /**
     * Test for open last, used when main test suite doesn't cover certain cases
     */
    @Test
    public void openLastTest() throws OpeningHoursParseException, OpeningHoursEvaluationException {
        LocalDateTime time = LocalDateTime.parse("2021-06-30T12:00");
        LocalDateTime answerTime = LocalDateTime.parse("2021-06-29T18:00");
        assertEquals(answerTime, InputTest.getLastEvent("14:00-18:00 unknown", time).getLastEventTime());
    }

    /**
     * Test for variable time test
     */
    @Test
    public void variableTimeTest() throws OpeningHoursParseException, OpeningHoursEvaluationException {
        OpeningHoursEvaluator evaluator = new OpeningHoursEvaluator("sunset-sunrise", false);
        assertEquals(Status.OPEN, evaluator.checkStatus("2021-06-13T05:31"));
        assertEquals(Status.CLOSED, evaluator.checkStatus("2021-06-13T05:32"));
    }

    /**
     * Test for testing variable time of countries with different coordinates
     */
    @Test
    public void diffCountryVarTimeTest() throws OpeningHoursParseException, OpeningHoursEvaluationException {
        double[][] countries = {{ 31.2304   , 121.4737 }, // Shanghai, China
                                { 41.8781   , -87.6298 }, // Chicago, USA
                                { 52.5200   , 10.4515  } // Berlin, Germany
                            }; 
        String[] inputTime = { "2021-07-22T04:50", "2021-07-22T06:00", "2021-07-22T05:20" };
        Status[] status = { Status.CLOSED, Status.OPEN, Status.CLOSED };

        for (int i=0; i<countries.length; i++) {
            OpeningHoursEvaluator evaluator
                = new OpeningHoursEvaluator("sunrise-sunset", false, countries[i][0], countries[i][1], "");
            assertEquals(status[i], evaluator.checkStatus(inputTime[i]));
        }
    }

    /**
     * Test for different Locale-based week number
     */
    @Test
    public void localeBasedWeekTest() throws OpeningHoursParseException, OpeningHoursEvaluationException {
        // US and some countries considers the first days of the year to be week 1
        OpeningHoursEvaluator evaluator = new OpeningHoursEvaluator("week 1", false, 43.1566, -77.6088, "US");
        assertEquals(Status.OPEN, evaluator.checkStatus("2021-01-01T05:31"));
        evaluator.setOpeningHoursTag("week 53");
        assertEquals(Status.CLOSED, evaluator.checkStatus("2021-01-01T05:32"));

        OpeningHoursEvaluator evaluator2 = new OpeningHoursEvaluator("week 1", false, 46.2276, 2.2276, Locale.FRANCE);
        assertEquals(Status.CLOSED, evaluator2.checkStatus("2021-01-01T05:31"));
        evaluator2.setOpeningHoursTag("week 53");
        assertEquals(Status.OPEN, evaluator2.checkStatus("2021-01-01T05:32"));
    }

    /**
     * Constructor test for evaluator
     */
    @Test
    public void constructorTest() throws OpeningHoursParseException {
        OpeningHoursEvaluator evaluator = new OpeningHoursEvaluator("week 1", false, 43.1566, -77.6088, Locale.US);
        assertEquals("US", evaluator.getGeolocation().getCountry());
    }

    /**
     * A weird test that I just put here to check if the code to retrieve week data
     * is working
     */
    @Test
    public void getWeekDataTest() throws OpeningHoursParseException, OpeningHoursEvaluationException {
        OpeningHoursEvaluator evaluator = new OpeningHoursEvaluator("10:00-20:00; 13:00-15:00 off", false);
        LocalDateTime inputTime = LocalDateTime.parse("2021-07-01T00:00");
        String expected = "Mo (2021-06-28) : 10:00-13:00(opening) 15:00-20:00(opening) ";

        // unsquashed
        String actual = evaluator.getWeekData(inputTime).get(0).getStartWeekDayRule().toString();
        assertEquals(expected, actual);

        // squashed week
        actual = evaluator.getSquashedWeekData(inputTime).getStartWeekDayRule().toString();
        assertEquals(expected, actual);
    }

    /**
     * A test for PH tag
     */
    @Test
    public void holidayTest() throws OpeningHoursParseException, OpeningHoursEvaluationException {
        OpeningHoursEvaluator e1 = new OpeningHoursEvaluator("PH open, PH -2 days unknown", false); // Vietnam geolocation
        Result result = e1.evaluate("2021-04-30T00:00");
        // PH open (30/4 and 1/5)
        assertEquals(Status.OPEN, result.getStatus());
        assertEquals("Day of liberating the South for national reunification", result.getComment());
        // PH -2 days unknown
        assertEquals(Status.UNKNOWN, e1.checkStatus("2021-04-28T00:00"));

        // SH test
        OpeningHoursEvaluator e2 = new OpeningHoursEvaluator("SH open, PH -2 days unknown", false);
        // PH open (30/4 and 1/5)
        assertEquals(Status.CLOSED, e2.checkStatus("2021-04-30T00:00"));
        // PH -2 days unknown
        assertEquals(Status.UNKNOWN, e2.checkStatus("2021-04-28T00:00"));

        // other type test
        OpeningHoursEvaluator e3 = new OpeningHoursEvaluator("PH open", false, 58.5953, 25.0136, "EE"); // Estonia
        assertEquals(Status.OPEN, e3.checkStatus("2021-02-24T00:00"));
        assertEquals(Status.CLOSED, e3.checkStatus("2021-02-23T00:00"));
        assertEquals(Status.OPEN, e3.checkStatus("2021-01-06T00:00"));

        OpeningHoursEvaluator e4 = new OpeningHoursEvaluator("PH open", false, 61.9241, 25.7482, "FI"); // Finland
        assertEquals(Status.OPEN, e4.checkStatus("2021-06-25T00:00"));

        OpeningHoursEvaluator e5 = new OpeningHoursEvaluator("12:00-15:00 open; SH open, PH 15:00-16:00 unknown", false, 42.7339, 25.4858, "BG"); // Bulgaria
        Result r5 = e5.evaluate("2021-11-01T00:00");
        assertEquals(Status.OPEN, r5.getStatus());
        assertEquals("Revival Leaders' Day", r5.getComment());
        assertEquals(Status.UNKNOWN, e5.checkStatus("2021-12-24T15:00"));
        assertEquals(Status.OPEN, e5.checkStatus("2021-12-26T13:00"));
        assertEquals(Status.UNKNOWN, e5.checkStatus("2021-12-26T15:00"));

        OpeningHoursEvaluator e6 = new OpeningHoursEvaluator("12:00-01:00; PH 13:00-15:00 open", false);
        assertEquals(Status.OPEN, e6.checkStatus("2021-09-02T00:00"));
        assertEquals(Status.CLOSED, e6.checkStatus("2021-09-02T02:00"));
        assertEquals(Status.OPEN, e6.checkStatus("2021-09-02T14:00"));

        OpeningHoursEvaluator e7 = new OpeningHoursEvaluator("08:00-13:00, PH 13:00-15:00 open", false);
        assertEquals(Status.OPEN, e7.checkStatus("2021-09-02T08:00"));
        assertEquals(Status.OPEN, e7.checkStatus("2021-09-02T14:00"));
    }
    
    @Test
    public void dataInitializationTest() {
        HolidayData holidayData = HolidayData.initializeData();
        assertEquals(168, holidayData.getHolidays().size());
    }
}
