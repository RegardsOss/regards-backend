package fr.cnes.regards.modules.accessRights.service;

import java.util.List;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;

public interface IProjectUserService {

    List<ProjectUser> retrieveAccessRequestList();

    ProjectUser requestAccess(ProjectUser pAccessRequest);

    ProjectUser retrieveAccessRequest(String pAccessId);

    void updateAccessRequest(String pAccessId, ProjectUser pUpdatedAccessRequest);

}
