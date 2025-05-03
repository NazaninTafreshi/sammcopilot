package io.github.nazaninmtafreshi;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Util {
    public static String readFileContent(String fileName) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString().trim();
    }

    public static void saveToFile(String content, String outputFilePath) {
        Path outputPath = Paths.get(outputFilePath);
        try {
            if (!Files.exists(outputPath.getParent())) {
                Files.createDirectories(outputPath.getParent());
            }
            Files.writeString(outputPath, content);
        } catch (IOException e) {
            System.out.println("An error occurred while creating the folder or writing to the " + outputPath + " file.");
            e.printStackTrace();
        }
    }
}
