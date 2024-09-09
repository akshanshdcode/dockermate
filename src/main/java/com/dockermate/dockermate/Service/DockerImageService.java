package com.dockermate.dockermate.Service;

import com.dockermate.dockermate.Entity.DockerImage;
import com.dockermate.dockermate.Entity.DockerContainer;
import com.dockermate.dockermate.Entity.User;
import com.dockermate.dockermate.Repository.DockerImageRepository;
import com.dockermate.dockermate.Repository.DockerContainerRepository;
import com.dockermate.dockermate.Repository.UserRepository;
import com.dockermate.dockermate.Script.CreateImage;
import com.dockermate.dockermate.Script.DeleteImage;
import com.dockermate.dockermate.Script.GetStatusOfImage;

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
public class DockerImageService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DockerImageRepository dockerImageRepository;
    @Autowired
    private DockerContainerRepository dockerContainerRepository;
    @Autowired
    private CreateImage createImage;
    @Autowired
    private GetStatusOfImage getStatusOfImage;
    @Autowired
    private DeleteImage deleteImage;

    @Transactional
    public String createImage(String containerId, String imageName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            Optional<DockerContainer> optionalContainer = dockerContainerRepository.findById(containerId);
            if (!optionalContainer.isPresent()) {
                return "Container not found.";
            }

            int exitCode = createImage.createImage(containerId, imageName);
            String imageId = createImage.getImageId();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDate = LocalDateTime.now().format(formatter);

            if (exitCode == 0) {

                DockerImage dockerImage = new DockerImage();
                dockerImage.setImageId(imageId);
                dockerImage.setImageName(imageName);
                dockerImage.setStatus("The image is not in use.");
                dockerImage.setCreatedAt(formattedDate);
                dockerImage.setCreatedUsingContainerId(containerId);

                dockerImageRepository.save(dockerImage);

                Optional<User> userOptional = userRepository.findByUsername(username);
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    user.getDockerImages().add(dockerImage);
                    userRepository.save(user);
                } else {
                    return "User not found.";
                }

                return "Image created successfully. Image Name: " + imageName + "\nImage Id: " + imageId;
            } else {
                return "Failed to create image. Exit code: " + exitCode;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception occurred: " + e.getMessage();
        }
    }

    @Transactional
    public List<DockerImage> getUserImages() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return user.getDockerImages();
        } else {
            throw new RuntimeException("User not found");
        }
    }

    @Transactional
    public ResponseEntity<?> getImageStatus(String imageId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            // Fetch the user
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (!userOptional.isPresent()) {
                return new ResponseEntity<>("User not found.", HttpStatus.NOT_FOUND);
            }

            User user = userOptional.get();

            // Check if the image belongs to the user
            boolean isOwner = user.getDockerImages().stream()
                    .anyMatch(image -> image.getImageId().equals(imageId));

            if (!isOwner) {
                return new ResponseEntity<>("Image Not Found", HttpStatus.FORBIDDEN);
            }

            // Get the image status
            String status = getStatusOfImage.getStatus(imageId);
            return ResponseEntity.ok(status);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Exception occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public ResponseEntity<?> deleteImageById(String imageId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            // Fetch the user
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (!userOptional.isPresent()) {
                return new ResponseEntity<>("User not found.", HttpStatus.NOT_FOUND);
            }

            User user = userOptional.get();

            // Check if the image belongs to the user
            boolean isOwner = user.getDockerImages().stream()
                    .anyMatch(image -> image.getImageId().equals(imageId));

            if (!isOwner) {
                return new ResponseEntity<>("Image Not Found", HttpStatus.FORBIDDEN);
            }

            // Delete the image
            int exitCode = deleteImage.deleteImage(imageId);
            if (exitCode != 0) {
                return new ResponseEntity<>("Failed to delete image. Exit code: " + exitCode, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            user.getDockerImages().removeIf(image -> image.getImageId().equals(imageId));
            userRepository.save(user);

            dockerImageRepository.deleteById(imageId);

            return ResponseEntity.ok("Image deleted successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Exception occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<DockerImage> getImageById(String imageId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<User> userOptional = userRepository.findByUsername(username);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        User user = userOptional.get();
        Optional<DockerImage> imageOptional = dockerImageRepository.findById(imageId);

        if (imageOptional.isPresent()) {
            DockerImage dockerImage = imageOptional.get();
            // Check if the user owns the image
            if (dockerImage.getCreatedUsingContainerId() != null) {
                // Ensure image is linked to a container owned by the user
                boolean isOwner = user.getDockerContainers().stream()
                        .anyMatch(container -> container.getContainerId().equals(dockerImage.getCreatedUsingContainerId()));

                if (isOwner) {
                    return ResponseEntity.ok(dockerImage);
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
                }
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}
