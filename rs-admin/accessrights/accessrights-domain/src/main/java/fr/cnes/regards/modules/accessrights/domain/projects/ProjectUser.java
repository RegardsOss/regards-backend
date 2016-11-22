/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Email;

import com.fasterxml.jackson.annotation.JsonBackReference;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.validator.PastOrNow;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.listeners.ProjectUserListener;

/**
 * Domain class representing a REGARDS project user.
 *
 * @author CS
 */
@Entity(name = "T_PROJECT_USER")
@EntityListeners(ProjectUserListener.class)
@SequenceGenerator(name = "projectUserSequence", initialValue = 1, sequenceName = "SEQ_PROJECT_USER")
public class ProjectUser implements IIdentifiable<Long> {

    /**
     * The id
     */
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
    @NotNull
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    /**
     * The list of meta data on the user
     */
    @Valid
    @OneToMany(fetch = FetchType.EAGER)
    @Column(name = "metaData")
    private List<MetaData> metaData;

    /**
     * The user's role.
     */
    @Valid
    @ManyToOne
    @JoinColumn(name = "role_id", foreignKey = @ForeignKey(name = "FK_USER_ROLE"))
    @JsonBackReference
    private Role role;

    /**
     * The list of specific permissions for this user, augmenting the permissions granted by the role.
     */
    @Valid
    @OneToMany(fetch = FetchType.EAGER)
    @Column(name = "permissions")
    private List<ResourcesAccess> permissions;

    /**
     * Create a new {@link ProjectUser} with empty values.
     */
    public ProjectUser() {
        super();
        permissions = new ArrayList<>();
        metaData = new ArrayList<>();
        status = UserStatus.WAITING_ACCESS;
    }

    /**
     * Creates a new {@link ProjectUser}
     *
     * @param pEmail
     *            The email
     * @param pRole
     *            The role
     * @param pPermissions
     *            The list of permissions
     * @param pMetaData
     *            The list of meta data
     */
    public ProjectUser(final String pEmail, final Role pRole, final List<ResourcesAccess> pPermissions,
            final List<MetaData> pMetaData) {
        email = pEmail;
        role = pRole;
        permissions = pPermissions;
        metaData = pMetaData;
        status = UserStatus.WAITING_ACCESS;
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
    public LocalDateTime getLastConnection() {
        return lastConnection;
    }

    /**
     * Set <code>lastConnection</code>
     *
     * @param pLastConnection
     *            The last connection date
     */

    public void setLastConnection(final LocalDateTime pLastConnection) {
        lastConnection = pLastConnection;
    }

    /**
     * Get <code>lastUpdate</code>
     *
     * @return The last update date
     */
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    /**
     * Set <code>lastUpdate</code>
     *
     * @param pLastUpdate
     *            The last update date
     */
    public void setLastUpdate(final LocalDateTime pLastUpdate) {
        lastUpdate = pLastUpdate;
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

}
