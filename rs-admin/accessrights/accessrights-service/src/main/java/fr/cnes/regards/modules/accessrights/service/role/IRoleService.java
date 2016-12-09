/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.role;

import java.util.List;

import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
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
     * @throws EntityNotFoundException
     *             when no role with passed name could be found
     * @since 1.0-SNAPSHOT
     */
    Role retrieveRole(String pRoleName) throws EntityNotFoundException;

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
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} when no {@link Role} with passed <code>id</code> could be found<br>
     *             {@link EntityInconsistentIdentifierException} Thrown when <code>pRoleId</code> is different from the
     *             id of <code>pUpdatedRole</code><br>
     */
    void updateRole(Long pRoleId, Role pUpdatedRole) throws EntityException;

    /**
     * Create a new {@link Role}.
     *
     * @param pNewRole
     *            The new {@link Role} values
     * @return The created {@link Role}
     * @throws EntityAlreadyExistsException
     *             Thrown if a {@link Role} with same <code>id</code> already exists
     */
    Role createRole(Role pNewRole) throws EntityAlreadyExistsException;

    /**
     * Delete the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @throws EntityOperationForbiddenException
     *             when the updated role is native. Native roles should not be removed
     */
    void removeRole(Long pRoleId) throws EntityOperationForbiddenException;

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
     * Retrieve the inherited roles of the given role. For exemple this method return PUBLIC, REGISTERED_USER and ADMIN
     * for role INSTANCE_ADMIN.
     *
     * @param pRole
     *            role to retrieve inherited roles
     * @return list of {@link Role}
     * @since 1.0-SNAPSHOT
     */
    List<Role> retrieveInheritedRoles(Role pRole);

    /**
     *
     */
    void initDefaultRoles();

}
