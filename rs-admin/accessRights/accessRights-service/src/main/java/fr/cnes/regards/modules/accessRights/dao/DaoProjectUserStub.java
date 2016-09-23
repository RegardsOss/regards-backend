/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.dao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.service.IAccountService;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

@Repository
public class DaoProjectUserStub implements IDaoProjectUser {

    private static List<ProjectUser> projectUsers_;

    private final IAccountService accountService_;

    public DaoProjectUserStub(@Qualifier("accountServiceStub") IAccountService pAccountService)
            throws AlreadyExistingException {
        accountService_ = pAccountService;

        projectUsers_ = new ArrayList<>();
        projectUsers_.add(new ProjectUser(accountService_.createAccount("instance_admin@cnes.fr")));
        projectUsers_.add(new ProjectUser(accountService_.createAccount("project_admin_0@cnes.fr")));
        projectUsers_.add(new ProjectUser(accountService_.createAccount("project_admin_1@cnes.fr")));
        projectUsers_.add(new ProjectUser(accountService_.createAccount("admin_0@cnes.fr")));
        projectUsers_.add(new ProjectUser(accountService_.createAccount("admin_1@cnes.fr")));
        projectUsers_.add(new ProjectUser(accountService_.createAccount("registered_user_0@cnes.fr")));
        projectUsers_.add(new ProjectUser(accountService_.createAccount("registered_user_1@cnes.fr")));
        projectUsers_.add(new ProjectUser(accountService_.createAccount("registered_user_2@cnes.fr")));
        projectUsers_.add(new ProjectUser(accountService_.createAccount("registered_user_3@cnes.fr")));
        projectUsers_.add(new ProjectUser(accountService_.createAccount("public_0@cnes.fr")));
        projectUsers_.add(new ProjectUser(accountService_.createAccount("public_1@cnes.fr")));

        getByEmail("admin_0@cnes.fr").accept();
        getByEmail("project_admin_0@cnes.fr").accept();
        getByEmail("registered_user_0@cnes.fr").accept();
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
