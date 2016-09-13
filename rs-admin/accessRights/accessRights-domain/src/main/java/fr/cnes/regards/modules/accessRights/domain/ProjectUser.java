package fr.cnes.regards.modules.accessRights.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ProjectUser {

    private String email_;

    @JsonIgnore
    private LocalDateTime lastConnection_;

    @JsonIgnore
    private LocalDateTime lastUpdate_;

    private UserStatus status_;

    private List<MetaData> metaDatas_;

    public ProjectUser() {
        super();
        this.lastUpdate_ = LocalDateTime.now();
        this.metaDatas_ = new ArrayList<>();
        this.status_ = UserStatus.WAITING_ACCES;
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

    public ProjectUser accept() {
        if (this.status_.equals(UserStatus.WAITING_ACCES)) {
            this.status_ = UserStatus.ACCESS_GRANTED;
            return this;
        }
        throw new IllegalStateException("This request has already been treated");
    }

}
