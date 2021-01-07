/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
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
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.validator.PastOrNow;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.listeners.ProjectUserListener;

/**
 * Domain class representing a REGARDS project user.
 *
 * @author CS
 */
@Entity
@Table(name = "t_project_user",
        uniqueConstraints = @UniqueConstraint(name = "uk_project_user_email", columnNames = { "email" }))
@EntityListeners(ProjectUserListener.class)
@SequenceGenerator(name = "projectUserSequence", initialValue = 1, sequenceName = "seq_project_user")
@NamedEntityGraph(name = "graph.user.metadata", attributeNodes = @NamedAttributeNode(value = "metadata"))
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
    @Column(name = "email", length = 128)
    private String email;

    /**
     * The last connection date
     */
    @PastOrNow
    @Column(name = "last_connection")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastConnection;

    /**
     * The last update date
     */
    @PastOrNow
    @Column(name = "last_update")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastUpdate;

    /**
     * The status of the user
     */
    @NotNull
    @Column(name = "status", length = 30)
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    /**
     * The list of meta data on the user
     */
    @Valid
    @OneToMany
    @Cascade(value = { CascadeType.ALL })
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_user_metadata"))
    private List<MetaData> metadata;

    /**
     * The user's role.
     */
    @Valid
    @ManyToOne
    @JoinColumn(name = "role_id", foreignKey = @ForeignKey(name = "fk_user_role"))
    private Role role;

    /**
     * The list of specific permissions for this user, augmenting the permissions granted by the role.
     */
    @Valid
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_user_permissions"))
    @GsonIgnore
    private List<ResourcesAccess> permissions;

    /**
     * whether the project user accepted the licence or not.
     */
    @Column(name = "licence_accepted")
    private boolean licenseAccepted = false;

    /**
     * Create a new {@link ProjectUser} with empty values.
     */
    public ProjectUser() {
        super();
        permissions = new ArrayList<>();
        metadata = new ArrayList<>();
        status = UserStatus.WAITING_ACCOUNT_ACTIVE;
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
        metadata = pMetaData;
        status = UserStatus.WAITING_ACCOUNT_ACTIVE;
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
    public OffsetDateTime getLastConnection() {
        return lastConnection;
    }

    /**
     * Set <code>lastConnection</code>
     *
     * @param pLastConnection
     *            The last connection date
     */

    public void setLastConnection(final OffsetDateTime pLastConnection) {
        lastConnection = pLastConnection;
    }

    /**
     * Get <code>lastUpdate</code>
     *
     * @return The last update date
     */
    public OffsetDateTime getLastUpdate() {
        return lastUpdate;
    }

    /**
     * Set <code>lastUpdate</code>
     *
     * @param pLastUpdate
     *            The last update date
     */
    public void setLastUpdate(final OffsetDateTime pLastUpdate) {
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
     * Get <code>metadata</code>
     *
     * @return The {@link List} of {@link MetaData}
     */
    public List<MetaData> getMetadata() {
        return metadata;
    }

    /**
     * Set <code>metadata</code>
     *
     * @param pMetaData
     *            {@link List} of {@link MetaData}
     */
    public void setMetadata(final List<MetaData> pMetaData) {
        metadata = pMetaData;
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

    /**
     * @return whether the license is accepted or not
     */
    public boolean isLicenseAccepted() {
        return licenseAccepted;
    }

    public void setLicenseAccepted(final boolean pLicenceAccepted) {
        licenseAccepted = pLicenceAccepted;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((email == null) ? 0 : email.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProjectUser other = (ProjectUser) obj;
        if (email == null) {
            return other.email == null;
        } else {
            return email.equals(other.email);
        }
    }

    @Override
    public String toString() {
        return "ProjectUser [id=" + id + ", email=" + email + ", lastConnection=" + lastConnection + ", lastUpdate="
                + lastUpdate + ", status=" + status + ", licenseAccepted=" + licenseAccepted + "]";
    }

}
