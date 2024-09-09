package com.dockermate.dockermate.Entity;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "docker_images")
@Setter
@Getter
public class DockerImage {

    @Id
    private String imageId;
    @NonNull
    private String imageName;
    @NonNull
    private String status;
    @NonNull
    private String createdAt;
    private String createdUsingContainerId;
}