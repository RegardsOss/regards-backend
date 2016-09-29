/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.client;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.naming.OperationNotSupportedException;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.core.annotation.ModuleInfo;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

@RestController
@ModuleInfo(name = "accessRights", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
@RequestMapping("/accesses")
public class AccessesFallback implements IAccessesClient {

    @Override
    public HttpEntity<List<Resource<ProjectUser>>> retrieveAccessRequestList() {
        List<ProjectUser> projectUsers = new ArrayList<>();
        List<Resource<ProjectUser>> resources = projectUsers.stream().map(p -> new Resource<>(p))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
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
