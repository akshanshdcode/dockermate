package com.dockermate.dockermate.Service;

import com.dockermate.dockermate.Entity.DockerContainer;
import com.dockermate.dockermate.Entity.DockerImage;
import com.dockermate.dockermate.Entity.User;
import com.dockermate.dockermate.Repository.DockerContainerRepository;
import com.dockermate.dockermate.Repository.DockerImageRepository;
import com.dockermate.dockermate.Repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DockerContainerRepository dockerContainerRepository;
    @Autowired
    private DockerImageRepository dockerImageRepository;
    @Autowired
    private DockerContainerService dockerContainerService; // For container deletion
    @Autowired
    private DockerImageService dockerImageService;

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public void saveNewUser(User user){
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(Arrays.asList("USER"));
        userRepository.save(user);
    }

    public Optional<User> findByUsername(String username){
        return userRepository.findByUsername(username);
    }

    @Transactional
    public ResponseEntity<?> deleteUser() {
        System.out.println("deleteUser service called");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (!userOptional.isPresent()) {
                return new ResponseEntity<>("User not found.", HttpStatus.NOT_FOUND);
            }

            User user = userOptional.get();

            List<DockerContainer> userContainers = user.getDockerContainers();
            for (DockerContainer container : userContainers) {
                dockerContainerService.deleteContainer(container.getContainerId());
            }

            List<DockerImage> images = user.getDockerImages();
            for (DockerImage image : images) {
                dockerImageService.deleteImageById(image.getImageId());
            }

            userRepository.delete(user);

            return ResponseEntity.ok("User and associated data deleted successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Exception occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
