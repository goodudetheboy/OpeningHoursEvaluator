package openinghoursevaluator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.junit.Test;

import ch.poole.openinghoursparser.OpeningHoursParseException;

public class InputTest {
    @Test
    public void evaluatorTestAll() throws OpeningHoursParseException, OpeningHoursEvaluationException {
        evaluatorTimepointTest();
        evaluatorWeekdayTest();
        evaluatorWeekTest();
        evaluatorMonthTest();
        evaluatorYearTest();
        evaluatorFailTest();
        evaluatorOpenNextTest();
        evaluatorOpenLastTest();
    }

    /**
     * Test only for the timespan test (hours and minutes ONLY)
     */
    @Test
    public void evaluatorTimepointTest() {
        evaluateBatchCheck("test-data/oh/timepoint.txt-oh","test-data/input-time/timepoint.txt", "test-data/answer/timepoint.txt-answer");
    }

    @Test
    public void evaluatorWeekdayTest() {
        evaluateBatchCheck("test-data/oh/weekday.txt-oh", "test-data/input-time/weekday.txt", "test-data/answer/weekday.txt-answer");
    }

    @Test
    public void evaluatorWeekTest() {
        evaluateBatchCheck("test-data/oh/week.txt-oh", "test-data/input-time/week.txt", "test-data/answer/week.txt-answer");
    }

    @Test
    public void evaluatorMonthTest() {
        evaluateBatchCheck("test-data/oh/month.txt-oh", "test-data/input-time/month.txt", "test-data/answer/month.txt-answer");
    }

    @Test
    public void evaluatorYearTest() {
        evaluateBatchCheck("test-data/oh/year.txt-oh", "test-data/input-time/year.txt", "test-data/answer/year.txt-answer");
    }

    @Test
    public void evaluatorFailTest() throws OpeningHoursParseException {
        evaluateFailBatchCheck("test-data/oh/fail.txt-oh", "test-data/answer/fail.txt-answer");
    }

    @Test
    public void evaluatorOpenNextTest() throws OpeningHoursParseException, OpeningHoursEvaluationException {
        evaluateDifferingEventBatchCheck("test-data/oh/open-next.txt-oh", "test-data/input-time/open-next.txt", "test-data/answer/open-next.txt-answer", true);
    }

    @Test
    public void evaluatorOpenLastTest() throws OpeningHoursParseException, OpeningHoursEvaluationException {
        evaluateDifferingEventBatchCheck("test-data/oh/open-last.txt-oh", "test-data/input-time/open-last.txt", "test-data/answer/open-last.txt-answer", false);
    }

    @Test
    public void unitTest() throws OpeningHoursParseException, OpeningHoursEvaluationException {
        assertTrue(evaluateCheck("00:00-02:00,12:00-14:00,17:00-24:00", "2021-06-09T15:00", Status.CLOSED));
        assertTrue(evaluateCheck("00:00-02:00,12:00-14:00,17:00-24:00", "2021-06-09T18:00", Status.OPEN));
        
        // open next test
        LocalDateTime time = LocalDateTime.parse("2021-07-01T12:00");
        LocalDateTime answerTime = LocalDateTime.parse("2021-07-01T14:00");
        assertEquals(answerTime, getNextEvent("14:00-18:00 unknown", time).getNextEventTime());

        // open last test
        LocalDateTime time1 = LocalDateTime.parse("2021-06-30T12:00");
        LocalDateTime answerTime1 = LocalDateTime.parse("2021-06-29T18:00");
        assertEquals(answerTime1, getLastEvent("14:00-18:00 unknown", time1).getLastEventTime());
    }

    @Test
    public void printTest() throws OpeningHoursParseException {
        // turn this to false if need to print normally
        boolean isDebug = true;
        printBatch("test-data/oh/timepoint.txt-oh", "2021-06-09T15:00", isDebug);
        printBatch("test-data/oh/weekday.txt-oh", "2021-06-09T15:00", isDebug);
        printBatch("test-data/oh/week.txt-oh", "2021-06-09T15:00", isDebug);
        printBatch("test-data/oh/month.txt-oh", "2021-06-09T15:00", isDebug);
        printBatch("test-data/oh/year.txt-oh", "2021-06-09T15:00", isDebug);
    }

    //-------------------------------------------------------------------------

