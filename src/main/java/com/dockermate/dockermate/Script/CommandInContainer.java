package com.dockermate.dockermate.Script;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

@Component
public class CommandInContainer {

    private static final Logger LOGGER = Logger.getLogger(CommandInContainer.class.getName());

    public int executeCommand(String containerId, String command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("docker", "exec", containerId, "sh", "-c", command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                LOGGER.info("Command output: " + output.toString());
            }

            int exitCode = process.waitFor();
            return exitCode;
        } catch (IOException e) {
            LOGGER.severe("IOException occurred: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            LOGGER.severe("InterruptedException occurred: " + e.getMessage());
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}