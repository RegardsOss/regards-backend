/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import java.util.List;

import fr.cnes.regards.modules.accessrights.domain.Couple;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

/**
 * @author CS SI
 *
 */
public interface IProjectUserService {

    /**
     * Retrieve the {@link List} of all {@link ProjectUser}s.
     *
     * @return
     */
    List<ProjectUser> retrieveUserList();

    /**
     * Retrieve the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     */
    ProjectUser retrieveUser(Long pUserId);

    /**
     * Retrieve the {@link ProjectUser} of passed <code>login</code>.
     *
     * @param pLogin
     *            The {@link ProjectUser}'s <code>login</code>
     */
    ProjectUser retrieveUser(String pLogin);

    /**
     * Update the {@link ProjectUser} of id <code>pUserId</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser} <code>id</code>
     * @param pUpdatedProjectUser
     *            The new {@link ProjectUser}
     * @throws InvalidValueException
     *             Thrown when <code>pUserId</code> differs from the id of <code>pUpdatedProjectUser</code>
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    void updateUser(Long pUserId, ProjectUser pUpdatedProjectUser)
            throws InvalidValueException, EntityNotFoundException;

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
     * @param pLogin
     *            The {@link ProjectUser}'s <code>id</code>
     * @param pBorrowedRoleName
     *            The borrowed {@link Role} <code>name</code> if the user is connected with a borrowed role. Optional.
     * @return
     * @throws InvalidValueException
     *             Thrown when the passed {@link Role} is not hierarchically inferior to the true {@link ProjectUser}'s
     *             <code>role</code>.
     */
    Couple<List<ResourcesAccess>, Role> retrieveProjectUserAccessRights(String pLogin, String pBorrowedRoleName)
            throws InvalidValueException;

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
     * @return
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    List<MetaData> retrieveUserMetaData(Long pUserId) throws EntityNotFoundException;

    /**
     * Set the passed {@link MetaData} onto the {@link ProjectUser} of passed <code>id</code>
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     * @param pUpdatedUserMetaData
     *            The {@link List} of {@link MetaData} to set
     */
    void updateUserMetaData(Long pUserId, List<MetaData> pUpdatedUserMetaData) throws EntityNotFoundException;

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
     * @return
     */
    boolean existUser(Long pId);

    /**
     * Return true when {@link ProjectUser} of passed <code>login</code> exists in db.
     *
     * @param pLogin
     *            The {@link ProjectUser}'s <code>login</code>
     * @return
     */
    boolean existUser(String pLogin);
}
