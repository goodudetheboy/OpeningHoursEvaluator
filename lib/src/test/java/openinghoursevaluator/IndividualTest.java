package openinghoursevaluator;

import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

import ch.poole.openinghoursparser.OpeningHoursParseException;

public class IndividualTest {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in); 
        Boolean isStrict = args[0].equals("true");
        while(true) {
            System.out.print("\nPlease enter your opening hour tag: ");
            String input = sc.nextLine();
            try {
                OpeningHoursEvaluator evaluator = new OpeningHoursEvaluator(input, isStrict);
                while(true) {
                    System.out.print("\nPlease enter time to be checked (takes the form YYYY-MM-DDTHH:MM, for example "
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                        + ")\nor press q to change to a different opening hours tag: ");
                    String inputTime = sc.nextLine();
                    if (inputTime.equals("q")) break;
                    System.out.println("\nEvaluating " + inputTime);
                    try {
                        Result result = evaluator.checkStatus(inputTime);
                        System.out.println("Current status: " + result.getStatus() +
                            ((result.hasComment()) ? ", comment: " + result.getComment() : ""));
                    } catch (OpeningHoursEvaluationException e) {
                        e.printStackTrace();
                    } catch (DateTimeParseException e) {
                        System.out.println("Invalid input time string");
                    }
                    System.out.println("\n------------------------------");
                }
            } catch (OpeningHoursParseException e) {
                System.out.println("Illegal input string");
                e.printStackTrace();
            } 
        }
    }
}
