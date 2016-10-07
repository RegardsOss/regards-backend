/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import java.util.List;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

@Service
public class AccessRequestService implements IAccessRequestService {

    @Override
    public List<ProjectUser> retrieveAccessRequestList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProjectUser requestAccess(ProjectUser pAccessRequest) throws AlreadyExistingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getAccessSettingList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateAccessSetting(String pUpdatedProjectUserSetting) throws InvalidValueException {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeAccessRequest(Long pAccessId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void acceptAccessRequest(Long pAccessId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void denyAccessRequest(Long pAccessId) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean existAccessRequest(Long pAccessRequestId) {
        // TODO Auto-generated method stub
        return false;
    }

}
