/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.domain;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;
import org.springframework.hateoas.Identifiable;

public class Role implements Identifiable<Long> {

    @NotNull
    private Long id_;

    @NotBlank
    private String name_;

    @NotNull
    @Valid
    // TODO: Create a specific constraint => only role PUBLIC can have no parent
    private Role parentRole_;

    @Valid
    private List<ResourcesAccess> permissions_;

    @Valid
    private List<ProjectUser> projectUsers_;

    private boolean isDefault;

    private boolean isNative;

    public void setNative(boolean pIsNative) {
        isNative = pIsNative;
    }

    public Role() {
        super();
        isDefault = false;
        isNative = false;
    }

    public Role(Long pRoleId) {
        this();
        id_ = pRoleId;
    }

    public Role(Long pRoleId, String pName, Role pParentRole, List<ResourcesAccess> pPermissions,
            List<ProjectUser> pProjectUsers) {
        this(pRoleId);
        name_ = pName;
        parentRole_ = pParentRole;
        permissions_ = pPermissions;
        projectUsers_ = pProjectUsers;
    }

    public Role(Long pRoleId, String pName, Role pParentRole, List<ResourcesAccess> pPermissions,
            List<ProjectUser> pProjectUsers, boolean pIsDefault, boolean pIsNative) {
        super();
        id_ = pRoleId;
        name_ = pName;
        parentRole_ = pParentRole;
        permissions_ = pPermissions;
        projectUsers_ = pProjectUsers;
        isDefault = pIsDefault;
        isNative = pIsNative;
    }

    @Override
    public Long getId() {
        return id_;
    }

    public void setId(Long pId) {
        id_ = pId;
    }

    public String getName() {
        return name_;
    }

    public Role getParentRole() {
        return parentRole_;
    }

    public List<ResourcesAccess> getPermissions() {
        return permissions_;
    }

    public List<ProjectUser> getProjectUsers() {
        return projectUsers_;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isNative() {
        return isNative;
    }

    public void setDefault(boolean pIsDefault) {
        isDefault = pIsDefault;
    }

    public void setName(String pName) {
        name_ = pName;
    }

    public void setParentRole(Role pParentRole) {
        parentRole_ = pParentRole;
    }

    public void setPermissions(List<ResourcesAccess> pPermissions) {
        permissions_ = pPermissions;
    }

    public void setProjectUsers(List<ProjectUser> pProjectUsers) {
        projectUsers_ = pProjectUsers;
    }

    @Override
    public boolean equals(Object pObj) {
        return (pObj instanceof Role) && ((Role) pObj).getId().equals(id_);
    }

    @Override
    public int hashCode() {
        return (int) (long) id_;
    }

}
