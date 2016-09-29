/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao.stubs;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.accessRights.dao.IAccountRepository;
import fr.cnes.regards.modules.accessRights.dao.IProjectUserRepository;
import fr.cnes.regards.modules.accessRights.domain.Account;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.accessRights.domain.UserStatus;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

@Repository
@Profile("test")
@Primary
public class ProjectUserRepositoryStub extends RepositoryStub<ProjectUser> implements IProjectUserRepository {

    private final IAccountRepository accountRepository_;

    public ProjectUserRepositoryStub(IAccountRepository pAccountService) throws AlreadyExistingException {
        super();
        accountRepository_ = pAccountService;

        Role rolePublic = new Role(0L, "Public", null, null, null, true, true);
        Role roleRegisteredUser = new Role(1L, "Registered User", rolePublic, null, null, false, true);
        Role roleAdmin = new Role(2L, "Admin", roleRegisteredUser, null, null, false, true);
        Role roleProjectAdmin = new Role(3L, "Project Admin", roleAdmin, null, null, false, true);
        Role roleInstanceAdmin = new Role(4L, "Instance Admin", roleProjectAdmin, null, null, false, true);

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

    private ProjectUser buildProjectUser(Long pProjectUserId, String pMail, Role pRole)
            throws AlreadyExistingException {

        Account account = new Account(pMail);
        account = accountRepository_.save(account);

        ProjectUser projectUser;
        projectUser = new ProjectUser(pProjectUserId, LocalDateTime.now(), LocalDateTime.now(),
                UserStatus.ACCESS_GRANTED, new ArrayList<>(), pRole, new ArrayList<>(), account);
        return projectUser;
    }

    @Override
    public ProjectUser findOneByLogin(String pLogin) {
        return entities_.stream().filter(r -> r.getAccount().getLogin().equals(pLogin)).findFirst().get();
    }

    @Override
    public boolean exists(String pLogin) {
        return findOneByLogin(pLogin) != null;
    }

}
