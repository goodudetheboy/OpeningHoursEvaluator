package openinghoursevaluator;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /** Default constructor */
    private Utils() {
        // empty on purpose
    }

    /**
     * Check if input value is in between the two start and end value
     * 
     * @param <T> a Comparable object
     * @param value value to be checked 
     * @param start start value
     * @param end end value
     * @return true if input value is in between the two start and end value
     */
    public static <T extends Comparable <T>> boolean isBetween(T value, T start, T end) {
        return value.compareTo(start) >= 0 && value.compareTo(end) <= 0;
    }

    /**
     * Convert hour and minute of a LocalDateTime instance to minutes
     *  
     * @param time a LocalDateTime instance
     * @return minutes
    */
    public static int timeInMinute(LocalDateTime time) {
        return time.getHour()*60 + time.getMinute();
    }


    /**
     * Get the overlap range of the alpha range (a1 to a2) and the beta
     * range(b1 to b2).
     * <p>
     * Note that the start must be less or equal to the ends
     * 
     * @param <T> a Comparable object
     * @param a1 the start value of the alpha range
     * @param a2 the end value of the alpha range
     * @param b1 the start value of the beta range
     * @param b2 the end value of the beta range
     * @return the overlap range in the form of List T
     */
    public static <T extends Comparable<T>> List<T> getOverlap(T a1, T a2, T b1, T b2) {
        int overlapsCode = overlapsCode(a1, a2, b1, b2);
        if (overlapsCode == 0) {
            return null;
        }

        T startOverlap = a1;
        T endOverlap = a2;

        switch (overlapsCode) {
        case 1:
            // startOverlap = a1;
            // endOverlap = a2;
            break;
        case 2:
            // startOverlap = a1;
            endOverlap = b2;            
            break;
        case 3:
            startOverlap = b1;
            // endOverlap = a2;
            break;
        case 4:
            startOverlap = b1;
            endOverlap = b2;
            break; 
        default: // hopefully this never gets here
        }
        List<T> result = new ArrayList<>();
        result.add(startOverlap);
        result.add(endOverlap);
        return result;
    }

    /**
     * Returns a code indicating how an alpha range (a1 to a2) is overlapped
     * with another beta range (b1 to b2), adhering to the following:
     * <ul>
     * <li>0: Doesn't overlap
     * <li>1: The alpha range is inside the beta range
     * <li>2: The alpha range has start inside the beta range and
     * end outside
     * <li>3: The alpha range has start outside the beta range and
     * end inside
     * <li>4: The beta range is inside the alpha range
     * </ul>
     * <p>
     * Note that the start must be less or equal to the ends
     * 
     * @param <T> a Comparable object
     * @param a1 the start value of the alpha range
     * @param a2 the end value of the alpha range
     * @param b1 the start value of the beta range
     * @param b2 the end value of the beta range
     * @return a number (0-4) adhering to the above specification
     */
    public static <T extends Comparable<T>> int overlapsCode(T a1, T a2, T b1, T b2) {
        if (a1.compareTo(a2) > 0 || b1.compareTo(b2) > 0) {
            throw new IllegalArgumentException("Start must be less or equal to end");
        }
        if (a1.compareTo(b2) > 0 || a2.compareTo(b1) < 0) {
            return 0;
        }
        if (Utils.isBetween(a1, b1, b2)) {
            return (Utils.isBetween(a2, b1, b2)) ? 1 : 2;
        } else {
            return (Utils.isBetween(a2, b1, b2)) ? 3 : 4;
        }
    }

    /**
     * Check if an alpha range (a1 to a2) has an overlap with a beta range 
     * (b1 to b2).
     * <p>
     * Note that the start must be less or equal to the ends
     * 
     * @param <T> a Comparable object
     * @param a1 the start value of the alpha range
     * @param a2 the end value of the alpha range
     * @param b1 the start value of the beta range
     * @param b2 the end value of the beta range
     * @return true if alpha range has an overlap with beta range
     */
    public static <T extends Comparable<T>> boolean isOverlapped(T a1, T a2, T b1, T b2) {
        return overlapsCode(a1, a2, b1, b2) != 0;
    }

    /**
     * Create a string from the content of input file, encoded by input charset
     * <p>
     * This code was adapted from the following Stack Overflow post:
     * https://stackoverflow.com/questions/326390/how-do-i-create-a-java-string-from-the-contents-of-a-file
     * 
     * @param path path to the file
     * @param encoding the charset to use
     * @return String created from content of a file
     * @throws IOException
     * @author erickson, https://stackoverflow.com/users/3474/erickson
     */
    static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
}
