package fr.cnes.regards.modules.accessRights.domain;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.ResourceSupport;

/*
 * LICENSE_PLACEHOLDER
 */
public class Role extends ResourceSupport {

    private Integer roleId_;

    private String name_;

    private Role parentRole_;

    private List<ResourcesAccess> permissions_;

    private List<ProjectUser> projectUsers_;

    public Role() {
        super();
        permissions_ = new ArrayList<>();
        projectUsers_ = new ArrayList<>();

    }

    public Role(Integer pRoleId, String pName, Role pParentRole, List<ResourcesAccess> pPermissions,
            List<ProjectUser> pProjectUsers) {
        super();
        name_ = pName;
        parentRole_ = pParentRole;
        permissions_ = pPermissions;
        projectUsers_ = pProjectUsers;
        roleId_ = pRoleId;
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

    public void setRoleId(Integer pRoleId) {
        roleId_ = pRoleId;
    }

}
