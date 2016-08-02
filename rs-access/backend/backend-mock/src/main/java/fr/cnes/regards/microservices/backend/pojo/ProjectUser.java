package fr.cnes.regards.microservices.backend.pojo;

import org.springframework.hateoas.ResourceSupport;

import java.util.Date;

public class ProjectUser extends ResourceSupport {
    private int status;
    private long lastConnection;
    private long lastUpdate;
    private Role role;

    public ProjectUser(String name) {
        super();
        this.lastConnection = new Date().getTime();
        this.lastUpdate = new Date().getTime();
        this.status = ProjectUserStatus.ACCESS_GRANTED.getValue();
    }
}
