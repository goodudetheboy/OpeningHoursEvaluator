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

import org.junit.Test;

public class InputTest {
    /**
     * Test only for the timespan test (hours and minutes ONLY)
     */
    @Test
    public void evaluatorTimepointTest() {
        evaluateBatchCheck("test-data/oh/timepoint.txt-oh", "test-data/input-time/timepoint.txt", "test-data/answer/timepoint.txt-answer");
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
    public void evaluatorFailTest() {
        evaluateFailBatchCheck("test-data/oh/fail.txt-oh", "test-data/answer/fail.txt-answer");
    }

    @Test
    public void unitTest() {
        assertTrue(evaluateCheck("00:00-02:00,12:00-14:00,17:00-24:00", "2021-06-09T15:00", Status.CLOSED, "xxxxx", 0, 0));
        assertTrue(evaluateCheck("00:00-02:00,12:00-14:00,17:00-24:00", "2021-06-09T18:00", Status.OPEN, "xxxxx", 0, 0));
    }

    @Test
    public void printTest() {
        // turn this to false if need to print normally
        boolean isDebug = true;
        printBatch("test-data/oh/timepoint.txt-oh", "2021-06-09T15:00", isDebug);
        printBatch("test-data/oh/weekday.txt-oh", "2021-06-09T15:00", isDebug);
        printBatch("test-data/oh/week.txt-oh", "2021-06-09T15:00", isDebug);
        printBatch("test-data/oh/month.txt-oh", "2021-06-09T15:00", isDebug);
        printBatch("test-data/oh/year.txt-oh", "2021-06-09T15:00", isDebug);
    }

    /** Used for checking on the spot, convenient during debugging */
    @Test
    public void spotCheck() {
        // assertTrue(evaluateCheck("00:00-02:00,17:00-24:00, 12:00-14:00; 15:00-16:00 unknown", "2021-06-09T03:00", Status.CLOSED, "xxxxx", 0, 0));
        // print("Jun 4, 12, 20-25, Jul 2-14, 23-31; Jun 14-20 unknown; Jun 17-20 off \"something happens\"; Jun 29-Jul 3 00:00-48:00 \"nothing here\"", "2021-07-04T03:00");
    }

    /**
     * Batch evaluation for an opening hours file, with input time values and its corresponding correct answers.
     * This is successful if the evaluator return all correct answer
     * 
     * This is still under construction since I'm moving to LocalDateTime instead
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
            while ((openingHours = openingHoursReader.readLine())    != null &&
                  (answers = answerReader.readLine().split("\\s+")) != null) {
                lineNumInput = 1;
                inputTimeReader = new BufferedReader(new InputStreamReader(new FileInputStream(inputTimeFile), StandardCharsets.UTF_8));
                for (String answerString : answers) {
                    String inputTime = inputTimeReader.readLine();
                    Status answer = Status.convert(answerString);
                    if (!evaluateCheck(openingHours, inputTime, answer, openingHoursFile, lineNumOH, lineNumInput)) {
                        hasWrong = true;
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
     * Evaluation for an opening hours string, with input time value and its corresponding correct answer.
     * This is used for unit test
     * 
     * @param openingHours opening hours string
     * @param inputTime input time string in the form of "yyyy-mm-ddThh:mm"
     * @param answer correct answer corresponding to input time string
     */
    public static boolean evaluateCheck(String openingHours, String inputTime, Status answer, String openingHoursFile, int lineNumOH, int lineNumInput) {
        try {
            Result result = evaluate(openingHours, inputTime);
            Status givenAnswer = result.getStatus();
            if (givenAnswer != answer) {
                print(openingHours, inputTime);
                System.out.println("Wrong answer for \"" + openingHours + "\" in file " + openingHoursFile + ", line " + lineNumOH);
                System.out.println("Input time: \"" + inputTime + "\"" + ", line " + lineNumInput);
                System.out.println("Correct answer: " + answer);
                System.out.println("Given answer: " + givenAnswer);
                System.out.println();
                return false;
            }
            // if(result.hasComment()) {
            //     System.out.println("Comment for \"" + openingHours + "\" with input time \"" + inputTime + "\": " + result.getComment());
            // }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error occured in " + openingHoursFile + ", line " + lineNumOH);
            System.out.println("Input time line " + lineNumInput);
            fail("Some exception occured during evaluating");
        }
        
        return true;
    }

    public static void evaluateFailBatchCheck(String openingHoursFile, String exceptionMessageFile) {
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
            while ((openingHours = openingHoursReader.readLine()) != null &&
                    (exceptionMessage = exceptionMessageReader.readLine()) != null) {
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
     * Evaluation for an opening hours string, with input time value
     * 
     * @param openingHours opening hours string
     * @param inputTime input time string in the form of "yyyy-mm-ddThh:mm"
     * @throws OpeningHoursEvaluationException
     */
    public static Result evaluate(String openingHours, String inputTime) throws OpeningHoursEvaluationException {
        OpeningHoursEvaluator evaluator = new OpeningHoursEvaluator(openingHours, false);
        return evaluator.checkStatus(inputTime);
    }

    public static void evaluateFail(String openingHours, String exceptionMessage, int lineNum) {
        try {
            evaluate(openingHours, "2021-07-03T20:57:51");
            fail("This OH tag " + openingHours + " (line num + " + lineNum + ") should have thrown an exception");
        } catch (OpeningHoursEvaluationException e) {
            assertEquals(exceptionMessage, e.getMessage());
        }
    }

    /**
     * Print the weekly schedule created by all opening hours in an input opening hours file,
     * the week data is taken from a LocalDateTime inputTime string
     * 
     * @param openingHoursFile OH files
     * @param inputTime for use to get week data
     * @param isDebug true to print debug string, false to print normal
     */
    public static void printBatch(String openingHoursFile, String inputTime, boolean isDebug) {
        System.out.println("Printing week schedule created from opening hours in " + openingHoursFile);
        BufferedReader openingHoursReader = null;
        try {
            openingHoursReader = new BufferedReader(new InputStreamReader(new FileInputStream(openingHoursFile), StandardCharsets.UTF_8));
            String openingHours;
            while ((openingHours = openingHoursReader.readLine()) != null) {
                System.out.println(openingHours);
                if (isDebug) {
                    printDebug(openingHours, inputTime);
                } else {
                    print(openingHours, inputTime);
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

    public static void print(String openingHours, String inputTime) {
        OpeningHoursEvaluator evaluator = new OpeningHoursEvaluator(openingHours, false);
        System.out.print(evaluator.toString(inputTime));
    }

    public static void printDebug(String openingHours, String inputTime) {
        OpeningHoursEvaluator evaluator = new OpeningHoursEvaluator(openingHours, false);
        System.out.print(evaluator.toDebugString(inputTime));
    }
}
