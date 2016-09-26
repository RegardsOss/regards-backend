/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao.stubs;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.accessRights.dao.IDaoProjectUser;
import fr.cnes.regards.modules.accessRights.domain.Account;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.accessRights.domain.UserStatus;
import fr.cnes.regards.modules.accessRights.service.IAccountService;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

@Repository
@Profile("test")
@Primary
public class DaoProjectUserStub implements IDaoProjectUser {

    private static List<ProjectUser> projectUsers_;

    private final IAccountService accountService_;

    public DaoProjectUserStub(IAccountService pAccountService) throws AlreadyExistingException {
        accountService_ = pAccountService;

        projectUsers_ = new ArrayList<>();
        Role rolePublic = new Role(0L, "Public", null, null, null, true, true);
        Role roleRegisteredUser = new Role(1L, "Registered User", rolePublic, null, null, false, true);
        Role roleAdmin = new Role(2L, "Admin", roleRegisteredUser, null, null, false, true);
        Role roleProjectAdmin = new Role(3L, "Project Admin", roleAdmin, null, null, false, true);
        Role roleInstanceAdmin = new Role(4L, "Instance Admin", roleProjectAdmin, null, null, false, true);

        projectUsers_.add(buildProjectUser(0L, "instance_admin@cnes.fr", roleInstanceAdmin));
        projectUsers_.add(buildProjectUser(1L, "project_admin_0@cnes.fr", roleProjectAdmin));
        projectUsers_.add(buildProjectUser(2L, "project_admin_1@cnes.fr", roleProjectAdmin));
        projectUsers_.add(buildProjectUser(3L, "admin_0@cnes.fr", roleAdmin));
        projectUsers_.add(buildProjectUser(4L, "admin_1@cnes.fr", roleAdmin));
        projectUsers_.add(buildProjectUser(5L, "registered_user_0@cnes.fr", roleRegisteredUser));
        projectUsers_.add(buildProjectUser(6L, "registered_user_1@cnes.fr", roleRegisteredUser));
        projectUsers_.add(buildProjectUser(7L, "registered_user_2@cnes.fr", roleRegisteredUser));
        projectUsers_.add(buildProjectUser(8L, "registered_user_3@cnes.fr", roleRegisteredUser));
        projectUsers_.add(buildProjectUser(9L, "public_0@cnes.fr", rolePublic));
        projectUsers_.add(buildProjectUser(10L, "public_1@cnes.fr", rolePublic));

        // getByEmail("admin_0@cnes.fr").accept();
        // getByEmail("project_admin_0@cnes.fr").accept();
        // getByEmail("registered_user_0@cnes.fr").accept();
    }

    private ProjectUser buildProjectUser(Long pProjectUserId, String pMail, Role pRole)
            throws AlreadyExistingException {
        Account account;
        ProjectUser projectUser;

        account = accountService_.createAccount(pMail);
        projectUser = new ProjectUser(pProjectUserId, LocalDateTime.now(), LocalDateTime.now(),
                UserStatus.ACCESS_GRANTED, new ArrayList<>(), pRole, new ArrayList<>(), account);
        return projectUser;
    }

    @Override
    public ProjectUser getByEmail(String pEmail) {
        return projectUsers_.stream().filter(r -> r.getAccount().getEmail().equals(pEmail)).findFirst().get();
    }

    @Override
    public List<ProjectUser> getAll() {
        return projectUsers_;
    }

}
