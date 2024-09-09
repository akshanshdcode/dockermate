package com.dockermate.dockermate.Script;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
public class CreateContainer {

    private String containerId;

    public int createContainer(String containerName, String imageName) {
        ProcessBuilder processBuilder = new ProcessBuilder("docker", "run", "-d", "--name", containerName, imageName);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                this.containerId = output.toString().trim();
            }

            return process.waitFor();
        } catch (IOException e) {
            throw new RuntimeException("IOException occurred: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            throw new RuntimeException("InterruptedException occurred: " + e.getMessage(), e);
        }
    }

    public String getContainerId() {
        return containerId;
    }
}