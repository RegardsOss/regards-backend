/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.signature;

import java.util.List;

import javax.naming.OperationNotSupportedException;
import javax.validation.Valid;

import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.framework.module.rest.exception.ModuleAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Define the common interface of REST clients for accesses.
 *
 * @author CS SI
 */
@RequestMapping("/accesses")
public interface IAccessesSignature {

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<List<Resource<ProjectUser>>> retrieveAccessRequestList();

    /**
     * Request a new access, i.e. a new project user
     *
     * @param pAccessRequest
     *            A Dto containing all information for creating a the new project user
     * @throws ModuleAlreadyExistsException
     *             When a project user with passed <code>email</code> already exists
     * @throws EntityTransitionForbiddenException
     *             TODO
     * @return the passed Dto
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<Resource<AccessRequestDTO>> requestAccess(@Valid @RequestBody AccessRequestDTO pAccessRequest)
            throws ModuleAlreadyExistsException, EntityTransitionForbiddenException, ModuleEntityNotFoundException;

    /**
     * Grants access to the project user
     *
     * @param pAccessId
     *            the project user id
     * @throws ModuleEntityNotFoundException
     *             when no project user of passed <code>id</code> could be found
     * @throws EntityTransitionForbiddenException
     *             when the project user has a <code>status</code> not allowing access granting
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @RequestMapping(value = "/{access_id}/accept", method = RequestMethod.PUT)
    ResponseEntity<Void> acceptAccessRequest(@PathVariable("access_id") Long pAccessId)
            throws OperationNotSupportedException, ModuleEntityNotFoundException, EntityTransitionForbiddenException;

    /**
     * Denies access to the project user
     *
     * @param pAccessId
     *            the project user id
     * @throws ModuleEntityNotFoundException
     *             when no project user of passed <code>id</code> could be found
     * @throws EntityTransitionForbiddenException
     *             when the project user has a <code>status</code> not allowing access denial
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = "/{access_id}/deny", method = RequestMethod.PUT)
    ResponseEntity<Void> denyAccessRequest(@PathVariable("access_id") Long pAccessId)
            throws ModuleEntityNotFoundException, EntityTransitionForbiddenException;

    /**
     * Rejects the access request
     *
     * @param pAccessId
     *            the project user id
     * @throws ModuleEntityNotFoundException
     *             when no project user of passed <code>id</code> could be found
     * @throws EntityTransitionForbiddenException
     *             when the project user has a <code>status</code> not allowing access rejection
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = "/{access_id}", method = RequestMethod.DELETE)
    ResponseEntity<Void> removeAccessRequest(@PathVariable("access_id") Long pAccessId)
            throws ModuleEntityNotFoundException, EntityTransitionForbiddenException;

    @ResponseBody
    @RequestMapping(value = "/settings", method = RequestMethod.GET)
    ResponseEntity<Resource<AccessSettings>> getAccessSettings();

    @ResponseBody
    @RequestMapping(value = "/settings", method = RequestMethod.PUT)
    ResponseEntity<Void> updateAccessSettings(@Valid @RequestBody AccessSettings pAccessSettings)
            throws ModuleEntityNotFoundException;
}
