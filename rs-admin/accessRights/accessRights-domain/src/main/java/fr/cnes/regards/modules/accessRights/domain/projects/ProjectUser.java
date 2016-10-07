/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.domain.projects;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.validation.Valid;
import javax.validation.constraints.Min;

import org.springframework.hateoas.Identifiable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import fr.cnes.regards.modules.accessRights.domain.PastOrNow;
import fr.cnes.regards.modules.accessRights.domain.UserStatus;
import fr.cnes.regards.modules.accessRights.domain.instance.Account;
import fr.cnes.regards.modules.core.deserializer.LocalDateTimeDeserializer;
import fr.cnes.regards.modules.core.serializer.LocalDateTimeSerializer;

@Entity(name = "T_PROJECT_USER")
@SequenceGenerator(name = "projectUserSequence", initialValue = 1, sequenceName = "SEQ_PROJECT_USER")
public class ProjectUser implements Identifiable<Long> {

    // TODO : Remove this useless attribute and add the id management in stub for test
    @Transient
    @Min(0L)
    private static Long maxProjectUserId_ = 0L;

    @Min(0L)
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "projectUserSequence")
    @Column(name = "id")
    private Long id;

    @PastOrNow
    @Column(name = "lastConnection")
    private LocalDateTime lastConnection;

    @PastOrNow
    @Column(name = "lastUpdate")
    private LocalDateTime lastUpdate;

    @Column(name = "status")
    private UserStatus status;

    @Valid
    @OneToMany
    @Column(name = "metaData")
    private List<MetaData> metaData;

    // Can be null according to /accesses@POST (role value can be unspecified and so it's PUBLIC)
    @Valid
    @ManyToOne
    @JoinColumn(name = "role_id", foreignKey = @javax.persistence.ForeignKey(name = "FK_USER_ROLE"))
    private Role role;

    @Valid
    @OneToMany
    @Column(name = "permissions")
    private List<ResourcesAccess> permissions;

    // Field to get from instance database
    @Valid
    @Transient
    private Account account;

    public ProjectUser() {
        super();
        id = maxProjectUserId_;
        maxProjectUserId_++;
        permissions = new ArrayList<>();
        metaData = new ArrayList<>();
        status = UserStatus.WAITING_ACCES;
        lastConnection = LocalDateTime.now();
        lastUpdate = LocalDateTime.now();
    }

    /**
     * @param pAccountRequesting
     */
    public ProjectUser(final Account pAccountRequesting) {
        this();
        account = pAccountRequesting;
    }

    public ProjectUser(final Long pProjectUserId, final LocalDateTime pLastConnection, final LocalDateTime pLastUpdate,
            final UserStatus pStatus, final List<MetaData> pMetaData, final Role pRole,
            final List<ResourcesAccess> pPermissions,
            final Account pAccount) {
        super();
        id = pProjectUserId;
        lastConnection = pLastConnection;
        lastUpdate = pLastUpdate;
        status = pStatus;
        metaData = pMetaData;
        role = pRole;
        permissions = pPermissions;
        account = pAccount;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(final Long pId) {
        id = pId;
    }

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getLastConnection() {
        return lastConnection;
    }

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    public void setLastConnection(final LocalDateTime pLastConnection) {
        lastConnection = pLastConnection;
    }

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    public void setLastUpdate(final LocalDateTime pLastUpdate) {
        lastUpdate = pLastUpdate;
        lastUpdate = LocalDateTime.now();
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(final UserStatus pStatus) {
        status = pStatus;
        lastUpdate = LocalDateTime.now();
    }

    public List<MetaData> getMetaData() {
        return metaData;
    }

    public void setMetaData(final List<MetaData> pMetaData) {
        metaData = pMetaData;
        lastUpdate = LocalDateTime.now();
    }

    public ProjectUser accept() {
        if (status.equals(UserStatus.WAITING_ACCES)) {
            setStatus(UserStatus.ACCESS_GRANTED);
            return this;
        }
        throw new IllegalStateException("This request has already been treated");
    }

    /**
     * @return
     */

    public ProjectUser deny() {
        if (status.equals(UserStatus.WAITING_ACCES)) {
            setStatus(UserStatus.ACCESS_DENIED);
            return this;
        }
        throw new IllegalStateException("This request has already been treated");
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(final Account pAccount) {
        account = pAccount;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(final Role pRole) {
        role = pRole;
    }

    public List<ResourcesAccess> getPermissions() {
        return permissions;
    }

    public void setPermissions(final List<ResourcesAccess> pPermissions) {
        permissions = pPermissions;
    }

    @Override
    public boolean equals(final Object o) {
        return (o instanceof ProjectUser) && (((ProjectUser) o).getId() == id);
    }

}
