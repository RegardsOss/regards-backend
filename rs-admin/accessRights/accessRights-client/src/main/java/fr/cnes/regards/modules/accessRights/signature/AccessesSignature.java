package fr.cnes.regards.modules.accessRights.signature;

import java.util.List;

import javax.naming.OperationNotSupportedException;
import javax.validation.Valid;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.microservices.core.security.endpoint.annotation.ResourceAccess;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

public interface AccessesSignature {

    @ResourceAccess(description = "retrieve the list of access request", name = "")
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<ProjectUser>>> retrieveAccessRequestList();

    @ResourceAccess(description = "create a new access request", name = "")
    @RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<ProjectUser>> requestAccess(@Valid @RequestBody ProjectUser pAccessRequest)
            throws AlreadyExistingException;

    @ResourceAccess(description = "accept the access request", name = "")
    @RequestMapping(value = "/{access_id}/accept", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    HttpEntity<Void> acceptAccessRequest(@PathVariable("access_id") Long pAccessId)
            throws OperationNotSupportedException;

    @ResourceAccess(description = "deny the access request", name = "")
    @RequestMapping(value = "/{access_id}/deny", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> denyAccessRequest(@PathVariable("access_id") Long pAccessId) throws OperationNotSupportedException;

    @ResourceAccess(description = "remove the access request", name = "")
    @RequestMapping(value = "/{access_id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> removeAccessRequest(@PathVariable("access_id") Long pAccessId);

    @ResourceAccess(description = "retrieve the list of setting managing the access requests", name = "")
    @RequestMapping(value = "/settings", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<String>>> getAccessSettingList();

    @ResourceAccess(description = "update the setting managing the access requests", name = "")
    @RequestMapping(value = "/settings", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> updateAccessSetting(@Valid @RequestBody String pUpdatedProjectUserSetting)
            throws InvalidValueException;
}
