package fr.cnes.regards.modules.accessRights.dao;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.service.AccountServiceStub;

/*
 * LICENSE_PLACEHOLDER
 */
@Repository
public class DaoProjectUserStub implements IDaoProjectUser {

    private static List<ProjectUser> projectUsers_;

    @Autowired
    private AccountServiceStub accountService;

    @PostConstruct
    public void init() {
        projectUsers_ = new ArrayList<>();
        projectUsers_.add(new ProjectUser(this.accountService.createAccount("instance_admin@cnes.fr")));
        projectUsers_.add(new ProjectUser(this.accountService.createAccount("project_admin_0@cnes.fr")));
        projectUsers_.add(new ProjectUser(this.accountService.createAccount("project_admin_1@cnes.fr")));
        projectUsers_.add(new ProjectUser(this.accountService.createAccount("admin_0@cnes.fr")));
        projectUsers_.add(new ProjectUser(this.accountService.createAccount("admin_1@cnes.fr")));
        projectUsers_.add(new ProjectUser(this.accountService.createAccount("registered_user_0@cnes.fr")));
        projectUsers_.add(new ProjectUser(this.accountService.createAccount("registered_user_1@cnes.fr")));
        projectUsers_.add(new ProjectUser(this.accountService.createAccount("registered_user_2@cnes.fr")));
        projectUsers_.add(new ProjectUser(this.accountService.createAccount("registered_user_3@cnes.fr")));
        projectUsers_.add(new ProjectUser(this.accountService.createAccount("public_0@cnes.fr")));
        projectUsers_.add(new ProjectUser(this.accountService.createAccount("public_1@cnes.fr")));

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
