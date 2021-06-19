package openinghoursevaluator;

import ch.poole.openinghoursparser.RuleModifier;
import ch.poole.openinghoursparser.RuleModifier.Modifier;

public enum Status {
    OPEN("opening"), CLOSED("closed"), UNKNOWN("unknown");

    private final String name;

    Status(String name) {
        this.name = name;
    }

    /**
     * Convert input Character from the test cases to its corresponding Status.
     * 0: CLOSED,
     * 1: OPEN,
     * x: UNKNOWN
     * 
     * @param input desired character from above list
     * @return a corresponding Status, null if not either 0, 1, or x
     */
    public static Status convert(String input) {
        switch(input) {
            case "0":   return CLOSED;
            case "1":   return OPEN;
            case "x":   return UNKNOWN;
            default:    return null;
        }
    }

    /**
     * Convert Modifier inside a RuleModifier to Status
     * If RuleModifier is null, the Status is OPEN
     * 
     * @param input a RuleModifier
     * @return a corresponding Status
     */
    public static Status convert(RuleModifier input) {
        if (input == null) {
            return OPEN;
        }
        if(input.getModifier() == null && input.getComment() != null) {
            return UNKNOWN;
        }
        return convert(input.getModifier());
    }

    /**
     * Convert Modifier to Status
     * 
     * @param input a Modifier
     * @return a corresponding Status
     */
    public static Status convert(Modifier input) {
        switch(input) {
            case OPEN: return OPEN;
            case CLOSED:
            case OFF: return CLOSED;
            case UNKNOWN: return UNKNOWN;
            default: return null;
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
