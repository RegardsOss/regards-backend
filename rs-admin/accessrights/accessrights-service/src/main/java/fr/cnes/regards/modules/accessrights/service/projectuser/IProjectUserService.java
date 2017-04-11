/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.projectuser;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;

/**
 * Strategy interface to handle Read an Update operations on access settings.
 *
 * @author Xavier-Alexandre Brochard
 *
 */
public interface IProjectUserService {

    /**
     * Retrieve the {@link List} of all {@link ProjectUser}s.
     *
     * @param pPageable
     *            the paging information
     *
     * @return The list of project users
     */
    Page<ProjectUser> retrieveUserList(Pageable pPageable);

    /**
     * Retrieve the {@link List} of all {@link ProjectUser}s with the given status.
     *
     * @param pStatus
     *            the {@link UserStatus} to retrieve
     * @param pPageable
     *            the paging information
     *
     * @return The list of project users
     */
    Page<ProjectUser> retrieveUserList(UserStatus pStatus, Pageable pPageable);

    /**
     * Retrieve the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     * @return The project user
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    ProjectUser retrieveUser(Long pUserId) throws EntityNotFoundException;

    /**
     * Retrieve the {@link ProjectUser} of passed <code>email</code>.
     *
     * @param pEmail
     *            The {@link ProjectUser}'s <code>email</code>
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     * @return The project user
     */
    ProjectUser retrieveOneByEmail(String pEmail) throws EntityNotFoundException;

    /**
     * Retrieve the current {@link ProjectUser}.
     *
     * @return The project user
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with <code>email</code> equal to the one set in current tenant
     *             could be found
     */
    ProjectUser retrieveCurrentUser() throws EntityNotFoundException;

    /**
     *
     * Create a new projectUser (and account if missing) without resitration process.
     *
     * @param pDto
     * @return
     * @since 1.0-SNAPSHOT
     */
    ProjectUser createProjectUser(final AccessRequestDto pDto) throws EntityAlreadyExistsException;

    /**
     * Update the {@link ProjectUser} of id <code>pUserId</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser} <code>id</code>
     * @param pUpdatedProjectUser
     *            The new {@link ProjectUser}
     * @throws EntityException
     *             <br>
     *             {@link EntityInconsistentIdentifierException} Thrown when <code>pUserId</code> differs from the id of
     *             <code>pUpdatedProjectUser</code><br>
     *             {@link EntityNotFoundException} Thrown when no {@link ProjectUser} with passed <code>id</code> could
     *             be found<br>
     */
    ProjectUser updateUser(Long pUserId, ProjectUser pUpdatedProjectUser) throws EntityException;

    /**
     * Retrieve the {@link List} of {@link ResourcesAccess} for the {@link Account} of passed <code>id</code>.
     *
     * @param pEmail
     *            The {@link ProjectUser}'s <code>id</code>
     * @param pBorrowedRoleName
     *            The borrowed {@link Role} <code>name</code> if the user is connected with a borrowed role. Optional.
     * @return The list of resources access
     * @throws EntityException
     *             <br>
     *             {@link EntityOperationForbiddenException} Thrown when the passed {@link Role} is not hierarchically
     *             inferior to the true {@link ProjectUser}'s <code>role</code><br>
     *             {@link EntityNotFoundException} Thrown when no {@link ProjectUser} with passed <code>id</code> could
     *             be found<br>
     */
    List<ResourcesAccess> retrieveProjectUserAccessRights(String pEmail, String pBorrowedRoleName)
            throws EntityException;

    /**
     * Update the the {@link List} of <code>permissions</code>.
     *
     * @param pLogin
     *            The {@link ProjectUser}'s <code>login</code>
     * @param pUpdatedUserAccessRights
     *            The {@link List} of {@link ResourcesAccess} to set
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    void updateUserAccessRights(String pLogin, List<ResourcesAccess> pUpdatedUserAccessRights)
            throws EntityNotFoundException;

    /**
     * Clear the {@link List} of {@link ResourcesAccess} of the {@link ProjectUser} with passed <code>login</code>.
     *
     * @param pLogin
     *            The {@link ProjectUser} <code>login</code>
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    void removeUserAccessRights(String pLogin) throws EntityNotFoundException;

    /**
     * Return the {@link List} of {@link MetaData} on the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     * @return The list of meta data
     */
    List<MetaData> retrieveUserMetaData(Long pUserId) throws EntityNotFoundException;

    /**
     * Set the passed {@link MetaData} onto the {@link ProjectUser} of passed <code>id</code>
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     * @param pUpdatedUserMetaData
     *            The {@link List} of {@link MetaData} to set
     * @throws EntityNotFoundException
     *             Thhrown when not project user of passed <code>id</code> could be found
     */
    List<MetaData> updateUserMetaData(Long pUserId, List<MetaData> pUpdatedUserMetaData) throws EntityNotFoundException;

    /**
     * Clear the {@link List} of {@link MetaData} of the {@link ProjectUser} with passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser} <code>id</code>
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    void removeUserMetaData(Long pUserId) throws EntityNotFoundException;

    /**
     * Return true when {@link ProjectUser} of passed <code>id</code> exists in db.
     *
     * @param pId
     *            The {@link ProjectUser}'s <code>id</code>
     * @return <code>True</code> exists, else <code>False</code>
     */
    boolean existUser(Long pId);

    /**
     * Return true when {@link ProjectUser} of passed <code>email</code> exists in db.
     *
     * @param pEmail
     *            The {@link ProjectUser}'s <code>email</code>
     * @return <code>True</code> exists, else <code>False</code>
     */
    boolean existUser(String pEmail);

    /**
     * Retrieve all access requests.
     *
     * @param pPageable
     *            the pagination information
     *
     * @return The {@link List} of all {@link ProjectUser}s with status {@link UserStatus#WAITING_ACCESS}
     */
    Page<ProjectUser> retrieveAccessRequestList(Pageable pPageable);

    /**
     * reset the licence for each user from the current project(which is in the SecurityContext)
     */
    void resetLicence();

}
