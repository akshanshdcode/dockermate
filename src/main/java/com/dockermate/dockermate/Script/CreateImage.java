package com.dockermate.dockermate.Script;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
public class CreateImage {

    private String imageId;

    public int createImage(String containerId, String imageName) {
        ProcessBuilder processBuilder = new ProcessBuilder("docker", "commit", containerId, imageName);
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

            this.imageId = output.toString().trim();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println("Error occurred while creating the image: " + output.toString());
            }

            return exitCode;
        } catch (IOException e) {
            throw new RuntimeException("IOException occurred: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("InterruptedException occurred: " + e.getMessage(), e);
        }
    }

    public String getImageId() {
        return imageId;
    }
}