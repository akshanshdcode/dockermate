package com.dockermate.dockermate.Entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreateImageRequest {
    private String containerId;
    private String imageName;
}
