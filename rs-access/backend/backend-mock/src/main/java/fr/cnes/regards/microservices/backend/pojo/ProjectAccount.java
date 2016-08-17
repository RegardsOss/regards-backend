package fr.cnes.regards.microservices.backend.pojo;

import org.springframework.hateoas.ResourceSupport;

import java.util.Date;

public class ProjectAccount extends ResourceSupport {
    private long projectAccountId;
    private int status;
    private long lastConnection;
    private long lastUpdate;
    private Role role;
    private Project project;

    public ProjectAccount() {
        super();
        this.lastConnection = new Date().getTime();
        this.lastUpdate = new Date().getTime();
        this.status = ProjectUserStatus.ACCESS_GRANTED.getValue();
        this.role = new Role("Guest");
    }

    public long getProjectAccountId() {
        return projectAccountId;
    }

    public void setProjectAccountId(long projectAccountId) {
        this.projectAccountId = projectAccountId;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getLastConnection() {
        return lastConnection;
    }

    public void setLastConnection(long lastConnection) {
        this.lastConnection = lastConnection;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
