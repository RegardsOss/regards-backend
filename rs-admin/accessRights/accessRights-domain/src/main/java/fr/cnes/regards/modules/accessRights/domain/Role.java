/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.domain;

import java.util.List;

import org.springframework.hateoas.ResourceSupport;

public class Role extends ResourceSupport {

    private Integer roleId_;

    private String name_;

    private Role parentRole_;

    private List<ResourcesAccess> permissions_;

    private List<ProjectUser> projectUsers_;

    private boolean isDefault;

    private boolean isNative;

    public Role() {
        super();
        isDefault = false;
        isNative = false;
    }

    public Role(Integer pRoleId, String pName, Role pParentRole, List<ResourcesAccess> pPermissions,
            List<ProjectUser> pProjectUsers) {
        this();
        name_ = pName;
        parentRole_ = pParentRole;
        permissions_ = pPermissions;
        projectUsers_ = pProjectUsers;
        roleId_ = pRoleId;
    }

    public Role(Integer pRoleId, String pName, Role pParentRole, List<ResourcesAccess> pPermissions,
            List<ProjectUser> pProjectUsers, boolean pIsDefault, boolean pIsNative) {
        super();
        roleId_ = pRoleId;
        name_ = pName;
        parentRole_ = pParentRole;
        permissions_ = pPermissions;
        projectUsers_ = pProjectUsers;
        isDefault = pIsDefault;
        isNative = pIsNative;
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

    public Integer getRoleId() {
        return roleId_;
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

    public void setNative(boolean pIsNative) {
        isNative = pIsNative;
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

    public void setRoleId(Integer pRoleId) {
        roleId_ = pRoleId;
    }

}
