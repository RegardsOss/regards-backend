package fr.cnes.regards.modules.accessRights.domain;

import java.time.LocalDateTime;
import java.util.List;

/*
 * LICENSE_PLACEHOLDER
 */
public class ProjectUser {

    private String email_;

    private LocalDateTime lastConnection_;

    private LocalDateTime lastUpdate_;

    private UserStatus status_;

    private List<MetaData> metaDatas_;

    public ProjectUser() {
        super();
    }

    public ProjectUser(String pEmail) {
        super();
        email_ = pEmail;
    }

    public String getEmail() {
        return email_;
    }

    public void setEmail(String pEmail) {
        email_ = pEmail;
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

}
