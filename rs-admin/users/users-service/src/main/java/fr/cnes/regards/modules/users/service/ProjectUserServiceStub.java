package fr.cnes.regards.modules.users.service;

import java.util.List;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.users.domain.ProjectUser;

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
