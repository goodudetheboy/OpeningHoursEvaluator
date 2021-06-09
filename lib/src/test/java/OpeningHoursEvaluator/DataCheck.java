package openinghoursevaluator;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.ParseException;

public class DataCheck {
    /**
     * Test if the input time value strings are legal according to the opening hours specification 
     * (because I'll be using it for developing the evaluator)
     *    
     */
    @Test
    public void inputTimeFolderLegalTest() {
        assertTrue(inputTimeFileLegalTest("test-data/input-time/timepoint.txt", false));
    }   

    public boolean inputTimeFileLegalTest(String inputFileDir, boolean isStrict) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFileDir), StandardCharsets.UTF_8));
            String data;
            int lineNum = 1;
            while ((data = reader.readLine()) != null) {
                try {
                    OpeningHoursParser parser = new OpeningHoursParser(new  ByteArrayInputStream(data.getBytes()));
                    List<ch.poole.openinghoursparser.Rule> rules = parser.rules(isStrict);
                } catch (ParseException pex) {
                    fail("Input value \"" + data + "\" is illegal in line " + lineNum + " of file " + inputFileDir);
                    return false;
                }
                lineNum++;
            } 
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        } finally {
            try { 
                reader.close();
            } catch (IOException ioe) {
                fail("Error closing BufferedReader");
                return false;
            }
        }
        return true;
    }
}
