package com.dockermate.dockermate.Script;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
public class DeleteImage {

    public int deleteImage(String imageId) {
        ProcessBuilder processBuilder = new ProcessBuilder("docker", "rmi", imageId);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            return process.waitFor();
        } catch (IOException e) {
            throw new RuntimeException("IOException occurred while deleting the image", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("InterruptedException occurred while deleting the image", e);
        }
    }
}