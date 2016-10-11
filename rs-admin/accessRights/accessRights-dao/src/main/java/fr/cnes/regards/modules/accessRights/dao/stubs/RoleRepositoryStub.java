/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao.stubs;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.accessRights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessRights.domain.HttpVerb;
import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.projects.Role;

@Repository
@Profile("test")
@Primary
public class RoleRepositoryStub extends RepositoryStub<Role> implements IRoleRepository {

    public RoleRepositoryStub() {
        final List<ResourcesAccess> permissionList_ = new ArrayList<ResourcesAccess>();
        permissionList_.add(new ResourcesAccess(0L, "ra 0", "Microservice 0", "Resource 0", HttpVerb.GET));
        permissionList_.add(new ResourcesAccess(1L, "ra 1", "Microservice 1", "Resource 1", HttpVerb.PUT));
        permissionList_.add(new ResourcesAccess(2L, "ra 2", "Microservice 2", "Resource 2", HttpVerb.DELETE));
        permissionList_.add(new ResourcesAccess(3L, "ra 3", "Microservice 3", "Resource 3", HttpVerb.GET));

        final List<ProjectUser> projectUsers_ = new ArrayList<>();
        projectUsers_.add(new ProjectUser());
        projectUsers_.add(new ProjectUser());
        projectUsers_.add(new ProjectUser());
        projectUsers_.add(new ProjectUser());
        projectUsers_.add(new ProjectUser());

        // Init default roles
        final Role rolePublic = new Role(0L, "Public", null, permissionList_, projectUsers_.subList(0, 2), true, true);
        final Role roleRegisteredUser = new Role(1L, "Registered User", rolePublic, null, projectUsers_.subList(2, 4),
                false, true);
        final Role roleAdmin = new Role(2L, "Admin", roleRegisteredUser, permissionList_.subList(1, 2),
                projectUsers_.subList(1, 2), false, true);
        final Role roleProjectAdmin = new Role(3L, "Project Admin", roleAdmin, permissionList_.subList(1, 3),
                projectUsers_.subList(1, 4), false, true);
        final Role roleInstanceAdmin = new Role(4L, "Instance Admin", roleProjectAdmin, new ArrayList<>(),
                projectUsers_.subList(0, 2), false, true);
        entities.add(rolePublic);
        entities.add(roleRegisteredUser);
        entities.add(roleAdmin);
        entities.add(roleProjectAdmin);
        entities.add(roleInstanceAdmin);

        // Init some custom roles
        final Role role5 = new Role(5L, "Role 5", rolePublic, permissionList_.subList(1, 2),
                projectUsers_.subList(0, 1));
        final Role role6 = new Role(6L, "Role 6", rolePublic, permissionList_.subList(0, 2),
                projectUsers_.subList(1, 2));
        final Role role7 = new Role(7L, "Role 7", rolePublic, permissionList_.subList(1, 3),
                projectUsers_.subList(0, 2));
        entities.add(role5);
        entities.add(role6);
        entities.add(role7);
    }

    @Override
    public Role findByIsDefault(final boolean pIsDefault) {
        return entities.stream().filter(r -> r.isDefault() == pIsDefault).findFirst().get();
    }

    @Override
    public Role findOneByName(final String pBorrowedRoleName) {
        return entities.stream().filter(r -> r.getName().equals(pBorrowedRoleName)).findFirst().get();
    }

}
