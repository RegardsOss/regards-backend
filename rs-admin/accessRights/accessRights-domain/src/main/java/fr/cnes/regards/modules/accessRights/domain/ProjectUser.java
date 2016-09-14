package fr.cnes.regards.modules.accessRights.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProjectUser {

    private String email_;

    private LocalDateTime lastConnection_;

    private LocalDateTime lastUpdate_;

    private UserStatus status_;

    private List<MetaData> metaDatas_;

    public ProjectUser() {
        super();
        this.metaDatas_ = new ArrayList<>();
        this.status_ = UserStatus.WAITING_ACCES;
        this.lastUpdate_ = LocalDateTime.now();
    }

    public ProjectUser(String pEmail) {
        this();
        email_ = pEmail;
    }

    public String getEmail() {
        return email_;
    }

    public void setEmail(String pEmail) {
        email_ = pEmail;
        this.lastUpdate_ = LocalDateTime.now();
    }

    public LocalDateTime getLastConnection() {
        return lastConnection_;
    }

    public void setLastConnection() {
        this.lastConnection_ = LocalDateTime.now();
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate_;
    }

    public void setLastUpdate(LocalDateTime pLastUpdate) {
        lastUpdate_ = pLastUpdate;
        this.lastUpdate_ = LocalDateTime.now();
    }

    public UserStatus getStatus() {
        return status_;
    }

    public void setStatus(UserStatus pStatus) {
        status_ = pStatus;
        this.lastUpdate_ = LocalDateTime.now();
    }

    public List<MetaData> getMetaDatas() {
        return metaDatas_;
    }

    public void setMetaDatas(List<MetaData> pMetaDatas) {
        metaDatas_ = pMetaDatas;
        this.lastUpdate_ = LocalDateTime.now();
    }

    public ProjectUser accept() {
        if (this.status_.equals(UserStatus.WAITING_ACCES)) {
            this.setStatus(UserStatus.ACCESS_GRANTED);
            return this;
        }
        throw new IllegalStateException("This request has already been treated");
    }

}
