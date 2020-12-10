/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.service.role;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * Define the base interface for any implementation of a Role Service.
 *
 * @author CS SI
 * @author Sylvain Vissiere-Guerinet
 */
public interface IRoleService {

    /**
     * Retrieve the set of all {@link Role}s.
     *
     * @return all {@link Role}s.
     */
    Set<Role> retrieveRoles();

    /**
     *
     * Retrieve the {@link Role} of passed <code>name</code>.
     *
     * @param pRoleName
     *            role name to retrieve
     * @return The {@link Role}'s
     * @throws EntityNotFoundException
     *             when no role with passed name could be found
    
     */
    Role retrieveRole(String pRoleName) throws EntityNotFoundException;

    /**
     *
     * retrieve a role by its Id
     *
     * @param pRoleId
     * @return required role
     * @throws EntityNotFoundException
     */
    Role retrieveRole(Long pRoleId) throws EntityNotFoundException;

    /**
     * Return the single <code>default</code> {@link Role}.
     *
     * @return The only {@link Role} with it's <code>default</code> attribute set to <code>true</code>
     */
    Role getDefaultRole();

    /**
     * Update the {@link Role} of name <code>pRoleName</code>.
     *
     * @param pRoleName
     *            The {@link Role} <code>name</code>
     * @param pUpdatedRole
     *            The new {@link Role}
     * @return the updated {@link Role}
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} when no {@link Role} with passed <code>id</code> could be found<br>
     *             {@link EntityInconsistentIdentifierException} Thrown when <code>pRoleId</code> is different from the
     *             id of <code>pUpdatedRole</code><br>
     */
    Role updateRole(String pRoleName, Role pUpdatedRole) throws EntityException;

    /**
     *
     * Create a new {@link Role} using native parent permissions as own permissions.
     *
     * @param role
     *            new {@link @Role} to create
     * @return {@link Role} created
     * @throws EntityException
    
     */
    Role createRole(Role role) throws EntityException;

    /**
     * Delete the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @throws EntityException
     *             If role doesn't exists or when the updated role is native. Native roles should not be removed
     */
    void removeRole(Long pRoleId) throws EntityException;

    /**
     * Delete the {@link Role} of passed <code>name</code>.
     *
     * @param pRoleName
     *            The {@link Role}'s <code>name</code>
     * @throws EntityException
     *             If role doesn't exists or when the updated role is native. Native roles should not be removed
     */
    void removeRole(String pRoleName) throws EntityException;

    /**
     * Return the {@link List} of {@link ResourcesAccess} on the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @return The {@link List} of {@link ResourcesAccess} on the {@link Role}
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    Set<ResourcesAccess> retrieveRoleResourcesAccesses(Long pRoleId) throws EntityNotFoundException;

    /**
     * Replace old ResourcesAccesses of the given role by the given ones.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @param pResourcesAccesses
     *            The {@link List} of {@link ResourcesAccess} to set
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     * @return The updated {@link Role}
     * @throws EntityException {@link EntityOperationForbiddenException} if pRoleId is the id of PROJECT_ADMIN
     *
     */
    Role updateRoleResourcesAccess(Long pRoleId, Set<ResourcesAccess> pResourcesAccesses) throws EntityException;

    /**
     * Clear the {@link List} of {@link ResourcesAccess} of the {@link Role} with passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role} <code>id</code>
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    void clearRoleResourcesAccess(Long pRoleId) throws EntityNotFoundException;

    /**
     * Retrieve the {@link List} of {@link ProjectUser} for the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @param pPageable
     *            the paging information
     * @return The {@link List} of {@link ProjectUser} for the {@link Role}
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    Page<ProjectUser> retrieveRoleProjectUserList(Long pRoleId, Pageable pPageable) throws EntityNotFoundException;

    /**
     * Retrieve the {@link List} of {@link ProjectUser} for the {@link Role} of passed <code>name</code>.
     *
     * @param roleName
     *            The {@link Role}'s <code>id</code>
     * @param pPageable
     *            the paging information
     * @return The {@link List} of {@link ProjectUser} for the {@link Role}
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    Page<ProjectUser> retrieveRoleProjectUserList(String roleName, Pageable pPageable) throws EntityNotFoundException;

    /**
     * Return true when {@link Role} of passed <code>id</code> exists in db.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @return True if the role exists, else false
     */
    boolean existRole(Long pRoleId);

    /**
     * Return true when the passed {@link Role} exists in db.
     *
     * @param pRole
     *            The {@link Role}
     * @return True if the role exists, else false
     */
    boolean existRole(Role pRole);

    /**
     * Return true when a {@link Role} with passed name exists in db.
     *
     * @param pName
     *            The {@link Role}'s name
     * @return True if the role exists, else false
     */
    boolean existByName(String pName);

    /**
     * Return true if the {@link Role} <code>pRole</code> is hierarchically inferior the the {@link Role}
     * <code>pOther</code>.
     *
     * @param pRole
     *            The {@link Role} we want to compare
     * @param pOther
     *            The reference {@link Role}
     * @return True if the {@link Role} <code>pRole</code> is hierarchically inferior the the {@link Role}, else false
     */
    boolean isHierarchicallyInferior(Role pRole, Role pOther);

    /**
     *
     * Retrieve the inherited roles of the given role. For exemple this method return PUBLIC, REGISTERED_USER for role
     * ADMIN.
     *
     * @param pRole
     *            role to retrieve inherited roles
     * @return list of {@link Role}
    
     */
    Set<Role> retrieveInheritedRoles(Role pRole);

    /**
     * Remove given resources accesses from the given role and its descendancy
     *
     * @param pRoleName the role name
     * @param pResourcesAccess the resources accesses to remove on the role
     * @throws EntityException {@link EntityOperationForbiddenException} thrown if pRole is PROJECT_ADMIN
     */
    void removeResourcesAccesses(String pRoleName, ResourcesAccess... pResourcesAccess) throws EntityException;

    /**
     * Add given resources accesses to the role of given role id
     *
     * @param pRoleId
     * @param pNewOnes
     * @throws EntityException
     */
    void addResourceAccesses(Long pRoleId, ResourcesAccess... pNewOnes) throws EntityException;

    /**
     * Retrieve roles that a user can borrow.
     *
     * @return roles which are borrowable for the current user
     */
    Set<Role> retrieveBorrowableRoles();

    /**
     * Retrieve descendants of a role.
     * @param role
     * @return all descendants of "role" and "role" itself
     */
    Set<Role> getDescendants(Role role);

    /**
     * Retrieve ascendants(parent and uncles) and brotherhood of the given role
     *
     * @param pRole
     * @return All ascendants of the given role
     */
    Set<Role> getAscendants(Role pRole);

    /**
     *
     * Retrieve roles associated to ginve resource id
     *
     * @param pResourceId
     * @return list of {@link Role}
    
     */
    Set<Role> retrieveRolesWithResource(Long pResourceId);

    /**
     * Init all default roles for the current tenant
     */
    void initDefaultRoles();

    /**
     * @param roleName ${@link Role} to compare with
     * @return true if the current user role is superior to the provided one
     */
    boolean isCurrentRoleSuperiorTo(String roleName) throws EntityNotFoundException;

    /**
     * Return current authentified user role
     * @return {@link Role}
     */
    Optional<Role> getCurrentRole();
}
