/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import java.util.List;

import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

/**
 * Define the base interface for any implementation of a Role Service.
 *
 * @author CS SI
 */
public interface IRoleService {

    /**
     * Retrieve the {@link List} of all {@link Role}s.
     *
     * @return The {@link List} of all {@link Role}s.
     */
    List<Role> retrieveRoleList();

    /**
     * Create a new {@link Role}.
     *
     * @param pNewRole
     *            The new {@link Role} values
     * @return The created {@link Role}
     * @throws AlreadyExistingException
     *             Thrown if a {@link Role} with same <code>id</code> already exists
     */
    Role createRole(Role pNewRole) throws AlreadyExistingException;

    /**
     * Retrieve the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @return The {@link Role}
     */
    Role retrieveRole(Long pRoleId);

    /**
     *
     * Retrieve the {@link Role} of passed <code>name</code>.
     *
     * @param pRoleName
     *            role name to retrieve
     * @return The {@link Role}'s
     * @since 1.0-SNAPSHOT
     */
    Role retrieveRole(String pRoleName);

    /**
     * Update the {@link Role} of id <code>pRoleId</code>.
     *
     * @param pRoleId
     *            The {@link Role} <code>id</code>
     * @param pUpdatedRole
     *            The new {@link Role}
     * @throws InvalidValueException
     *             Thrown when <code>pRoleId</code> is different from the id of <code>pUpdatedRole</code>
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    void updateRole(Long pRoleId, Role pUpdatedRole) throws InvalidValueException, EntityNotFoundException;

    /**
     * Delete the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    void removeRole(Long pRoleId) throws EntityNotFoundException;

    /**
     * Return the {@link List} of {@link ResourcesAccess} on the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @return The {@link List} of {@link ResourcesAccess} on the {@link Role}
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    List<ResourcesAccess> retrieveRoleResourcesAccessList(Long pRoleId) throws EntityNotFoundException;

    /**
     * Set the passed {@link ResourcesAccess} onto the {@link role} of passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @param pResourcesAccessList
     *            The {@link List} of {@link ResourcesAccess} to set
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     * @return The updated {@link Role}
     */
    Role updateRoleResourcesAccess(Long pRoleId, List<ResourcesAccess> pResourcesAccessList)
            throws EntityNotFoundException;

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
     * @return The {@link List} of {@link ProjectUser} for the {@link Role}
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    List<ProjectUser> retrieveRoleProjectUserList(Long pRoleId) throws EntityNotFoundException;

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
     * Return the single <code>default</code> {@link Role}.
     *
     * @return The only {@link Role} with it's <code>default</code> attribute set to <code>true</code>
     */
    Role getDefaultRole();

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
}
