package com.dockermate.dockermate.Controller;

import com.dockermate.dockermate.Entity.*;
import com.dockermate.dockermate.Service.DockerContainerService;
import com.dockermate.dockermate.Service.DockerImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/docker")
public class DockerController {

    @Autowired
    private DockerContainerService dockerContainerService;

    @Autowired
    private DockerImageService dockerImageService;

    @PostMapping("/container")
    public ResponseEntity<?> createContainer(@RequestBody DockerContainer dockerContainer) {
        String createdContainer = dockerContainerService.createContainer(dockerContainer);
        if (createdContainer != null && !createdContainer.isEmpty()) {
            return ResponseEntity.ok(createdContainer);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/containers")
    public ResponseEntity<?> getContainers() {
        try {
            List<DockerContainer> containers = dockerContainerService.getUserContainers();
            return ResponseEntity.ok(containers);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/container")
    public ResponseEntity<?> getContainer(ContainerRequest request) {
        return dockerContainerService.getUserContainer(request.getContainerId());
    }

    @DeleteMapping("/container")
    public ResponseEntity<?> deleteContainer(@RequestBody ContainerRequest request) {
        return dockerContainerService.deleteContainer(request.getContainerId());
    }

    @PostMapping("/container/command")
    public ResponseEntity<?> CommandRequestWithinContainer(@RequestBody CommandRequest request) {
        String containerId = request.getContainerId();
        String command = request.getCommand();
        String result = dockerContainerService.CommandRequestInContainer(containerId, command);

        if (result.contains("successfully")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("/container/status")
    public ResponseEntity<String> getContainerStatus(@RequestBody ContainerRequest request) {
        return ResponseEntity.ok(dockerContainerService.getContainerStatus(request.getContainerId()));
    }

    @PostMapping("/container/start")
    public ResponseEntity<String> startContainer(@RequestBody ContainerRequest request) {
        return ResponseEntity.ok(dockerContainerService.startContainer(request.getContainerId()));
    }

    @PostMapping("/container/stop")
    public ResponseEntity<String> stopContainer(@RequestBody ContainerRequest request) {
        return ResponseEntity.ok(dockerContainerService.stopContainer(request.getContainerId()));
    }

    @PostMapping("/image/create")
    public ResponseEntity<String> createImage(@RequestBody CreateImageRequest request) {
        return ResponseEntity.ok(dockerImageService.createImage(request.getContainerId(), request.getImageName()));
    }

    @PostMapping("/image")
    public ResponseEntity<DockerImage> getImageById(@RequestBody ImageRequest request){
        return ResponseEntity.ok(dockerImageService.getImageById(request.getImageId()).getBody());
    }

    @GetMapping("/images")
    public ResponseEntity<List<DockerImage>> getUserImages() {
        return ResponseEntity.ok(dockerImageService.getUserImages());
    }

    @PostMapping("/image/status")
    public ResponseEntity<?> getStatusOfImage(@RequestBody ImageRequest request) {
        return ResponseEntity.ok(dockerImageService.getImageStatus(request.getImageId()));
    }

    @DeleteMapping("/image")
    public ResponseEntity<?> deleteImage(@RequestBody ImageRequest request) {
        return ResponseEntity.ok(dockerImageService.deleteImageById(request.getImageId()));
    }
}