/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.role;

import java.util.List;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.InvalidValueException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.OperationForbiddenException;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

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
     * Return the single <code>default</code> {@link Role}.
     *
     * @return The only {@link Role} with it's <code>default</code> attribute set to <code>true</code>
     */
    Role getDefaultRole();

    /**
     * Update the {@link Role} of id <code>pRoleId</code>.
     *
     * @param pRoleId
     *            The {@link Role} <code>id</code>
     * @param pUpdatedRole
     *            The new {@link Role}
     * @throws ModuleEntityNotFoundException
     *             when no {@link Role} with passed <code>id</code> could be found
     * @throws InvalidValueException
     *             Thrown when <code>pRoleId</code> is different from the id of <code>pUpdatedRole</code>
     */
    void updateRole(Long pRoleId, Role pUpdatedRole) throws ModuleEntityNotFoundException, InvalidValueException;

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
     * Delete the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @throws OperationForbiddenException
     *             when the updated role is native. Native roles should not be removed
     */
    void removeRole(Long pRoleId) throws OperationForbiddenException;

    /**
     * Return the {@link List} of {@link ResourcesAccess} on the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @return The {@link List} of {@link ResourcesAccess} on the {@link Role}
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    List<ResourcesAccess> retrieveRoleResourcesAccessList(Long pRoleId) throws ModuleEntityNotFoundException;

    /**
     * Set the passed {@link ResourcesAccess} onto the {@link role} of passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @param pResourcesAccessList
     *            The {@link List} of {@link ResourcesAccess} to set
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     * @return The updated {@link Role}
     */
    Role updateRoleResourcesAccess(Long pRoleId, List<ResourcesAccess> pResourcesAccessList)
            throws ModuleEntityNotFoundException;

    /**
     * Clear the {@link List} of {@link ResourcesAccess} of the {@link Role} with passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role} <code>id</code>
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    void clearRoleResourcesAccess(Long pRoleId) throws ModuleEntityNotFoundException;

    /**
     * Retrieve the {@link List} of {@link ProjectUser} for the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @return The {@link List} of {@link ProjectUser} for the {@link Role}
     * @throws ModuleEntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    List<ProjectUser> retrieveRoleProjectUserList(Long pRoleId) throws ModuleEntityNotFoundException;

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
     * Init the default roles
     */
    void initDefaultRoles();

}
