/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao.stubs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.test.repository.JpaRepositoryStub;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
import fr.cnes.regards.modules.accessrights.domain.projects.DefaultRoleNames;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

@Repository
@Profile("test")
@Primary
public class RoleRepositoryStub extends JpaRepositoryStub<Role> implements IRoleRepository {

    public RoleRepositoryStub() {
        final List<ResourcesAccess> permissionList = new ArrayList<ResourcesAccess>();
        permissionList.add(new ResourcesAccess(0L, "ra 0", "Microservice 0", "Resource 0", HttpVerb.GET));
        permissionList.add(new ResourcesAccess(1L, "ra 1", "Microservice 1", "Resource 1", HttpVerb.PUT));
        permissionList.add(new ResourcesAccess(2L, "ra 2", "Microservice 2", "Resource 2", HttpVerb.DELETE));
        permissionList.add(new ResourcesAccess(3L, "ra 3", "Microservice 3", "Resource 3", HttpVerb.GET));

        final List<ProjectUser> projectUsers = new ArrayList<>();
        projectUsers.add(new ProjectUser());
        projectUsers.add(new ProjectUser());
        projectUsers.add(new ProjectUser());
        projectUsers.add(new ProjectUser());
        projectUsers.add(new ProjectUser());

        // Init default roles
        final Role rolePublic = new Role(0L, DefaultRoleNames.PUBLIC.toString(), null, permissionList,
                projectUsers.subList(0, 2), true, true);
        final Role roleRegisteredUser = new Role(1L, DefaultRoleNames.REGISTERED_USER.toString(), rolePublic, null,
                projectUsers.subList(2, 4), false, true);
        final Role roleAdmin = new Role(2L, DefaultRoleNames.ADMIN.toString(), roleRegisteredUser,
                permissionList.subList(1, 2), projectUsers.subList(1, 2), false, true);
        final Role roleProjectAdmin = new Role(3L, DefaultRoleNames.PROJECT_ADMIN.toString(), roleAdmin,
                permissionList.subList(1, 3), projectUsers.subList(1, 4), false, true);
        final Role roleInstanceAdmin = new Role(4L, DefaultRoleNames.INSTANCE_ADMIN.toString(), roleProjectAdmin,
                new ArrayList<>(), projectUsers.subList(0, 2), false, true);
        entities.add(rolePublic);
        entities.add(roleRegisteredUser);
        entities.add(roleAdmin);
        entities.add(roleProjectAdmin);
        entities.add(roleInstanceAdmin);

        // Init some custom roles
        final Role role5 = new Role(5L, "Role 5", rolePublic, permissionList.subList(1, 2), projectUsers.subList(0, 1));
        final Role role6 = new Role(6L, "Role 6", rolePublic, permissionList.subList(0, 2), projectUsers.subList(1, 2));
        final Role role7 = new Role(7L, "Role 7", rolePublic, permissionList.subList(1, 3), projectUsers.subList(0, 2));
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

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository#findByNameIn(java.util.Collection)
     */
    @Override
    public List<Role> findByNameIn(final Collection<String> pNames) {
        try (final Stream<Role> stream = entities.stream()) {
            return stream.filter(e -> pNames.contains(e.getName())).collect(Collectors.toList());
        }
    }

}
