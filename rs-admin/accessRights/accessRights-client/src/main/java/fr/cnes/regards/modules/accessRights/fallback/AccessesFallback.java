/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.fallback;

import java.util.List;

import javax.naming.OperationNotSupportedException;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;

import fr.cnes.regards.modules.accessRights.client.AccessesClient;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

public class AccessesFallback implements AccessesClient {

    @Override
    public HttpEntity<List<Resource<ProjectUser>>> retrieveAccessRequestList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Resource<ProjectUser>> requestAccess(ProjectUser pAccessRequest) throws AlreadyExistingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> acceptAccessRequest(Long pAccessId) throws OperationNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> denyAccessRequest(Long pAccessId) throws OperationNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> removeAccessRequest(Long pAccessId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<List<Resource<String>>> getAccessSettingList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> updateAccessSetting(String pUpdatedProjectUserSetting) throws InvalidValueException {
        // TODO Auto-generated method stub
        return null;
    }

}
