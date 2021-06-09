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
    public void timespanTest() {
        evaluateBatchCheckForTimepoint("test-data/oh.txt", "test-data/input-time/timepoint.txt", "test-data/answer/timepoint.txt-answer", "2021-06-09");
    }

    @Test
    public void timespanUnitTest() {
        assertTrue(evaluateCheck("00:00-02:00,12:00-14:00,17:00-00:00", "2021-06-09T15:00", false));
        assertTrue(evaluateCheck("00:00-02:00,12:00-14:00,17:00-00:00", "2021-06-09T18:00", true));
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
    public void evaluateBatchCheckForTimepoint(String openingHoursFile, String inputTimeFile, String answerFile, String fixedDate) {
        BufferedReader openingHoursReader = null;
        BufferedReader inputTimeReader = null;
        BufferedReader answerReader = null;
        boolean hasWrong = false;
        try {
            openingHoursReader = new BufferedReader(new InputStreamReader(new FileInputStream(openingHoursFile), StandardCharsets.UTF_8));

            answerReader = new BufferedReader(new InputStreamReader(new FileInputStream(answerFile), StandardCharsets.UTF_8));
            String openingHours;
            String[] answers;
            int lineNumOH = 1;
            while((openingHours = openingHoursReader.readLine())    != null &&
                  (answers = answerReader.readLine().split("\\s+")) != null) {
                int lineNumInput = 1;
                inputTimeReader = new BufferedReader(new InputStreamReader(new FileInputStream(inputTimeFile), StandardCharsets.UTF_8));

                for(String answerString : answers) {
                    String inputTime = fixedDate + "T" + inputTimeReader.readLine();
                    boolean answer = answerString.equals("1");
                    boolean givenAnswer = evaluate(openingHours, inputTime);
                    if(givenAnswer != answer) {
                        hasWrong = true;
                        System.out.println("Wrong answer for \"" + openingHours + "\" in file " + openingHoursFile + ", line " + lineNumOH);
                        System.out.println("Input time: \"" + inputTime + "\"" + ", line " + lineNumInput);
                        System.out.println("Correct answer: " + answer);
                        System.out.println("Given answer: " + givenAnswer);
                        System.out.println();
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
        if(hasWrong) fail("There's a wrong answer, check output for more info");
    }

    /**
     * Evaluation for an opening hours string, with input time value and its corresponding correct answer.
     * This is used for unit test
     * 
     * @param openingHours opening hours string
     * @param inputTime input time string in the form of "yyyy-mm-ddThh:mm"
     * @param answer correct answer corresponding to input time string
     */
    public boolean evaluateCheck(String openingHours, String inputTime, boolean answer) {
        return evaluate(openingHours, inputTime) == answer;
    }

    /**
     * Evaluation for an opening hours string, with input time value
     * 
     * @param openingHours opening hours string
     * @param inputTime input time string in the form of "yyyy-mm-ddThh:mm"
     */
    public boolean evaluate(String openingHours, String inputTime) {
        OpeningHoursEvaluator evaluator = new OpeningHoursEvaluator(openingHours, false);
        // non-strict for now
        return evaluator.checkStatus(inputTime);
    }
}
