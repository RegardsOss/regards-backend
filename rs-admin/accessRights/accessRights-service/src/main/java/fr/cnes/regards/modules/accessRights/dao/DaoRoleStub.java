package fr.cnes.regards.modules.accessRights.dao;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;

/*
 * LICENSE_PLACEHOLDER
 */
@Repository
public class DaoRoleStub implements IDaoRole {

    private List<ResourcesAccess> permissionList_;

    private List<ProjectUser> projectUsers_;

    private List<Role> roles_;

    private Role rolePublic;

    private Role roleRegisteredUser;

    private Role roleAdmin;

    private Role roleProjectAdmin;

    private Role roleInstanceAdmin;

    @Autowired
    private IDaoResourcesAccess daoResourcesAccess_;

    @Autowired
    private IDaoProjectUser daoProjectUser_;

    @PostConstruct
    public void init() {
        permissionList_ = daoResourcesAccess_.getAll();
        projectUsers_ = daoProjectUser_.getAll();

        roles_ = initDefaultRoles();
        roles_.addAll(initCustomRoles());
    }

    @Override
    public Role getById(Integer pRoleId) {
        return roles_.stream().filter(r -> r.getRoleId().equals(pRoleId)).findFirst().get();
    }

    @Override
    public List<Role> getAll() {
        return roles_;
    }

    public List<Role> initDefaultRoles() {
        List<Role> defaultRoles = new ArrayList<>();

        rolePublic = new Role(0, "Public", null, null, projectUsers_.subList(8, 10), true, true);
        roleRegisteredUser = new Role(1, "Registered User", rolePublic, null, projectUsers_.subList(5, 8), false, true);
        roleAdmin = new Role(2, "Admin", roleRegisteredUser, null, projectUsers_.subList(3, 5), false, true);
        roleProjectAdmin = new Role(3, "Project Admin", roleAdmin, null, projectUsers_.subList(1, 3), false, true);
        roleInstanceAdmin = new Role(4, "Instance Admin", roleProjectAdmin, permissionList_,
                projectUsers_.subList(0, 1), false, true);

        defaultRoles.add(rolePublic);
        defaultRoles.add(roleRegisteredUser);
        defaultRoles.add(roleAdmin);
        defaultRoles.add(roleProjectAdmin);
        defaultRoles.add(roleInstanceAdmin);

        return defaultRoles;
    }

    public List<Role> initCustomRoles() {
        List<Role> customRoles = new ArrayList<>();

        Role role0 = new Role(5, "Role 0", rolePublic, permissionList_.subList(1, 2), projectUsers_.subList(0, 1));
        Role role1 = new Role(6, "Role 1", rolePublic, permissionList_.subList(0, 2), projectUsers_.subList(1, 2));
        Role role2 = new Role(7, "Role 2", rolePublic, permissionList_.subList(1, 3), projectUsers_.subList(0, 2));

        customRoles.add(role0);
        customRoles.add(role1);
        customRoles.add(role2);

        return customRoles;
    }

}
