/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.projectuser;

import java.util.List;

import fr.cnes.regards.framework.module.rest.exception.InvalidValueException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * @author CS SI
 *
 */
public interface IProjectUserService extends IProjectUserTransitions {

    /**
     * Retrieve the {@link List} of all {@link ProjectUser}s.
     *
     * @return The list of project users
     */
    List<ProjectUser> retrieveUserList();

    /**
     * Retrieve the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     * @return The project user
     */
    ProjectUser retrieveUser(Long pUserId);

    /**
     * Retrieve the {@link ProjectUser} of passed <code>email</code>.
     *
     * @param pEmail
     *            The {@link ProjectUser}'s <code>email</code>
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     * @return The project user
     */
    ProjectUser retrieveOneByEmail(String pEmail) throws ModuleEntityNotFoundException;

    /**
     * Retrieve the current {@link ProjectUser}.
     *
     * @return The project user
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link ProjectUser} with <code>email</code> equal to the one set in current tenant
     *             could be found
     */
    ProjectUser retrieveCurrentUser() throws ModuleEntityNotFoundException;

    /**
     * Update the {@link ProjectUser} of id <code>pUserId</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser} <code>id</code>
     * @param pUpdatedProjectUser
     *            The new {@link ProjectUser}
     * @throws InvalidValueException
     *             Thrown when <code>pUserId</code> differs from the id of <code>pUpdatedProjectUser</code>
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    void updateUser(Long pUserId, ProjectUser pUpdatedProjectUser)
            throws InvalidValueException, ModuleEntityNotFoundException;

    /**
     * Delete the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     */
    void removeUser(Long pUserId);

    /**
     * Retrieve the {@link List} of {@link ResourcesAccess} for the {@link Account} of passed <code>id</code>.
     *
     * @param pEmail
     *            The {@link ProjectUser}'s <code>id</code>
     * @param pBorrowedRoleName
     *            The borrowed {@link Role} <code>name</code> if the user is connected with a borrowed role. Optional.
     * @return The list of resources access
     * @throws InvalidValueException
     *             Thrown when the passed {@link Role} is not hierarchically inferior to the true {@link ProjectUser}'s
     *             <code>role</code>.
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    List<ResourcesAccess> retrieveProjectUserAccessRights(String pEmail, String pBorrowedRoleName)
            throws InvalidValueException, ModuleEntityNotFoundException;

    /**
     * Update the the {@link List} of <code>permissions</code>.
     *
     * @param pLogin
     *            The {@link ProjectUser}'s <code>login</code>
     * @param pUpdatedUserAccessRights
     *            The {@link List} of {@link ResourcesAccess} to set
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    void updateUserAccessRights(String pLogin, List<ResourcesAccess> pUpdatedUserAccessRights)
            throws ModuleEntityNotFoundException;

    /**
     * Clear the {@link List} of {@link ResourcesAccess} of the {@link ProjectUser} with passed <code>login</code>.
     *
     * @param pLogin
     *            The {@link ProjectUser} <code>login</code>
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    void removeUserAccessRights(String pLogin) throws ModuleEntityNotFoundException;

    /**
     * Return the {@link List} of {@link MetaData} on the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     * @return The list of meta data
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    List<MetaData> retrieveUserMetaData(Long pUserId) throws ModuleEntityNotFoundException;

    /**
     * Set the passed {@link MetaData} onto the {@link ProjectUser} of passed <code>id</code>
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     * @param pUpdatedUserMetaData
     *            The {@link List} of {@link MetaData} to set
     * @throws ModuleEntityNotFoundException
     *             Thhrown when not project user of passed <code>id</code> could be found
     */
    void updateUserMetaData(Long pUserId, List<MetaData> pUpdatedUserMetaData) throws ModuleEntityNotFoundException;

    /**
     * Clear the {@link List} of {@link MetaData} of the {@link ProjectUser} with passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser} <code>id</code>
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    void removeUserMetaData(Long pUserId) throws ModuleEntityNotFoundException;

    /**
     * Return true when {@link ProjectUser} of passed <code>id</code> exists in db.
     *
     * @param pId
     *            The {@link ProjectUser}'s <code>id</code>
     * @return <code>True</code> exists, else <code>False</code>
     */
    boolean existUser(Long pId);

    /**
     * Return true when {@link ProjectUser} of passed <code>login</code> exists in db.
     *
     * @param pLogin
     *            The {@link ProjectUser}'s <code>login</code>
     * @return <code>True</code> exists, else <code>False</code>
     */
    boolean existUser(String pLogin);

    /**
     * Retrieve all access requests.
     *
     * @return The {@link List} of all {@link ProjectUser}s with status {@link UserStatus#WAITING_ACCESS}
     */
    List<ProjectUser> retrieveAccessRequestList();

}
