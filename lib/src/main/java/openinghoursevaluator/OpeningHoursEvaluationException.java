package openinghoursevaluator;

import javax.annotation.Nullable;

import ch.poole.openinghoursparser.Rule;

/**
 * An exception which will be thrown during evaluating opening hours
 */
public class OpeningHoursEvaluationException extends Exception {

    /**
     * Construct an exception from a message
     * 
     * @param message
     */
    public OpeningHoursEvaluationException(String message) {
        this(message, null);
    }

    /**
     * Construct an exception from a message and an optional Rule where the
     * exception happenend
     * 
     * @param message
     * @param failingRule
     */
    public OpeningHoursEvaluationException(String message, @Nullable Rule failingRule) {
        super(messsageBuilder(message, failingRule));
    }

    /**
     * Build a custom message based on input message and optional Rule where
     * the exception happened
     * 
     * @param message a message
     * @param rule an optional Rule
     * @return the message for Exception
     */
    private static String messsageBuilder(String message, @Nullable Rule rule) {
        return message + ((rule != null) ? " at " + rule : "");
    }
}
