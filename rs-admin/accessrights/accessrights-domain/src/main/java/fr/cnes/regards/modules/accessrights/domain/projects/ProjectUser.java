/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.validator.PastOrNow;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.listeners.ProjectUserListener;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Domain class representing a REGARDS project user.
 *
 * @author CS
 */
@Entity
@Table(name = "t_project_user", uniqueConstraints = @UniqueConstraint(name = "uk_project_user_email", columnNames = {"email"}))
@EntityListeners(ProjectUserListener.class)
@SequenceGenerator(name = "projectUserSequence", initialValue = 1, sequenceName = "seq_project_user")
@NamedEntityGraph(name = "graph.user.metadata", attributeNodes = {@NamedAttributeNode(value = "metadata"), @NamedAttributeNode(value = "accessGroups")})
public class ProjectUser implements IIdentifiable<Long> {

    public static final String REGARDS_ORIGIN = "Regards";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "projectUserSequence")
    @Column(name = "id")
    private Long id;

    @Email
    @Column(name = "email", length = 128)
    private String email;

    @Valid
    @Length(max = 128)
    @Column(name = "firstName", length = 128)
    private String firstName;

    @Valid
    @Length(max = 128)
    @Column(name = "lastName", length = 128)
    private String lastName;

    @PastOrNow
    @Column(name = "last_connection")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastConnection;

    @PastOrNow
    @Column(name = "last_update")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastUpdate;

    @PastOrNow
    @Column(name = "created")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime created;

    @NotBlank
    @Length(max = 128)
    @Column(length = 128)
    private String origin = REGARDS_ORIGIN;

    @ElementCollection
    @Column(name = "access_group")
    @CollectionTable(
            name = "ta_project_user_access_group",
            joinColumns = @JoinColumn(name = "project_user_id", foreignKey = @ForeignKey(name = "fk_ta_project_user_access_group_t_project_user")))
    private Set<String> accessGroups;

    @Column(name = "max_quota")
    private Long maxQuota;

    @Column(name = "current_quota")
    private Long currentQuota;

    @NotNull
    @Column(name = "status", length = 30)
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Valid
    @OneToMany
    @Cascade(value = {CascadeType.ALL})
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_user_metadata"))
    private Set<MetaData> metadata;

    @Valid
    @ManyToOne
    @JoinColumn(name = "role_id", foreignKey = @ForeignKey(name = "fk_user_role"))
    private Role role;

    @Valid
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_user_permissions"))
    @GsonIgnore
    private List<ResourcesAccess> permissions;

    @Column(name = "licence_accepted")
    private boolean licenseAccepted = false;

    public ProjectUser() {
        permissions = new ArrayList<>();
        metadata = new HashSet<>();
        status = UserStatus.WAITING_ACCOUNT_ACTIVE;
    }

    public ProjectUser(String email, Role role, List<ResourcesAccess> permissions, Set<MetaData> metaData) {
        this.email = email;
        this.role = role;
        this.permissions = permissions;
        this.metadata = metaData;
        status = UserStatus.WAITING_ACCOUNT_ACTIVE;
    }

    @Override
    public Long getId() {
        return id;
    }

    public ProjectUser setId(Long id) {
        this.id = id;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public ProjectUser setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public ProjectUser setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public ProjectUser setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public OffsetDateTime getLastConnection() {
        return lastConnection;
    }

    public ProjectUser setLastConnection(OffsetDateTime lastConnection) {
        this.lastConnection = lastConnection;
        return this;
    }

    public OffsetDateTime getLastUpdate() {
        return lastUpdate;
    }

    public ProjectUser setLastUpdate(OffsetDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
        return this;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public ProjectUser setCreated(OffsetDateTime created) {
        this.created = created;
        return this;
    }

    public String getOrigin() {
        return origin;
    }

    public ProjectUser setOrigin(String origin) {
        this.origin = origin;
        return this;
    }

    public Set<String> getAccessGroups() {
        return accessGroups;
    }

    public ProjectUser setAccessGroups(Set<String> accessGroups) {
        this.accessGroups = accessGroups;
        return this;
    }

    public Long getMaxQuota() {
        return maxQuota;
    }

    public ProjectUser setMaxQuota(Long maxQuota) {
        this.maxQuota = maxQuota;
        return this;
    }

    public Long getCurrentQuota() {
        return currentQuota;
    }

    public ProjectUser setCurrentQuota(Long usedQuota) {
        this.currentQuota = usedQuota;
        return this;
    }

    public UserStatus getStatus() {
        return status;
    }

    public ProjectUser setStatus(UserStatus status) {
        this.status = status;
        return this;
    }

    public Set<MetaData> getMetadata() {
        return metadata;
    }

    public ProjectUser setMetadata(Set<MetaData> metadata) {
        this.metadata = metadata;
        return this;
    }

    public Role getRole() {
        return role;
    }

    public ProjectUser setRole(Role role) {
        this.role = role;
        return this;
    }

    public List<ResourcesAccess> getPermissions() {
        return permissions;
    }

    public ProjectUser setPermissions(List<ResourcesAccess> permissions) {
        this.permissions = permissions;
        return this;
    }

    public boolean isLicenseAccepted() {
        return licenseAccepted;
    }

    public ProjectUser setLicenseAccepted(boolean licenseAccepted) {
        this.licenseAccepted = licenseAccepted;
        return this;
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
