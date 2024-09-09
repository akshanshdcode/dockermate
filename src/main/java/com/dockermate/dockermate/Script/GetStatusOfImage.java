package com.dockermate.dockermate.Script;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
public class GetStatusOfImage {

    public String getStatus(String imageId) {
        String command = "docker ps -a --filter ancestor=" + imageId + " --format {{.Status}}";
        StringBuilder output = new StringBuilder();

        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            process.waitFor();

            if (output.toString().isEmpty()) {
                return "The image is not in use.";
            } else {
                return "The image is in use by the following container(s):\n" + output.toString();
            }

        } catch (IOException e) {
            return "Error: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: " + e.getMessage();
        }
    }
}