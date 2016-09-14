package fr.cnes.regards.modules.accessRights.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import fr.cnes.regards.modules.core.deserializer.LocalDateTimeDeserializer;
import fr.cnes.regards.modules.core.serializer.LocalDateTimeSerializer;

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
        this.lastConnection_ = LocalDateTime.now();
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

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getLastCo() {
        return lastConnection_;
    }

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    public void setLastCo(LocalDateTime pLastConnection) {
        this.lastConnection_ = pLastConnection;
    }

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getLastUpdate() {
        return lastUpdate_;
    }

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
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

    /**
     * @return
     */
    public ProjectUser deny() {
        if (this.status_.equals(UserStatus.WAITING_ACCES)) {
            this.setStatus(UserStatus.ACCES_DENIED);
            return this;
        }
        throw new IllegalStateException("This request has already been treated");
    }

}
