package fr.cnes.regards.modules.users.domain;

import java.time.LocalDateTime;
import java.util.List;

import fr.cnes.regards.modules.project.domain.Project;

public class ProjectUser {

    private LocalDateTime lastConnection_;

    private LocalDateTime lastUpdate_;

    private UserStatus status_;

    private List<MetaData> metaDatas_;

    private Project project_;

    public ProjectUser() {
        super();
    }

    public LocalDateTime getLastConnection() {
        return lastConnection_;
    }

    public void setLastConnection(LocalDateTime pLastConnection) {
        lastConnection_ = pLastConnection;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate_;
    }

    public void setLastUpdate(LocalDateTime pLastUpdate) {
        lastUpdate_ = pLastUpdate;
    }

    public UserStatus getStatus() {
        return status_;
    }

    public void setStatus(UserStatus pStatus) {
        status_ = pStatus;
    }

    public List<MetaData> getMetaDatas() {
        return metaDatas_;
    }

    public void setMetaDatas(List<MetaData> pMetaDatas) {
        metaDatas_ = pMetaDatas;
    }

    public Project getProject() {
        return project_;
    }

    public void setProject(Project pProject) {
        project_ = pProject;
    }

}
