package fr.cnes.regards.modules.accessRights.dao;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;

/*
 * LICENSE_PLACEHOLDER
 */
@Repository
public class DaoProjectUserStub implements IDaoProjectUser {

    private static List<ProjectUser> projectUsers_;

    @PostConstruct
    public void init() {
        projectUsers_ = new ArrayList<>();
        projectUsers_.add(new ProjectUser("instance_admin@cnes.fr"));
        projectUsers_.add(new ProjectUser("project_admin_0@cnes.fr"));
        projectUsers_.add(new ProjectUser("project_admin_1@cnes.fr"));
        projectUsers_.add(new ProjectUser("admin_0@cnes.fr"));
        projectUsers_.add(new ProjectUser("admin_1@cnes.fr"));
        projectUsers_.add(new ProjectUser("registered_user_0@cnes.fr"));
        projectUsers_.add(new ProjectUser("registered_user_1@cnes.fr"));
        projectUsers_.add(new ProjectUser("registered_user_2@cnes.fr"));
        projectUsers_.add(new ProjectUser("registered_user_3@cnes.fr"));
        projectUsers_.add(new ProjectUser("public_0@cnes.fr"));
        projectUsers_.add(new ProjectUser("public_1@cnes.fr"));
    }

    @Override
    public ProjectUser getByEmail(String pEmail) {
        return projectUsers_.stream().filter(r -> r.getEmail().equals(pEmail)).findFirst().get();
    }

    @Override
    public List<ProjectUser> getAll() {
        return projectUsers_;
    }

}
