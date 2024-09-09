package com.dockermate.dockermate.Entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CommandRequest {
    private String containerId;
    private String command;
}
