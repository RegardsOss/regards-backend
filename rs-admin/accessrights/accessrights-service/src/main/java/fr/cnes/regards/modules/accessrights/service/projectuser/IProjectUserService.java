/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.service.projectuser;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.*;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Strategy interface to handle Read an Update operations on access settings.
 * @author Xavier-Alexandre Brochard
 */
public interface IProjectUserService {

    /**
     * Retrieve the paged {@link List} of all {@link ProjectUser}s filtered by given properties.
     *
     * @param pageable  the paging information
     * @param parameters search parameters as request params
     * @return The list of project users
     */
    Page<ProjectUser> retrieveUserList(ProjectUserSearchParameters parameters, Pageable pageable);

    /**
     * Retrieve the {@link ProjectUser} of passed <code>id</code>.
     * @param pUserId The {@link ProjectUser}'s <code>id</code>
     * @return The project user
     * @throws EntityNotFoundException Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    ProjectUser retrieveUser(Long pUserId) throws EntityNotFoundException;

    /**
     * Retrieve the {@link ProjectUser} of passed <code>email</code>.
     * @param pEmail The {@link ProjectUser}'s <code>email</code>
     * @return The project user
     * @throws EntityNotFoundException Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    ProjectUser retrieveOneByEmail(String pEmail) throws EntityNotFoundException;

    /**
     * Retrieve the {@link ProjectUser} of passed <code>email</code>.
     * @param pEmail The {@link ProjectUser}'s <code>email</code>
     * @return The project user
     */
    Optional<ProjectUser> retrieveOneOptionalByEmail(String pEmail);

    /**
     * Retrieve the current {@link ProjectUser}.
     * @return The project user
     * @throws EntityNotFoundException Thrown when no {@link ProjectUser} with <code>email</code> equal to the one set in current tenant
     *                                 could be found
     */
    ProjectUser retrieveCurrentUser() throws EntityNotFoundException;

    /**
     * Create a new projectUser (and account if missing) without registration process.
     *
     * @param dto request
     * @return {@link ProjectUser}
     * @throws EntityException
     */
    ProjectUser createProjectUser(AccessRequestDto dto) throws EntityException;

    /**
     * Create a new project user.
     * Create a new account if necessary.
     *
     * @param accessRequestDto user information
     * @param isExternal
     * @param userStatus
     * @param accountStatus
     * @return
     * @throws EntityException
     */
    ProjectUser create(AccessRequestDto accessRequestDto, boolean isExternal, UserStatus userStatus, AccountStatus accountStatus) throws EntityException;

    /**
     * Update the {@link ProjectUser} of id <code>pUserId</code>.
     * @param pUserId The {@link ProjectUser} <code>id</code>
     * @param pUpdatedProjectUser The new {@link ProjectUser}
     * @return {@link ProjectUser}
     * @throws EntityException <br>
     *                         {@link EntityInconsistentIdentifierException} Thrown when <code>pUserId</code> differs from the id of
     *                         <code>pUpdatedProjectUser</code><br>
     *                         {@link EntityNotFoundException} Thrown when no {@link ProjectUser} with passed <code>id</code> could
     *                         be found<br>
     */
    ProjectUser updateUser(Long pUserId, ProjectUser pUpdatedProjectUser) throws EntityException;

    /**
     * Update the main informations for {@link ProjectUser} of id <code>pUserId</code>. The calculated informations are
     * not overidden.
     * @param pUserId The {@link ProjectUser} <code>id</code>
     * @param pUpdatedProjectUser The new {@link ProjectUser}
     * @return {@link ProjectUser}
     * @throws EntityException <br>
     *                         {@link EntityInconsistentIdentifierException} Thrown when <code>pUserId</code> differs from the id of
     *                         <code>pUpdatedProjectUser</code><br>
     *                         {@link EntityNotFoundException} Thrown when no {@link ProjectUser} with passed <code>id</code> could
     *                         be found<br>
     */
    ProjectUser updateUserInfos(Long pUserId, ProjectUser pUpdatedProjectUser) throws EntityException;

    /**
     * Retrieve the {@link List} of {@link ResourcesAccess} for the {@link Account} of passed <code>id</code>.
     * @param pEmail The {@link ProjectUser}'s <code>id</code>
     * @param pBorrowedRoleName The borrowed {@link Role} <code>name</code> if the user is connected with a borrowed role. Optional.
     * @return The list of resources access
     * @throws EntityException <br>
     *                         {@link EntityOperationForbiddenException} Thrown when the passed {@link Role} is not hierarchically
     *                         inferior to the true {@link ProjectUser}'s <code>role</code><br>
     *                         {@link EntityNotFoundException} Thrown when no {@link ProjectUser} with passed <code>id</code> could
     *                         be found<br>
     */
    List<ResourcesAccess> retrieveProjectUserAccessRights(String pEmail, String pBorrowedRoleName)
            throws EntityException;

    /**
     * Update the the {@link List} of <code>permissions</code>.
     * @param pLogin The {@link ProjectUser}'s <code>login</code>
     * @param pUpdatedUserAccessRights The {@link List} of {@link ResourcesAccess} to set
     * @throws EntityNotFoundException Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    void updateUserAccessRights(String pLogin, List<ResourcesAccess> pUpdatedUserAccessRights)
            throws EntityNotFoundException;

    /**
     * Clear the {@link List} of {@link ResourcesAccess} of the {@link ProjectUser} with passed <code>login</code>.
     * @param pLogin The {@link ProjectUser} <code>login</code>
     * @throws EntityNotFoundException Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    void removeUserAccessRights(String pLogin) throws EntityNotFoundException;

    /**
     * Return the {@link List} of {@link MetaData} on the {@link ProjectUser} of passed <code>id</code>.
     * @param pUserId The {@link ProjectUser}'s <code>id</code>
     * @return The list of meta data
     * @throws EntityNotFoundException Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    List<MetaData> retrieveUserMetaData(Long pUserId) throws EntityNotFoundException;

    /**
     * Set the passed {@link MetaData} onto the {@link ProjectUser} of passed <code>id</code>
     * @param pUserId The {@link ProjectUser}'s <code>id</code>
     * @param pUpdatedUserMetaData The {@link List} of {@link MetaData} to set
     * @return {@link MetaData}s updated
     *
     * @throws EntityNotFoundException Thhrown when not project user of passed <code>id</code> could be found
     */
    List<MetaData> updateUserMetaData(Long pUserId, List<MetaData> pUpdatedUserMetaData) throws EntityNotFoundException;

    /**
     * Clear the {@link List} of {@link MetaData} of the {@link ProjectUser} with passed <code>id</code>.
     * @param pUserId The {@link ProjectUser} <code>id</code>
     * @throws EntityNotFoundException Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    void removeUserMetaData(Long pUserId) throws EntityNotFoundException;

    /**
     * Retrieve all access requests.
     * @param pPageable the pagination information
     * @return The {@link List} of all {@link ProjectUser}s with status {@link UserStatus#WAITING_ACCESS}
     */
    Page<ProjectUser> retrieveAccessRequestList(Pageable pPageable);

    /**
     * reset the licence for each user from the current project(which is in the SecurityContext)
     */
    void resetLicence();

    /**
     * @param role
     * @return users which role is the given one
     */
    Collection<ProjectUser> retrieveUserByRole(Role role);

    /**
     * Determine whether current user has sufficient privilege to delete a user
     */
    boolean canDelete(ProjectUser projectUser);

    void sendVerificationEmail(String email) throws EntityNotFoundException;

    void updateQuota(Map<String, Long> currentQuotaByEmail);

    Map<String, Long> getUserCountByAccessGroup();

}
