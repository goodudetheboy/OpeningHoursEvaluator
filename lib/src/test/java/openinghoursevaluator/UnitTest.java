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
        assertTrue(InputTest.evaluateCheck("sunset-sunrise", "2021-06-13T05:31", Status.OPEN));
        assertTrue(InputTest.evaluateCheck("sunset-sunrise", "2021-06-13T05:32", Status.CLOSED));
    }
}
