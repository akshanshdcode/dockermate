package com.dockermate.dockermate.Entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "docker_containers")
@Getter
@Setter
public class DockerContainer {

    @Id
    @NonNull
    private String containerId;
    @NonNull
    private String containerName;
    @NonNull
    private String imageName;
    @NonNull
    private String createdAt;
    @NonNull
    private String status;
}
