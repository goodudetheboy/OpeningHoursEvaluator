package openinghoursevaluator;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import org.junit.Test;

public class DataTest {
    /**
     * Test if the input time value strings are legal according to the opening hours specification 
     * (because I'll be using it for developing the evaluator)
     *    
     */
    @Test
    public void inputTimeFolderLegalTest() {
        inputTimeFileLegalTest("test-data/input-time/timepoint.txt");
        inputTimeFileLegalTest("test-data/input-time/weekday.txt");
    }

    public void inputTimeFileLegalTest(String inputFileDir) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFileDir), StandardCharsets.UTF_8));
            String data;
            int lineNum = 1;
            while ((data = reader.readLine()) != null) {
                try {
                    LocalDateTime.parse(data);
                } catch (DateTimeParseException e) {
                    fail("Input value \"" + data + "\" is illegal in line " + lineNum + " of file " + inputFileDir);
                }
                lineNum++;
            } 
        } catch (FileNotFoundException e) {
            fail("File not found exception occured");
            e.printStackTrace();
        } catch (IOException ioe) {
            fail("IO exception occured");
            ioe.printStackTrace();
        } finally {
            try { 
                reader.close();
            } catch (IOException ioe) {
                fail("Error closing BufferedReader");
            }
        }
    }  
}
