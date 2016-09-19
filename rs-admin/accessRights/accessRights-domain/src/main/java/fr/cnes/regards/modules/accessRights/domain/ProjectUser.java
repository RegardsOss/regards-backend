/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;

import org.springframework.hateoas.Identifiable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import fr.cnes.regards.modules.core.deserializer.LocalDateTimeDeserializer;
import fr.cnes.regards.modules.core.serializer.LocalDateTimeSerializer;

public class ProjectUser implements Identifiable<Long> {

    @Min(0)
    private static Long maxProjectUserId_ = 0L;

    @Min(0)
    private final Long id_;

    @Past
    private LocalDateTime lastConnection_;

    @Past
    private LocalDateTime lastUpdate_;

    @NotNull
    private UserStatus status_;

    @Valid
    private List<MetaData> metaData_;

    @NotNull
    @Valid
    private Role role_;

    private List<ResourcesAccess> permissions;

    @NotNull
    @Valid
    private Account account_;

    public ProjectUser() {
        super();
        this.id_ = maxProjectUserId_;
        maxProjectUserId_++;
        this.permissions = new ArrayList<>();
        this.metaData_ = new ArrayList<>();
        this.status_ = UserStatus.WAITING_ACCES;
        this.lastConnection_ = LocalDateTime.now();
        this.lastUpdate_ = LocalDateTime.now();
    }

    /**
     * @param pAccountRequesting
     */
    public ProjectUser(Account pAccountRequesting) {
        this();
        this.account_ = pAccountRequesting;
    }

    public ProjectUser(Long projectUserId_, LocalDateTime lastConnection_, LocalDateTime lastUpdate_,
            UserStatus status_, List<MetaData> metaData_, Role role_, List<ResourcesAccess> permissions,
            Account account_) {
        super();
        this.id_ = projectUserId_;
        this.lastConnection_ = lastConnection_;
        this.lastUpdate_ = lastUpdate_;
        this.status_ = status_;
        this.metaData_ = metaData_;
        this.role_ = role_;
        this.permissions = permissions;
        this.account_ = account_;
    }

    @Override
    public Long getId() {
        return id_;
    }

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getLastConnection() {
        return lastConnection_;
    }

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    public void setLastConnection(LocalDateTime pLastConnection) {
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

    public List<MetaData> getMetaData() {
        return metaData_;
    }

    public void setMetaData(List<MetaData> pMetaData) {
        metaData_ = pMetaData;
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

    public Account getAccount() {
        return account_;
    }

    public void setAccount(Account pAccount) {
        account_ = pAccount;
    }

    public Role getRole() {
        return role_;
    }

    public void setRole(Role pRole) {
        role_ = pRole;
    }

    public List<ResourcesAccess> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<ResourcesAccess> pPermissions) {
        permissions = pPermissions;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof ProjectUser) && (((ProjectUser) o).getId() == id_);
    }

}
