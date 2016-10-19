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

import org.hibernate.validator.constraints.Email;
import org.springframework.hateoas.Identifiable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import fr.cnes.regards.modules.accessRights.domain.PastOrNow;
import fr.cnes.regards.modules.accessRights.domain.UserStatus;
import fr.cnes.regards.modules.core.deserializer.LocalDateTimeDeserializer;
import fr.cnes.regards.modules.core.serializer.LocalDateTimeSerializer;

/**
 * Domain class representing a REGARDS project user.
 *
 * @author CS
 */
@Entity(name = "T_PROJECT_USER")
@SequenceGenerator(name = "projectUserSequence", initialValue = 1, sequenceName = "SEQ_PROJECT_USER")
public class ProjectUser implements Identifiable<Long> {

    /**
     * Temporary value to automatically manage ids
     */
    // TODO : Remove this useless attribute and add the id management in stub for test
    @Transient
    @Min(0L)
    private static Long maxProjectUserId_ = 0L;

    /**
     * Te id
     */
    @Min(0L)
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "projectUserSequence")
    @Column(name = "id")
    private Long id;

    /**
     * The user's email. Used to associate the Project User to its Account.
     */
    @Email
    @Column(name = "email")
    private String email;

    /**
     * The last connection date
     */
    @PastOrNow
    @Column(name = "lastConnection")
    private LocalDateTime lastConnection;

    /**
     * The last update date
     */
    @PastOrNow
    @Column(name = "lastUpdate")
    private LocalDateTime lastUpdate;

    /**
     * The status of the user
     */
    @Column(name = "status")
    private UserStatus status;

    /**
     * The list of meta data on the user
     */
    @Valid
    @OneToMany
    @Column(name = "metaData")
    private List<MetaData> metaData;

    /**
     * The user's role. Can be null according to /accesses@POST (role value can be unspecified and so it's PUBLIC)
     */
    @Valid
    @ManyToOne
    @JoinColumn(name = "role_id", foreignKey = @javax.persistence.ForeignKey(name = "FK_USER_ROLE"))
    private Role role;

    /**
     * The list of specific permissions for this user, augmenting the permissions granted by the role.
     */
    @Valid
    @OneToMany
    @Column(name = "permissions")
    private List<ResourcesAccess> permissions;

    /**
     * Create a new {@link ProjectUser} with empty values.
     */
    public ProjectUser() {
        super();
        id = maxProjectUserId_;
        maxProjectUserId_++;
        permissions = new ArrayList<>();
        metaData = new ArrayList<>();
        status = UserStatus.WAITING_ACCESS;
        lastConnection = LocalDateTime.now();
        lastUpdate = LocalDateTime.now();
    }

    /**
     * Create a new {@link ProjectUser} with specific values.
     *
     * @param pId
     *            The id
     * @param pLastConnection
     *            The last connection date
     * @param pLastUpdate
     *            The last update date
     * @param pStatus
     *            The status
     * @param pMetaData
     *            The list of meta data
     * @param pRole
     *            The role
     * @param pPermissions
     *            The list of permissions
     * @param pEmail
     *            The email
     */
    public ProjectUser(final Long pId, final LocalDateTime pLastConnection, final LocalDateTime pLastUpdate,
            final UserStatus pStatus, final List<MetaData> pMetaData, final Role pRole,
            final List<ResourcesAccess> pPermissions, final String pEmail) {
        super();
        id = pId;
        lastConnection = pLastConnection;
        lastUpdate = pLastUpdate;
        status = pStatus;
        metaData = pMetaData;
        role = pRole;
        permissions = pPermissions;
        email = pEmail;
    }

    /**
     * Get <code>id</code>
     */
    @Override
    public Long getId() {
        return id;
    }

    /**
     * Set <code>id</code>
     *
     * @param pId
     *            The id
     */
    public void setId(final Long pId) {
        id = pId;
    }

    /**
     * Get <code>lastConnection</code>
     *
     * @return The last connection date
     */
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getLastConnection() {
        return lastConnection;
    }

    /**
     * Set <code>lastConnection</code>
     *
     * @param pLastConnection
     *            The last connection date
     */
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    public void setLastConnection(final LocalDateTime pLastConnection) {
        lastConnection = pLastConnection;
    }

    /**
     * Get <code>lastUpdate</code>
     *
     * @return The last update date
     */
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    /**
     * Set <code>lastUpdate</code>
     *
     * @param pLastUpdate
     *            The last update date
     */
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    public void setLastUpdate(final LocalDateTime pLastUpdate) {
        lastUpdate = pLastUpdate;
        lastUpdate = LocalDateTime.now();
    }

    /**
     * Get <code>status</code>
     *
     * @return The status
     */
    public UserStatus getStatus() {
        return status;
    }

    /**
     * Set <code>status</code>
     *
     * @param pStatus
     *            The status
     */
    public void setStatus(final UserStatus pStatus) {
        status = pStatus;
        lastUpdate = LocalDateTime.now();
    }

    /**
     * Get <code>metaData</code>
     *
     * @return The {@link List} of {@link MetaData}
     */
    public List<MetaData> getMetaData() {
        return metaData;
    }

    /**
     * Set <code>metaData</code>
     *
     * @param pMetaData
     *            {@link List} of {@link MetaData}
     */
    public void setMetaData(final List<MetaData> pMetaData) {
        metaData = pMetaData;
        lastUpdate = LocalDateTime.now();
    }

    /**
     * TODO move this to service!!
     *
     * @return The accepted user
     */
    public ProjectUser accept() {
        if (status.equals(UserStatus.WAITING_ACCESS)) {
            setStatus(UserStatus.ACCESS_GRANTED);
            return this;
        }
        throw new IllegalStateException("This request has already been treated (accepted)");
    }

    /**
     * TODO move this to service!!
     *
     * @return The denied user
     */
    public ProjectUser deny() {
        if (status.equals(UserStatus.WAITING_ACCESS)) {
            setStatus(UserStatus.ACCESS_DENIED);
            return this;
        }
        throw new IllegalStateException("This request has already been treated (denied)");
    }

    /**
     * Get <code>role</code>
     *
     * @return The role
     */
    public Role getRole() {
        return role;
    }

    /**
     * Set <code>role</code>
     *
     * @param pRole
     *            The role
     */
    public void setRole(final Role pRole) {
        role = pRole;
    }

    /**
     * Get <code>permissions</code>
     *
     * @return The {@link List} of {@link ResourcesAccess}
     */
    public List<ResourcesAccess> getPermissions() {
        return permissions;
    }

    /**
     * Set <code>permissions</code>
     *
     * @param pPermissions
     *            The {@link List} of {@link ResourcesAccess}
     */
    public void setPermissions(final List<ResourcesAccess> pPermissions) {
        permissions = pPermissions;
    }

    /**
     * Set <code>email</code>
     *
     * @return The project user's email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Set the project user's <code>email</code>.
     *
     * @param pEmail
     *            Must be a valid email format.
     */
    public void setEmail(final String pEmail) {
        email = pEmail;
    }

    @Override
    public boolean equals(final Object pObject) {
        return (pObject instanceof ProjectUser) && (((ProjectUser) pObject).getId() == id);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 37;
        int result = 1;

        result = (prime * result);
        if (id != null) {
            result += id.hashCode();
        }

        return result;
    }

}
