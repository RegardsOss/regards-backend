/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import java.util.List;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Strategy interface to handle CRUD operations on access requests.<br>
 * In all the file, an "access request" is defined by "a {@link ProjectUser}s which <code>status</code> is equal to
 * {@link UserStatus#WAITING_ACCESS}".
 *
 * @author CS SI
 */
public interface IAccessRequestService {

    /**
     * Retrieve all access requests.
     *
     * @return The {@link List} of all {@link ProjectUser}s with status {@link UserStatus#WAITING_ACCESS}
     */
    List<ProjectUser> retrieveAccessRequestList();

    /**
     * Request a new access, that is to say create a new {@link ProjectUser} with <code>status</code>
     * {@link UserStatus#WAITING_ACCESS}.
     *
     * @param pDto
     *            The content of the request access. The DTO object simply wraps all information required for creating
     *            an {@link Account} and the linked {@link ProjectUser}.
     * @return All passed information
     * @throws AlreadyExistingException
     *             Thrown if a {@link ProjectUser} with same <code>email</code> already exists
     */
    AccessRequestDTO requestAccess(AccessRequestDTO pDto) throws AlreadyExistingException;

    /**
     * Remove the access request of passed <code>id</code>.
     *
     * @param pAccessId
     *            The resource access' <code>id</code>
     * @throws ModuleEntityNotFoundException
     *             Thrown if a {@link ProjectUser} with same <code>email</code> could not be found
     */
    void removeAccessRequest(Long pAccessId) throws ModuleEntityNotFoundException;

    /**
     * Accept an access request by setting the {@link ProjectUser}'s status to {@link UserStatus#ACCESS_GRANTED}.
     *
     * @param pAccessId
     *            The access access' <code>id</code>
     * @throws ModuleEntityNotFoundException
     *             Thrown if a {@link ProjectUser} with same <code>email</code> could not be found
     */
    void acceptAccessRequest(Long pAccessId) throws ModuleEntityNotFoundException;

    /**
     * Deny an access request by setting the {@link ProjectUser}'s status to {@link UserStatus#ACCESS_DENIED}.
     *
     * @param pAccessId
     *            The access access' <code>id</code>
     * @throws ModuleEntityNotFoundException
     *             Thrown when a project user with passed id could not be found
     * @throws ModuleEntityNotFoundException
     */
    void denyAccessRequest(Long pAccessId) throws ModuleEntityNotFoundException;

    /**
     * Tell if an access request with passed <code>id</code> exists.
     *
     * @param pId
     *            The {@link ProjectUser}'s <code>id</code>
     * @return <code>True</code> if exists, else <code>False</code>
     */
    boolean exists(Long pId);

    /**
     * Tell if a request access with passed <code>email</code> exists.
     *
     * @param pEmail
     *            The {@link ProjectUser}'s <code>email</code>
     * @return <code>True</code> if exists, else <code>False</code>
     */
    boolean existsByEmail(String pEmail);

}
