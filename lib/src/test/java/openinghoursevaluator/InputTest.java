package openinghoursevaluator;

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
    public void evaluatorTest() {
        evaluateBatchCheck("test-data/oh/timepoint.txt-oh", "test-data/input-time/timepoint.txt", "test-data/answer/timepoint.txt-answer");
        evaluateBatchCheck("test-data/oh/weekday.txt-oh", "test-data/input-time/weekday.txt", "test-data/answer/weekday.txt-answer");
    }

    @Test
    public void unitTest() {
        assertTrue(evaluateCheck("00:00-02:00,12:00-14:00,17:00-24:00", "2021-06-09T15:00", Status.CLOSED, "xxxxx", 0, 0));
        assertTrue(evaluateCheck("00:00-02:00,12:00-14:00,17:00-24:00", "2021-06-09T18:00", Status.OPEN, "xxxxx", 0, 0));
    }

    @Test
    public void printCheck() {
        printBatch("test-data/oh/timepoint.txt-oh", "2021-06-09T15:00");
        printBatch("test-data/oh/weekday.txt-oh", "2021-06-09T15:00");
    }

    /** Used for checking on the spot, convenient during debugging */
    @Test
    public void spotCheck() {
        // assertTrue(evaluateCheck("00:00-02:00,17:00-24:00, 12:00-14:00; 15:00-16:00 unknown", "2021-06-09T03:00", Status.CLOSED, "xxxxx", 0, 0));
        // print("Tue,Thu 12:00-35:00 open \"on special occasions only\", Tue, Thu 06:00-12:00 \"hours too hard to see\"", "2021-06-09T03:00");
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
    public void evaluateBatchCheck(String openingHoursFile, String inputTimeFile, String answerFile) {
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
    public boolean evaluateCheck(String openingHours, String inputTime, Status answer, String openingHoursFile, int lineNumOH, int lineNumInput) {
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
            if(result.hasComment()) {
                System.out.println("Comment for \"" + openingHours + "\" with input time \"" + inputTime + "\": " + result.getComment());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error occured in " + openingHoursFile + ", line " + lineNumOH);
            System.out.println("Input time line " + lineNumInput);
            fail("Some exception occured during evaluating");
        }
        
        return true;
    }

    /**
     * Evaluation for an opening hours string, with input time value
     * 
     * @param openingHours opening hours string
     * @param inputTime input time string in the form of "yyyy-mm-ddThh:mm"
     */
    public Result evaluate(String openingHours, String inputTime) {
        OpeningHoursEvaluator evaluator = new OpeningHoursEvaluator(openingHours, false);
        return evaluator.checkStatus(inputTime);
    }

    /**
     * Print the weekly schedule created by all opening hours in an input opening hours file,
     * the week data is taken from a LocalDateTime inputTime string
     * 
     * @param openingHoursFile OH files
     * @param inputTime for use to get week data
     */
    public void printBatch(String openingHoursFile, String inputTime) {
        System.out.println("Printing week schedule created from opening hours in " + openingHoursFile);
        BufferedReader openingHoursReader = null;
        try {
            openingHoursReader = new BufferedReader(new InputStreamReader(new FileInputStream(openingHoursFile), StandardCharsets.UTF_8));
            String openingHours;
            while ((openingHours = openingHoursReader.readLine()) != null) {
                System.out.println(openingHours);
                print(openingHours, inputTime);
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

    public void print(String openingHours, String inputTime) {
        OpeningHoursEvaluator evaluator = new OpeningHoursEvaluator(openingHours, false);
        System.out.print(evaluator.toString(inputTime));
    }
}
