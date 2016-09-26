/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;

import org.springframework.hateoas.Identifiable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import fr.cnes.regards.modules.core.deserializer.LocalDateTimeDeserializer;
import fr.cnes.regards.modules.core.serializer.LocalDateTimeSerializer;

public class ProjectUser implements IProjectUser, Identifiable<Long> {

    @Min(0L)
    private static Long maxProjectUserId_ = 0L;

    @Min(0L)
    private Long id_;

    @PastOrNow
    private LocalDateTime lastConnection_;

    @PastOrNow
    private LocalDateTime lastUpdate_;

    private UserStatus status_;

    @Valid
    private List<MetaData> metaData_;

    // Can be null according to /accesses@POST (role value can be unspecified and so it's PUBLIC)
    @Valid
    private Role role_;

    @Valid
    private List<ResourcesAccess> permissions;

    @Valid
    private Account account_;

    public ProjectUser() {
        super();
        id_ = maxProjectUserId_;
        maxProjectUserId_++;
        permissions = new ArrayList<>();
        metaData_ = new ArrayList<>();
        status_ = UserStatus.WAITING_ACCES;
        lastConnection_ = LocalDateTime.now();
        lastUpdate_ = LocalDateTime.now();
    }

    /**
     * @param pAccountRequesting
     */
    public ProjectUser(Account pAccountRequesting) {
        this();
        account_ = pAccountRequesting;
    }

    public ProjectUser(Long projectUserId_, LocalDateTime lastConnection_, LocalDateTime lastUpdate_,
            UserStatus status_, List<MetaData> metaData_, Role role_, List<ResourcesAccess> permissions,
            Account account_) {
        super();
        id_ = projectUserId_;
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

    public void setId(Long pId) {
        id_ = pId;
    }

    @Override
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getLastConnection() {
        return lastConnection_;
    }

    @Override
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    public void setLastConnection(LocalDateTime pLastConnection) {
        lastConnection_ = pLastConnection;
    }

    @Override
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getLastUpdate() {
        return lastUpdate_;
    }

    @Override
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    public void setLastUpdate(LocalDateTime pLastUpdate) {
        lastUpdate_ = pLastUpdate;
        lastUpdate_ = LocalDateTime.now();
    }

    @Override
    public UserStatus getStatus() {
        return status_;
    }

    @Override
    public void setStatus(UserStatus pStatus) {
        status_ = pStatus;
        lastUpdate_ = LocalDateTime.now();
    }

    @Override
    public List<MetaData> getMetaData() {
        return metaData_;
    }

    @Override
    public void setMetaData(List<MetaData> pMetaData) {
        metaData_ = pMetaData;
        lastUpdate_ = LocalDateTime.now();
    }

    @Override
    public ProjectUser accept() {
        if (status_.equals(UserStatus.WAITING_ACCES)) {
            setStatus(UserStatus.ACCESS_GRANTED);
            return this;
        }
        throw new IllegalStateException("This request has already been treated");
    }

    /**
     * @return
     */
    @Override
    public ProjectUser deny() {
        if (status_.equals(UserStatus.WAITING_ACCES)) {
            setStatus(UserStatus.ACCES_DENIED);
            return this;
        }
        throw new IllegalStateException("This request has already been treated");
    }

    @Override
    public Account getAccount() {
        return account_;
    }

    @Override
    public void setAccount(Account pAccount) {
        account_ = pAccount;
    }

    @Override
    public Role getRole() {
        return role_;
    }

    @Override
    public void setRole(Role pRole) {
        role_ = pRole;
    }

    @Override
    public List<ResourcesAccess> getPermissions() {
        return permissions;
    }

    @Override
    public void setPermissions(List<ResourcesAccess> pPermissions) {
        permissions = pPermissions;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof ProjectUser) && (((ProjectUser) o).getId() == id_);
    }

}
