package com.dockermate.dockermate.Repository;

import com.dockermate.dockermate.Entity.DockerContainer;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DockerContainerRepository extends MongoRepository<DockerContainer, String> {
}
