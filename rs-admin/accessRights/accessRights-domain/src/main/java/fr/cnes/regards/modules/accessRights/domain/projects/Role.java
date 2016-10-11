/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.domain.projects;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.hateoas.Identifiable;

/**
 * Models a user's role.
 *
 * @author Xaver-Alexandre Brochard
 */
@Entity(name = "T_ROLE")
@SequenceGenerator(name = "roleSequence", initialValue = 1, sequenceName = "SEQ_ROLE")
public class Role implements Identifiable<Long> {

    @NotNull
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "roleSequence")
    @Column(name = "id")
    private Long id;

    @NotBlank
    @Column(name = "name")
    private String name;

    // TODO: Create a specific constraint => only role PUBLIC can have no parent
    @OneToOne
    private Role parentRole;

    @Valid
    @OneToMany
    @Column(name = "permissions")
    private List<ResourcesAccess> permissions;

    @Valid
    @OneToMany
    @Column(name = "projectUsers")
    private List<ProjectUser> projectUsers;

    @Column(name = "default")
    private boolean isDefault;

    @Column(name = "native")
    private boolean isNative;

    public void setNative(final boolean pIsNative) {
        isNative = pIsNative;
    }

    public Role() {
        super();
        isDefault = false;
        isNative = false;
    }

    public Role(final Long pRoleId) {
        this();
        id = pRoleId;
    }

    public Role(final Long pRoleId, final String pName, final Role pParentRole,
            final List<ResourcesAccess> pPermissions, final List<ProjectUser> pProjectUsers) {
        this(pRoleId);
        name = pName;
        parentRole = pParentRole;
        permissions = pPermissions;
        projectUsers = pProjectUsers;
    }

    public Role(final Long pRoleId, final String pName, final Role pParentRole,
            final List<ResourcesAccess> pPermissions, final List<ProjectUser> pProjectUsers, final boolean pIsDefault,
            final boolean pIsNative) {
        super();
        id = pRoleId;
        name = pName;
        parentRole = pParentRole;
        permissions = pPermissions;
        projectUsers = pProjectUsers;
        isDefault = pIsDefault;
        isNative = pIsNative;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(final Long pId) {
        id = pId;
    }

    public String getName() {
        return name;
    }

    public Role getParentRole() {
        return parentRole;
    }

    public List<ResourcesAccess> getPermissions() {
        return permissions;
    }

    public List<ProjectUser> getProjectUsers() {
        return projectUsers;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isNative() {
        return isNative;
    }

    public void setDefault(final boolean pIsDefault) {
        isDefault = pIsDefault;
    }

    public void setName(final String pName) {
        name = pName;
    }

    public void setParentRole(final Role pParentRole) {
        parentRole = pParentRole;
    }

    public void setPermissions(final List<ResourcesAccess> pPermissions) {
        permissions = pPermissions;
    }

    public void setProjectUsers(final List<ProjectUser> pProjectUsers) {
        projectUsers = pProjectUsers;
    }

    @Override
    public boolean equals(final Object pObj) {
        return (pObj instanceof Role) && ((Role) pObj).getId().equals(id);
    }

    @Override
    public int hashCode() {
        return (int) (long) id;
    }

}
