package com.dockermate.dockermate.Service;

import com.dockermate.dockermate.Entity.DockerContainer;
import com.dockermate.dockermate.Entity.DockerImage;
import com.dockermate.dockermate.Entity.User;
import com.dockermate.dockermate.Repository.DockerContainerRepository;
import com.dockermate.dockermate.Repository.DockerImageRepository;
import com.dockermate.dockermate.Repository.UserRepository;
import com.dockermate.dockermate.Script.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class DockerContainerService {

    @Autowired
    private DockerContainerRepository dockerContainerRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CreateContainer createContainer;
    @Autowired
    private DeleteContainer deleteContainer;
    @Autowired
    private CommandInContainer commandInContainer;
    @Autowired
    private StartAndStop startAndStop;
    @Autowired
    private GetStatusOfContainer getStatusOfContainer;
    @Autowired
    private DockerImageRepository dockerImageRepository;

    public String createContainer(DockerContainer dockerRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        String containerName = dockerRequest.getContainerName();
        String imageName = dockerRequest.getImageName();

        try {

            int exitCode = createContainer.createContainer(containerName, imageName);
            String containerId = createContainer.getContainerId();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDate = LocalDateTime.now().format(formatter);

            if (exitCode == 0) {
                DockerContainer dockerContainer = new DockerContainer();
                dockerContainer.setContainerId(containerId);
                dockerContainer.setContainerName(containerName);
                dockerContainer.setImageName(imageName);
                dockerContainer.setCreatedAt(formattedDate);
                dockerContainer.setStatus(getStatusOfContainer.getStatus(containerId));

                dockerContainerRepository.save(dockerContainer);

                Optional<User> userOptional = userRepository.findByUsername(username);
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    user.getDockerContainers().add(dockerContainer);
                    userRepository.save(user);
                } else {
                    return "User not found.";
                }

                return "Container created successfully. Container ID: " + containerId;
            } else {
                return "Failed to create container. Exit code: " + exitCode;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception occurred: " + e.getMessage();
        }
    }


    @Transactional
    public List<DockerContainer> getUserContainers() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return user.getDockerContainers();
        } else {
            throw new RuntimeException("User not found");
        }
    }

    public ResponseEntity<?> getUserContainer(String containerId){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();

            Optional<DockerContainer> containerOptional = user.getDockerContainers()
                    .stream()
                    .filter(container -> container.getContainerId().equals(containerId))
                    .findFirst();

            if (containerOptional.isPresent()) {
                return ResponseEntity.ok(containerOptional.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Container not found.");
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }
    }

    @Transactional
    public ResponseEntity<String> deleteContainer(String containerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (!userOptional.isPresent()) {
                return new ResponseEntity<>("User not found.", HttpStatus.NOT_FOUND);
            }

            User user = userOptional.get();

            boolean isOwner = user.getDockerContainers().stream()
                    .anyMatch(container -> container.getContainerId().equals(containerId));

            if (!isOwner) {
                return new ResponseEntity<>("Container Not Found", HttpStatus.FORBIDDEN);
            }

            int exitCode = deleteContainer.deleteContainer(containerId);
            if (exitCode != 0) {
                return new ResponseEntity<>("Failed to delete container. Exit code: " + exitCode, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            user.getDockerContainers().removeIf(container -> container.getContainerId().equals(containerId));
            userRepository.save(user);

            dockerContainerRepository.deleteById(containerId);

            List<DockerImage> images = dockerImageRepository.findByCreatedUsingContainerId(containerId);
            for (DockerImage image : images) {
                image.setCreatedUsingContainerId(null);
                dockerImageRepository.save(image);
            }

            return ResponseEntity.ok("Container deleted successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Exception occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public String CommandRequestInContainer(String containerId, String command) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (!userOptional.isPresent()) {
                return "User not found.";
            }

            User user = userOptional.get();
            boolean isOwner = user.getDockerContainers().stream()
                    .anyMatch(container -> container.getContainerId().equals(containerId));

            if (!isOwner) {
                return "Unauthorized: You do not own this container.";
            }

            Optional<DockerContainer> optionalContainer = dockerContainerRepository.findById(containerId);
            if (!optionalContainer.isPresent()) {
                return "Container not found.";
            }

            String status = getStatusOfContainer.getStatus(containerId);
            if (status.contains("exited")) {
                return "Container is not running. Current status: " + status;
            }

            int exitCode = commandInContainer.executeCommand(containerId, command);
            if (exitCode == 0) {
                return "Command run successfully.";
            } else {
                return "Failed to run command. Exit code: " + exitCode;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception occurred: " + e.getMessage();
        }
    }

    @Transactional
    public String startContainer(String containerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (!userOptional.isPresent()) {
                return "User not found.";
            }

            User user = userOptional.get();
            boolean isOwner = user.getDockerContainers().stream()
                    .anyMatch(container -> container.getContainerId().equals(containerId));

            if (!isOwner) {
                return "Unauthorized: You do not own this container.";
            }

            String command = "docker start " + containerId;

            String oldStatus = getStatusOfContainer.getStatus(containerId);
            if (oldStatus.contains("running")) {
                return String.format("Container is already running. Current status: %s", oldStatus);
            }

            String startCommandResult = startAndStop.executeCommand(command);

            Optional<DockerContainer> optionalDockerContainer = dockerContainerRepository.findById(containerId);
            if (!optionalDockerContainer.isPresent()) {
                return "Container not found.";
            }

            DockerContainer dockerContainer = optionalDockerContainer.get();
            String status = getStatusOfContainer.getStatus(containerId);
            dockerContainer.setStatus(status);
            dockerContainerRepository.save(dockerContainer);

            return String.format("Container ID: %s\nCommand Result: %s\nStatus: %s", containerId, startCommandResult, status);
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception occurred: " + e.getMessage();
        }
    }

    @Transactional
    public String stopContainer(String containerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (!userOptional.isPresent()) {
                return "User not found.";
            }

            User user = userOptional.get();
            boolean isOwner = user.getDockerContainers().stream()
                    .anyMatch(container -> container.getContainerId().equals(containerId));

            if (!isOwner) {
                return "Unauthorized: You do not own this container.";
            }

            String command = "docker stop " + containerId;

            String oldStatus = getStatusOfContainer.getStatus(containerId);
            if (oldStatus.contains("exited")) {
                return String.format("Container is already stopped. Current status: %s", oldStatus);
            }

            String stopCommandResult = startAndStop.executeCommand(command);

            Optional<DockerContainer> optionalDockerContainer = dockerContainerRepository.findById(containerId);
            if (!optionalDockerContainer.isPresent()) {
                return "Container not found.";
            }

            DockerContainer dockerContainer = optionalDockerContainer.get();
            String status = getStatusOfContainer.getStatus(containerId);
            dockerContainer.setStatus(status);
            dockerContainerRepository.save(dockerContainer);

            return String.format("Container ID: %s\nCommand Result: %s\nStatus: %s", containerId, stopCommandResult, status);
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception occurred: " + e.getMessage();
        }
    }

    public String getContainerStatus(String containerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (!userOptional.isPresent()) {
                return "User not found.";
            }

            User user = userOptional.get();
            boolean isOwner = user.getDockerContainers().stream()
                    .anyMatch(container -> container.getContainerId().equals(containerId));

            if (!isOwner) {
                return "Unauthorized: You do not own this container.";
            }

            return getStatusOfContainer.getStatus(containerId);
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception occurred: " + e.getMessage();
        }
    }
}