package com.dockermate.dockermate.Repository;

import com.dockermate.dockermate.Entity.DockerImage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DockerImageRepository extends MongoRepository<DockerImage, String> {
    List<DockerImage> findByCreatedUsingContainerId(String containerId);
}