import java.io.File;
import java.io.FileNotFoundException;

import org.assimbly.docconverter.DocConverter;

/**
 * Small processor class to convert holiday data from
 * https://github.com/commenthol/date-holidays which is in YAML format, to JSON
 * format
 */
public class DataProcessor {

    private static String getExtension(String filename) {
        return filename.substring(filename.length()-4, filename.length());
    }

    private static void convertSingleFile(String inputPath, String outputPath) throws Exception {
        String yaml = DocConverter.convertFileToString(inputPath);
        String json = DocConverter.convertYamlToJson(yaml);
        DocConverter.convertStringToFile(outputPath, json);
    }

    public static void main(String[] args) throws Exception {
        String resourcesFolder = System.getProperty("user.dir") + "/lib/src/main/resources/"; 
        File dir = new File(resourcesFolder + "holiday-data-yaml/");
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File file : directoryListing) {
                String filename = file.getCanonicalPath();
                if (getExtension(filename).equals("yaml")) {
                    convertSingleFile(filename, resourcesFolder + "holiday-data/" + file.getName().substring(0, 2) + ".json");
                }
            }
        } else {
            throw new FileNotFoundException("Resource file not found!");
        }
    }
}
