/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao.stubs;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.accessRights.dao.IRoleRepository;
import fr.cnes.regards.modules.accessRights.domain.HttpVerb;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;

@Repository
@Profile("test")
@Primary
public class RoleRepositoryStub extends RepositoryStub<Role> implements IRoleRepository {

    public RoleRepositoryStub() {

        List<ResourcesAccess> permissionList_ = new ArrayList<ResourcesAccess>();
        permissionList_.add(new ResourcesAccess(0L, "ra 0", "Microservice 0", "Resource 0", HttpVerb.GET));
        permissionList_.add(new ResourcesAccess(1L, "ra 1", "Microservice 1", "Resource 1", HttpVerb.PUT));
        permissionList_.add(new ResourcesAccess(2L, "ra 2", "Microservice 2", "Resource 2", HttpVerb.DELETE));
        permissionList_.add(new ResourcesAccess(3L, "ra 3", "Microservice 3", "Resource 3", HttpVerb.GET));

        List<ProjectUser> projectUsers_ = new ArrayList<>();
        projectUsers_.add(new ProjectUser());
        projectUsers_.add(new ProjectUser());
        projectUsers_.add(new ProjectUser());
        projectUsers_.add(new ProjectUser());
        projectUsers_.add(new ProjectUser());

        // Init default roles
        Role rolePublic = new Role(0L, "Public", null, permissionList_, projectUsers_.subList(0, 2), true, true);
        Role roleRegisteredUser = new Role(1L, "Registered User", rolePublic, null, projectUsers_.subList(2, 4), false,
                true);
        Role roleAdmin = new Role(2L, "Admin", roleRegisteredUser, permissionList_.subList(1, 2),
                projectUsers_.subList(1, 2), false, true);
        Role roleProjectAdmin = new Role(3L, "Project Admin", roleAdmin, permissionList_.subList(1, 3),
                projectUsers_.subList(1, 4), false, true);
        Role roleInstanceAdmin = new Role(4L, "Instance Admin", roleProjectAdmin, new ArrayList<>(),
                projectUsers_.subList(0, 2), false, true);
        entities_.add(rolePublic);
        entities_.add(roleRegisteredUser);
        entities_.add(roleAdmin);
        entities_.add(roleProjectAdmin);
        entities_.add(roleInstanceAdmin);

        // Init some custom roles
        Role role0 = new Role(5L, "Role 0", rolePublic, permissionList_.subList(1, 2), projectUsers_.subList(0, 1));
        Role role1 = new Role(6L, "Role 1", rolePublic, permissionList_.subList(0, 2), projectUsers_.subList(1, 2));
        Role role2 = new Role(7L, "Role 2", rolePublic, permissionList_.subList(1, 3), projectUsers_.subList(0, 2));
        entities_.add(role0);
        entities_.add(role1);
        entities_.add(role2);
    }

    @Override
    public Role findByIsDefault(boolean pIsDefault) {
        return entities_.stream().filter(r -> r.isDefault() == pIsDefault).findFirst().get();
    }

    @Override
    public Role findOneByName(String pBorrowedRoleName) {
        return entities_.stream().filter(r -> r.getName().equals(pBorrowedRoleName)).findFirst().get();
    }

}
