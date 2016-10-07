/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao.stubs;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.accessRights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessRights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessRights.domain.UserStatus;
import fr.cnes.regards.modules.accessRights.domain.instance.Account;
import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.projects.Role;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

@Repository
@Profile("test")
@Primary
public class ProjectUserRepositoryStub extends RepositoryStub<ProjectUser> implements IProjectUserRepository {

    private final IAccountRepository accountRepository_;

    public ProjectUserRepositoryStub(final IAccountRepository pAccountService) throws AlreadyExistingException {
        super();
        accountRepository_ = pAccountService;

        final Role rolePublic = new Role(0L, "Public", null, null, null, true, true);
        final Role roleRegisteredUser = new Role(1L, "Registered User", rolePublic, null, null, false, true);
        final Role roleAdmin = new Role(2L, "Admin", roleRegisteredUser, null, null, false, true);
        final Role roleProjectAdmin = new Role(3L, "Project Admin", roleAdmin, null, null, false, true);
        final Role roleInstanceAdmin = new Role(4L, "Instance Admin", roleProjectAdmin, null, null, false, true);

        entities_.add(buildProjectUser(0L, "instance_admin@cnes.fr", roleInstanceAdmin));
        entities_.add(buildProjectUser(1L, "project_admin_0@cnes.fr", roleProjectAdmin));
        entities_.add(buildProjectUser(2L, "project_admin_1@cnes.fr", roleProjectAdmin));
        entities_.add(buildProjectUser(3L, "admin_0@cnes.fr", roleAdmin));
        entities_.add(buildProjectUser(4L, "admin_1@cnes.fr", roleAdmin));
        entities_.add(buildProjectUser(5L, "registered_user_0@cnes.fr", roleRegisteredUser));
        entities_.add(buildProjectUser(6L, "registered_user_1@cnes.fr", roleRegisteredUser));
        entities_.add(buildProjectUser(7L, "registered_user_2@cnes.fr", roleRegisteredUser));
        entities_.add(buildProjectUser(8L, "registered_user_3@cnes.fr", roleRegisteredUser));
        entities_.add(buildProjectUser(9L, "public_0@cnes.fr", rolePublic));
        entities_.add(buildProjectUser(10L, "public_1@cnes.fr", rolePublic));

        // getByEmail("admin_0@cnes.fr").accept();
        // getByEmail("project_admin_0@cnes.fr").accept();
        // getByEmail("registered_user_0@cnes.fr").accept();
    }

    private ProjectUser buildProjectUser(final Long pProjectUserId, final String pMail, final Role pRole)
            throws AlreadyExistingException {

        Account account = new Account(pMail);
        account = accountRepository_.save(account);

        ProjectUser projectUser;
        projectUser = new ProjectUser(pProjectUserId, LocalDateTime.now(), LocalDateTime.now(),
                UserStatus.ACCESS_GRANTED, new ArrayList<>(), pRole, new ArrayList<>(), account);
        return projectUser;
    }

    @Override
    public ProjectUser findOneByLogin(final String pLogin) {
        return entities_.stream().filter(r -> r.getAccount().getLogin().equals(pLogin)).findFirst().get();
    }

    @Override
    public boolean exists(final String pLogin) {
        return findOneByLogin(pLogin) != null;
    }

}
