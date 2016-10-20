/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.signature;

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

import fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidEntityException;

/**
 * Define the common interface of REST clients for accesses.
 *
 * @author CS SI
 */
@RequestMapping("/accesses")
public interface IAccessesSignature {

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<ProjectUser>>> retrieveAccessRequestList();

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<AccessRequestDTO>> requestAccess(@Valid @RequestBody AccessRequestDTO pAccessRequest)
            throws AlreadyExistingException, InvalidEntityException;

    @RequestMapping(value = "/{access_id}/accept", method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    HttpEntity<Void> acceptAccessRequest(@PathVariable("access_id") Long pAccessId)
            throws OperationNotSupportedException, EntityNotFoundException;

    @RequestMapping(value = "/{access_id}/deny", method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> denyAccessRequest(@PathVariable("access_id") Long pAccessId)
            throws OperationNotSupportedException, EntityNotFoundException;

    @RequestMapping(value = "/{access_id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> removeAccessRequest(@PathVariable("access_id") Long pAccessId) throws EntityNotFoundException;

    @RequestMapping(value = "/settings", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<AccessSettings>> getAccessSettings();

    @RequestMapping(value = "/settings", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> updateAccessSettings(@Valid @RequestBody AccessSettings pAccessSettings)
            throws EntityNotFoundException;
}
