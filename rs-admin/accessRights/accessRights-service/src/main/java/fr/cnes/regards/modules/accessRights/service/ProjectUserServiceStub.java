package fr.cnes.regards.modules.accessRights.service;

import java.util.List;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;

/*
 * LICENSE_PLACEHOLDER
 */
@Service
public class ProjectUserServiceStub implements IProjectUserService {

    @Override
    public List<ProjectUser> retrieveAccessRequestList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProjectUser requestAccess(ProjectUser pAccessRequest) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProjectUser retrieveAccessRequest(String pAccessId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateAccessRequest(String pAccessId, ProjectUser pUpdatedAccessRequest) {
        // TODO Auto-generated method stub

    }

}