    /**
     * Batch evaluation for an opening hours file, with input time values and its corresponding correct answers.
     * This is successful if the evaluator return all correct answer
     * 
     * @param openingHoursFile opening hours file
     * @param inputTimeFile input time value file
     * @param answerFile correct answer corresponding to each input time value
     */
    public static void evaluateBatchCheck(String openingHoursFile, String inputTimeFile, String answerFile) {
        BufferedReader openingHoursReader = null;
        BufferedReader inputTimeReader = null;
        BufferedReader answerReader = null;
        boolean hasWrong = false;
        int lineNumOH = 0;
        int lineNumInput = 0;
        try {
            openingHoursReader = new BufferedReader(new InputStreamReader(new FileInputStream(openingHoursFile), StandardCharsets.UTF_8));
            answerReader = new BufferedReader(new InputStreamReader(new FileInputStream(answerFile), StandardCharsets.UTF_8));
            String openingHours;
            String[] answers;
            lineNumOH = 1;
            while ((openingHours = openingHoursReader.readLine()) != null
                    &&(answers = answerReader.readLine().split("\\s+")) != null) {
                lineNumInput = 1;
                inputTimeReader = new BufferedReader(new InputStreamReader(new FileInputStream(inputTimeFile), StandardCharsets.UTF_8));
                for (String answerString : answers) {
                    String inputTime = inputTimeReader.readLine();
                    Status answer = Status.convert(answerString);
                    try {
                        if (!evaluateCheck(openingHours, inputTime, answer)) {
                            System.out.println("Opening hours file: " + openingHoursFile
                                            + ", line: " + lineNumOH);
                            System.out.println("Input time line: " + lineNumInput);
                            hasWrong = true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Error occured in " + openingHoursFile
                                        + ", line " + lineNumOH);
                        System.out.println("Input time line " + lineNumInput);
                        fail("Some exception occured during evaluating");
                    }
                    lineNumInput++;
                }
                lineNumOH++;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            fail("Null pointer exception occured, maybe some test cases doesn't have answer yet?");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail("File not found exception occured");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            fail("IOexception occured");
        } finally {
            try { 
                openingHoursReader.close();
                inputTimeReader.close();
                answerReader.close();
            } catch (IOException ioe) {
                fail("Error closing BufferedReader");
            }
        }
        if (hasWrong) fail("There's a wrong answer, check output for more info");
    }

    /**
     * Batch evaluation for an opening hours file, which are expected to throw
     * an exception, and its corresponding exception message file
     * <p>
     * This is successful if the evaluator throw all correct exception message
     * 
     * @param openingHoursFile opening hours file
     * @param exceptionMessageFile exception message file
     * @throws OpeningHoursParseException
     */
    public static void evaluateFailBatchCheck(String openingHoursFile, String exceptionMessageFile)
            throws OpeningHoursParseException {
        BufferedReader openingHoursReader = null;
        BufferedReader exceptionMessageReader = null;
        boolean hasWrong = false;
        int lineNumOH = 0;
        try {
            openingHoursReader = new BufferedReader(new InputStreamReader(new FileInputStream(openingHoursFile), StandardCharsets.UTF_8));
            exceptionMessageReader = new BufferedReader(new InputStreamReader(new FileInputStream(exceptionMessageFile), StandardCharsets.UTF_8));
            String openingHours;
            String exceptionMessage;
            lineNumOH = 1;
            while ((openingHours = openingHoursReader.readLine()) != null
                    && (exceptionMessage = exceptionMessageReader.readLine()) != null) {
                evaluateFail(openingHours, exceptionMessage, lineNumOH);
                lineNumOH++;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            fail("Null pointer exception occured, maybe some test cases doesn't have answer yet?");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail("File not found exception occured");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            fail("IOexception occured");
        } finally {
            try { 
                openingHoursReader.close();
            } catch (IOException ioe) {
                fail("Error closing BufferedReader");
            }
        }
        if (hasWrong) fail("There's a wrong answer, check output for more info");
    }

    /**
     * Batch evaluation for an opening hours file in getting next event
     * (open/close next), with input time from an inputTimeFile compared with
     * answers from an answerFile
     * <p>
     * This is successful if the evaluator get all next event correctly
     * 
     * @param openingHoursFile opening hours file
     * @param inputTimeFile input time file
     * @param answerFile answer file
     * @throws OpeningHoursParseException
     * @throws OpeningHoursEvaluationException
     */
    public static void evaluateDifferingEventBatchCheck(String openingHoursFile, String inputTimeFile, String answerFile, boolean isNext)
            throws OpeningHoursParseException, OpeningHoursEvaluationException {
        BufferedReader openingHoursReader = null;
        BufferedReader inputTimeReader = null;
        BufferedReader answerReader = null;
        boolean hasWrong = false;
        int lineNumInput = 1;
        try {
            inputTimeReader = new BufferedReader(new InputStreamReader(new FileInputStream(inputTimeFile), StandardCharsets.UTF_8));
            String inputTime;

            while ((inputTime = inputTimeReader.readLine()) != null) {
                lineNumInput = 1;
                openingHoursReader = new BufferedReader(new InputStreamReader(new FileInputStream(openingHoursFile), StandardCharsets.UTF_8));
                answerReader = new BufferedReader(new InputStreamReader(new FileInputStream(answerFile), StandardCharsets.UTF_8));
                LocalDateTime time = LocalDateTime.parse(inputTime);
                String openingHours;
                String answerString;
                int lineNumOH = 1;
                while ((openingHours = openingHoursReader.readLine()) != null
                        && (answerString = answerReader.readLine()) != null) {
                    String[] answers = answerString.split(",+");
                    String[] answer = answers[lineNumInput-1].split("\\s+");
                    
                    if (!evaluateDifferingEventCheck(openingHours, time, answer, isNext)) {
                        System.out.println("Opening hours file: " + openingHoursFile
                                         + ", line: " + lineNumOH);
                        System.out.println("Input time line: " + lineNumInput);
                        System.out.println();
                        hasWrong = true;
                    }
                    lineNumOH++;
                }
                lineNumInput++;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            fail("Null pointer exception occured, maybe some test cases doesn't have answer yet?");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail("File not found exception occured");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            fail("IOexception occured");
        } finally {
            try { 
                openingHoursReader.close();
                inputTimeReader.close();
                answerReader.close();
            } catch (IOException ioe) {
                fail("Error closing BufferedReader");
            }
        }
        if (hasWrong) fail("There's a wrong answer, check output for more info");
    }

    //-------------------------------------------------------------------------

    /**
     * Evaluation for an opening hours string, with input time value and its
     * corresponding correct answer.
     * <p>
     * This is used for unit test
     * 
     * @param openingHours opening hours string
     * @param timeString input time string in the form of "yyyy-mm-ddThh:mm"
     * @param answer correct answer corresponding to input time string
     * @throws OpeningHoursParseException
     * @throws OpeningHoursEvaluationException
     */
    public static boolean evaluateCheck(String openingHours, String timeString, Status answer)
            throws OpeningHoursParseException, OpeningHoursEvaluationException {
        LocalDateTime inputTime = LocalDateTime.parse(timeString);
        Result result = evaluate(openingHours, inputTime);
        Status givenAnswer = result.getStatus();
        if (givenAnswer != answer) {
            print(openingHours, inputTime);
            System.out.println("Wrong answer for \"" + openingHours);
            System.out.println("Input time: \"" + inputTime);
            System.out.println("Correct answer: " + answer);
            System.out.println("Given answer: " + givenAnswer);
            return false;
        }
        // if(result.hasComment()) {
        //     System.out.println("Comment for \"" + openingHours
        //  + "\" with input time \"" + inputTime + "\": " + result.getComment());
        // }
        return true;
    }

    /**
     * Evaluation for an opening hours string, with input time value
     * 
     * @param openingHours opening hours string
     * @param inputTime a LocalDateTime instance
     * @throws OpeningHoursEvaluationException
     * @throws OpeningHoursParseException
     */
    public static Result evaluate(String openingHours, LocalDateTime inputTime)
            throws OpeningHoursEvaluationException, OpeningHoursParseException {
        OpeningHoursEvaluator evaluator
            = new OpeningHoursEvaluator(openingHours, false);
        return evaluator.checkStatus(inputTime);
    }

    /**
     * Evaluation for an opening hours string, with input time value, which is
     * expected for throw an OpeningHoursEvaluationException exception
     * 
     * @param openingHours opening hours string
     * @param exceptionMessage expected exception message
     * @param lineNum optional line number
     * @throws OpeningHoursParseException
     */
    public static void evaluateFail(String openingHours, String exceptionMessage, int lineNum)
            throws OpeningHoursParseException {
        try {
            LocalDateTime inputTime = LocalDateTime.parse("2021-07-03T20:57:51");
            evaluate(openingHours, inputTime);
            fail("This OH tag " + openingHours + " (line num + " + lineNum
                + ") should have thrown an exception");
        } catch (OpeningHoursEvaluationException e) {
            assertEquals(exceptionMessage, e.getMessage());
        }
    }

    /**
     * Evaluation for getting next event (open/close next) of an opening hours
     * string, with input time value and its corresponding correct answer.
     * 
     * @param openingHours opening hours string
     * @param inputTime input time
     * @param answer answer string, formatted by
     *      "(1|0|x), (always|'instance of LocalDateTime YYYY-MM-DD'T'HH:MM')"
     * @return true if correct, false otherwise
     * @throws OpeningHoursParseException
     * @throws OpeningHoursEvaluationException
     */
    public static boolean evaluateDifferingEventCheck(String openingHours, LocalDateTime inputTime, String[] answer, boolean isNext)
            throws OpeningHoursParseException, OpeningHoursEvaluationException {
        boolean result = true;

        // get expected answer
        String timeString = answer[1];
        Status expectedStatus = Status.convert(answer[0]);
        boolean expectedAlways = timeString.equals("always");

        // get actual answer
        Result actual = (isNext) ? getNextEvent(openingHours, inputTime) 
                                 : getLastEvent(openingHours, inputTime);
        Status actualStatus = actual.getStatus();
        boolean actualAlways = actual.isAlways();
        LocalDateTime actualTime = (isNext)
                                    ? actual.getNextEventTime()
                                    : actual.getLastEventTime();

        // check expected and actual
        if (actualStatus == expectedStatus) {
            if (expectedAlways) {
                result = (expectedAlways == actualAlways);
            } else if (!actualAlways){
                result = LocalDateTime.parse(timeString).equals(actualTime);
            } else {
                result = false;
            }
        } else {    
            result = false;
        }

        // handle wrong answer
        if (!result) {
            String expectedAnswer = Status.convert(answer[0]) + ", " + answer[1];
            String givenAnswer = actualStatus + ", "
                + ((actual.isAlways()) ? "always" : actualTime);
            System.out.println("Wrong answer for \"" + openingHours + "\"");
            System.out.println("Input time: \"" + inputTime + "\"");
            System.out.println("Correct answer: " + expectedAnswer);
            System.out.println("Given answer: " + givenAnswer);
        }

        return result;
    }

    public static Result getNextEvent(String openingHours, LocalDateTime inputTime)
            throws OpeningHoursParseException, OpeningHoursEvaluationException {
        OpeningHoursEvaluator evaluator = new OpeningHoursEvaluator(openingHours, false);
        return evaluator.getNextEvent(inputTime);
    }

    public static Result getLastEvent(String openingHours, LocalDateTime inputTime)
            throws OpeningHoursParseException, OpeningHoursEvaluationException {
        OpeningHoursEvaluator evaluator = new OpeningHoursEvaluator(openingHours, false);
        return evaluator.getLastEvent(inputTime);
    }

    //-------------------------------------------------------------------------

    /**
     * Print the weekly schedule created by all opening hours in an input opening hours file,
     * the week data is taken from a LocalDateTime inputTime string
     * 
     * @param openingHoursFile OH files
     * @param inputTime for use to get week data
     * @param isDebug true to print debug string, false to print normal
     * @throws OpeningHoursParseException
     */
    public static void printBatch(String openingHoursFile, String inputTime, boolean isDebug)
            throws OpeningHoursParseException {
        System.out.println("Printing week schedule created from opening hours in "
                        + openingHoursFile);
        BufferedReader openingHoursReader = null;
        try {
            openingHoursReader = new BufferedReader(new InputStreamReader(new FileInputStream(openingHoursFile), StandardCharsets.UTF_8));
            String openingHours;
            while ((openingHours = openingHoursReader.readLine()) != null) {
                System.out.println(openingHours);
                LocalDateTime time = LocalDateTime.parse(inputTime);
                if (isDebug) {
                    printDebug(openingHours, time);
                } else {
                    print(openingHours, time);
                }
                System.out.println("___________________________________\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail("File not found exception occured");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            fail("IOexception occured");
        } finally {
            try { 
                openingHoursReader.close();
            } catch (IOException ioe) {
                fail("Error closing BufferedReader");
            }
        }
    }

    public static void print(String openingHours, LocalDateTime inputTime)
            throws OpeningHoursParseException {
        OpeningHoursEvaluator evaluator
            = new OpeningHoursEvaluator(openingHours, false);
        System.out.print(evaluator.toString(inputTime));
    }

    public static void printDebug(String openingHours, LocalDateTime inputTime)
            throws OpeningHoursParseException {
        OpeningHoursEvaluator evaluator
            = new OpeningHoursEvaluator(openingHours, false);
        System.out.print(evaluator.toDebugString(inputTime));
    }
}
