package openinghoursevaluator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.junit.Test;

import ch.poole.openinghoursparser.OpeningHoursParseException;

public class UnitTest {

    @Test
    public void timepointTest() throws OpeningHoursParseException, OpeningHoursEvaluationException {
        assertTrue(InputTest.evaluateCheck("00:00-02:00,12:00-14:00,17:00-24:00", "2021-06-09T15:00", Status.CLOSED));
        assertTrue(InputTest.evaluateCheck("00:00-02:00,12:00-14:00,17:00-24:00", "2021-06-09T18:00", Status.OPEN));
        assertTrue(InputTest.evaluateCheck("2021 open", "2020-12-31T00:00", Status.CLOSED));
    }

    @Test
    public void openNextTest() throws OpeningHoursParseException, OpeningHoursEvaluationException {
        LocalDateTime time = LocalDateTime.parse("2021-07-01T12:00");
        LocalDateTime answerTime = LocalDateTime.parse("2021-07-01T14:00");
        assertEquals(answerTime, InputTest.getNextEvent("14:00-18:00 unknown", time).getNextEventTime());
    }

    @Test
    public void openLastTest() throws OpeningHoursParseException, OpeningHoursEvaluationException {
        LocalDateTime time = LocalDateTime.parse("2021-06-30T12:00");
        LocalDateTime answerTime = LocalDateTime.parse("2021-06-29T18:00");
        assertEquals(answerTime, InputTest.getLastEvent("14:00-18:00 unknown", time).getLastEventTime());
    }

    @Test
    public void variableTimeTest() throws OpeningHoursParseException, OpeningHoursEvaluationException {
        OpeningHoursEvaluator evaluator = new OpeningHoursEvaluator("sunset-sunrise", false);
        assertEquals(Status.OPEN, evaluator.checkStatus("2021-06-13T05:31"));
        assertEquals(Status.CLOSED, evaluator.checkStatus("2021-06-13T05:32"));
    }

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
}
