package openinghoursevaluator;

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
        evaluateBatch("test-data/oh.txt", "test-data/input-time/timespan.txt", "test-data/answer/timespan.txt-answer");
    }

    /**
     * Batch evaluation for an opening hours file, with input time values and its corresponding correct answers.
     * This is successful if the evaluator return all correct answer
     * 
     * @param openingHoursFile opening hours file
     * @param inputTimeFile input time value file
     * @param answerFile correct answer corresponding to each input time value
     */
    public void evaluateBatch(String openingHoursFile, String inputTimeFile, String answerFile) {
        BufferedReader openingHoursReader = null;
        BufferedReader inputTimeReader = null;
        BufferedReader answerReader = null;
        boolean hasWrong = false;
        try {
            openingHoursReader = new BufferedReader(new InputStreamReader(new FileInputStream(openingHoursFile), StandardCharsets.UTF_8));
            String openingHours;
            while((openingHours = openingHoursReader.readLine()) != null) {
                inputTimeReader = new BufferedReader(new InputStreamReader(new FileInputStream(inputTimeFile), StandardCharsets.UTF_8));
                answerReader = new BufferedReader(new InputStreamReader(new FileInputStream(answerFile), StandardCharsets.UTF_8));   
                String inputTime;
                String[] answers = answerReader.readLine().split("\\s+");
                int lineNum = 1;
                while((inputTime = inputTimeReader.readLine()) != null) {
                    OpeningHoursEvaluator evaluator = new OpeningHoursEvaluator(openingHours);
                    boolean answer = answers[lineNum-1].equals("1");
                    boolean givenAnswer = evaluator.checkStatusWithTime(inputTime);

                    if(givenAnswer != answer) {
                        hasWrong = true;
                        System.out.println("Wrong answer for \"" + openingHours + "\" in file " + openingHoursFile);
                        System.out.println("Input time: \"" + inputTime + "\"" + ", line " + lineNum);
                        System.out.println("Correct answer: " + answer);
                        System.out.println("Given answer: " + givenAnswer);
                        System.out.println();
                    }
                    lineNum++;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
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
     * This is successful if the evaluator return all correct answer
     * 
     * @param openingHours opening hours string
     * @param inputTime input time string
     * @param answer correct answer corresponding to input time string
     */
    public boolean evaluate(String openingHours, String inputTime, boolean answer) {
        OpeningHoursEvaluator evaluator = new OpeningHoursEvaluator(openingHours);
        return evaluator.checkStatusWithTime(inputTime);
    }
}
