/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.*;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.event.ResourceAccessEvent;
import fr.cnes.regards.framework.security.event.RoleEvent;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framwork.logbackappender.LogConstants;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link IRoleService} implementation
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 * @author Olivier Rousselot
 */
@Service
@MultitenantTransactional
public class RoleService implements IRoleService, InitializingBean {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleService.class);

    /**
     * Error message
     */
    private static final String NATIVE_ROLE_NOT_REMOVABLE = "Modifications on native roles are forbidden";

    public static final String ROLE_GAINED_ACCESS = LogConstants.SECURITY_MARKER
                                                    + "Role {} has been granted access to these resources: {}";

    public static final String ROLE_LOST_ACCESS = LogConstants.SECURITY_MARKER
                                                  + "Role {} does not have access to the these resources anymore: {}";

    /**
     * CRUD repository managing {@link Role}s. Autowired by Spring.
     */
    private final IRoleRepository roleRepository;

    /**
     * CRUD repository managing {@link ProjectUser}s. Autowired by Spring.
     */
    private final IProjectUserRepository projectUserRepository;

    /**
     * Tenant resolver to access all configured tenant
     */
    private final ITenantResolver tenantResolver;

    /**
     * Runtime tenant resolver
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * AMQP tenant publisher
     */
    private final IPublisher publisher;

    /**
     * Authentication resolver
     */
    private final IAuthenticationResolver authResolver;

    public RoleService(IRoleRepository roleRepository,
                       IProjectUserRepository projectUserRepository,
                       ITenantResolver tenantResolver,
                       IRuntimeTenantResolver runtimeTenantResolver,
                       IPublisher publisher,
                       IAuthenticationResolver authResolver) {
        super();
        this.roleRepository = roleRepository;
        this.projectUserRepository = projectUserRepository;
        this.tenantResolver = tenantResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.publisher = publisher;
        this.authResolver = authResolver;
    }

    /**
     * Post contruct
     */
    @Override
    public void afterPropertiesSet() {
        // Ensure the existence of default roles.
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            // Set working tenant
            try {
                runtimeTenantResolver.forceTenant(tenant);
                initDefaultRoles();
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /**
     * Init default roles for a specified tenant
     */
    @Override
    public void initDefaultRoles() {
        // Init factory to create missing roles
        RoleFactory roleFactory = new RoleFactory().doNotAutoCreateParents();
        // Manage public
        Role publicRole = createOrLoadDefaultRole(roleFactory.createPublic(), null);
        // Manage registered user
        Role registeredUserRole = createOrLoadDefaultRole(roleFactory.createRegisteredUser(), publicRole);
        // Manage exploit
        Role exploitUserRole = createOrLoadDefaultRole(roleFactory.createExploit(), registeredUserRole);
        // Manage admin
        createOrLoadDefaultRole(roleFactory.createAdmin(), exploitUserRole);
        // Manage project admin
        createOrLoadDefaultRole(roleFactory.createProjectAdmin(), null);
        // Manage instance admin
        createOrLoadDefaultRole(roleFactory.createInstanceAdmin(), null);
    }

    /**
     * Create or load a default role
     *
     * @param defaultRole default role to create
     * @param parentRole  parent role to attach
     * @return created role
     */
    private Role createOrLoadDefaultRole(Role defaultRole, Role parentRole) {
        // Retrieve role from database
        Optional<Role> role = roleRepository.findOneByName(defaultRole.getName());
        if (role.isPresent()) {
            return role.get();
        }
        defaultRole.setParentRole(parentRole);
        return roleRepository.save(defaultRole);
    }

    /**
     * @return all roles manageable by current authenticated user
     */
    @Override
    public Set<Role> retrieveRoles() {
        List<Role> manageableRoles = new ArrayList<>();
        for (Role role : roleRepository.findAllDistinctLazy()) {
            // Instance Admin role is only usable by one user:
            // the project admin configured at install, so we have not to send it back to the front
            if (DefaultRole.INSTANCE_ADMIN.name().equals(role.getName())) {
                continue;
            }

            try {
                // Check if current user can manage this role
                canManageRole(role);
                // Add manageable role
                manageableRoles.add(role);

            } catch (EntityOperationForbiddenException e) {
                LOGGER.debug("Do not send role {} cause authenticated user cannot manage it!", role.getName());
                LOGGER.trace(e.getMessage(), e);
            }
        }

        Set<Role> sortedRole = new TreeSet<>(new RoleComparator(this));
        sortedRole.addAll(manageableRoles);
        return sortedRole;

    }

    @Override
    public Role createRole(Role role) throws EntityException {
        if (existByName(role.getName())) {
            throw new EntityAlreadyExistsException(role.getName());
        }

        if ((role.getParentRole() == null) || (role.getParentRole().getName() == null)) {
            throw new EntityException("A parent role is required to create a new role.");
        }

        // If parent role is a native role. Copy resources from the parent role.
        Optional<Role> roleOpt = roleRepository.findOneByName(role.getParentRole().getName());
        if (!roleOpt.isPresent()) {
            throw new EntityNotFoundException(role.getParentRole().getName(), Role.class);
        }

        Role parentRole = roleOpt.get();
        Role newCreatedRole;
        if (parentRole.isNative()) {
            newCreatedRole = roleRepository.save(role);
            newCreatedRole.setPermissions(Sets.newHashSet(parentRole.getPermissions()));
        } else {
            // Retrieve parent native role of the given parent role.
            if (!parentRole.getParentRole().isNative()) {
                throw new EntityException("There is no native parent associated to the given parent role "
                                          + parentRole.getName());
            }

            newCreatedRole = new Role(role.getName(), parentRole.getParentRole());
            if (role.getAuthorizedAddresses() != null) {
                newCreatedRole.setAuthorizedAddresses(role.getAuthorizedAddresses());
            }
            newCreatedRole = roleRepository.save(newCreatedRole);
            newCreatedRole.setPermissions(Sets.newHashSet(parentRole.getPermissions()));
        }
        // Save permissions
        return saveAndPublish(newCreatedRole);
    }

    @Override
    public Role retrieveRole(String roleName) throws EntityNotFoundException {
        return roleRepository.findOneByName(roleName)
                             .orElseThrow(() -> new EntityNotFoundException(roleName, Role.class));
    }

    /**
     * Retrieve a role
     */
    @Override
    public Role retrieveRole(Long roleId) throws EntityNotFoundException {
        Optional<Role> roleOpt = roleRepository.findById(roleId);
        if (!roleOpt.isPresent()) {
            throw new EntityNotFoundException(roleId, Role.class);
        }
        return roleOpt.get();
    }

    @Override
    public Role updateRole(String roleName, Role updatedRole) throws EntityException {
        if (!roleName.equals(updatedRole.getName())) {
            throw new EntityInconsistentIdentifierException(roleName, updatedRole.getName(), Role.class);
        }
        Role beforeUpdate = roleRepository.findByName(roleName)
                                          .orElseThrow(() -> new EntityNotFoundException(roleName, Role.class));
        if (beforeUpdate.isNative() && (((beforeUpdate.getParentRole() == null) && (updatedRole.getParentRole()
                                                                                    != null)) || (!Objects.equal(
            beforeUpdate.getParentRole(),
            updatedRole.getParentRole())))) {
            throw new EntityOperationForbiddenException(roleName, Role.class, "Native role parent cannot be changed");
        }
        Role updated = updatedRole;
        if (!beforeUpdate.isNative() && !beforeUpdate.getParentRole().equals(updatedRole.getParentRole())) {
            // if this is a custom role and and the parent has changed: we set the resources of the custom role to the
            // one of its new parent
            // so lets get its parent with its permissions
            Role newParent = roleRepository.findOneById(updatedRole.getParentRole().getId());
            updated = updateRoleResourcesAccess(beforeUpdate.getId(), newParent.getPermissions());
        }
        return saveAndPublish(updated);
    }

    @Override
    public void removeRole(Long roleId) throws EntityException {
        Optional<Role> previousOpt = roleRepository.findById(roleId);
        if ((previousOpt.isPresent()) && previousOpt.get().isNative()) {
            throw new EntityOperationForbiddenException(roleId.toString(), Role.class, NATIVE_ROLE_NOT_REMOVABLE);
        } else if (!previousOpt.isPresent()) {
            throw new EntityNotFoundException(roleId, Role.class);
        } else {
            deleteAndPublish(previousOpt.get());
        }
    }

    @Override
    public void removeRole(String roleName) throws EntityException {
        Optional<Role> role = roleRepository.findOneByName(roleName);
        if (!role.isPresent()) {
            throw new EntityNotFoundException(roleName, Role.class);
        } else if (role.get().isNative()) {
            throw new EntityOperationForbiddenException(roleName, Role.class, NATIVE_ROLE_NOT_REMOVABLE);
        } else {
            deleteAndPublish(role.get());
        }

    }

    /**
     * Each role contains all its permission.
     */
    @Override
    public Set<ResourcesAccess> retrieveRoleResourcesAccesses(Long roleId) throws EntityNotFoundException {
        if (!existRole(roleId)) {
            throw new EntityNotFoundException(roleId.toString(), Role.class);
        }
        Role role = roleRepository.findOneById(roleId);
        LOGGER.debug("Retrieving resource accesses for role \"{}\"", role.getName());
        Set<ResourcesAccess> accesses = role.getPermissions();
        LOGGER.debug("{} resource accesses found for role \"{}\"", accesses.size(), role.getName());
        return accesses;
    }

    @Override
    public Role updateRoleResourcesAccess(Long roleId, Set<ResourcesAccess> resourcesAccesses) throws EntityException {
        Optional<Role> roleOpt = roleRepository.findById(roleId);
        if (!roleOpt.isPresent()) {
            throw new EntityNotFoundException(roleId.toString(), Role.class);
        }
        Role role = roleOpt.get();
        Set<ResourcesAccess> permissions = role.getPermissions();

        // extract which one are to be removed
        Set<ResourcesAccess> toBeRemoved = new HashSet<>(permissions);
        toBeRemoved.removeAll(resourcesAccesses);
        // remove them by handling descendancy
        removeResourcesAccesses(role, toBeRemoved.toArray(new ResourcesAccess[0]));

        // extract which ResourcesAccess is really new
        Set<ResourcesAccess> newOnes = new HashSet<>(resourcesAccesses);
        newOnes.removeAll(permissions);
        // add the ResourceAccesses by handling descendancy
        addResourceAccesses(role, newOnes.toArray(new ResourcesAccess[0]));

        return role;
    }

    @Override
    public void addResourceAccesses(Long roleId, ResourcesAccess... newOnes) throws EntityException {
        Role role = roleRepository.findOneById(roleId);
        if (role == null) {
            throw new EntityNotFoundException(roleId, Role.class);
        }
        addResourceAccesses(role, newOnes);
    }

    /**
     * Check authenticated user can manage target role resource accesses.
     *
     * @param role target role to manage
     * @throws EntityOperationForbiddenException if operation forbidden
     */
    private void canManageRole(Role role) throws EntityOperationForbiddenException {

        String securityRole = authResolver.getRole();

        if (securityRole == null) {
            LOGGER.debug("No security role set. Internal call granted");
            return;
        }

        // System role always granted
        if (RoleAuthority.isSysRole(securityRole)
            || RoleAuthority.isInstanceAdminRole(securityRole)
            || RoleAuthority.isProjectAdminRole(securityRole)) {
            LOGGER.debug("Priviledged call granted");
            return;
        }

        Optional<Role> currentRole = roleRepository.findByName(securityRole);

        // Compare with native role
        Role refRole = role;
        if (!role.isNative()) {
            refRole = role.getParentRole();
        }

        // Check if target role is hierarchically inferior so current user can alter it
        if (currentRole.isPresent() && isHierarchicallyInferior(refRole, currentRole.get())) {

            LOGGER.debug("User with role {} can add resource accesses to role {}",
                         currentRole.get().getName(),
                         role.getName());

        } else if (currentRole.isPresent()) {
            String message = "A user can only add resources on role hierarchically inferior to its own.";
            LOGGER.error(message);
            throw new EntityOperationForbiddenException(message);
        } else {
            String message = String.format("Unknown role %s.", securityRole);
            LOGGER.error(message);
            throw new EntityOperationForbiddenException(message);
        }
    }

    /**
     * Check authenticated user can add specified resources.
     *
     * @param newOnes resources to add only if they are a subset of use ones.
     */
    private void canAddResourceAccesses(ResourcesAccess... newOnes) throws EntityOperationForbiddenException {
        String securityRole = authResolver.getRole();

        if (securityRole == null) {
            LOGGER.debug("No security role set. Internal call granted");
            return;
        }

        // System role always granted
        if (RoleAuthority.isSysRole(securityRole)
            || RoleAuthority.isInstanceAdminRole(securityRole)
            || RoleAuthority.isProjectAdminRole(securityRole)) {
            LOGGER.debug("Priviledged call granted");
            return;
        }

        Optional<Role> currentRole = roleRepository.findOneByName(securityRole);

        Set<ResourcesAccess> resourcesToAdd = new HashSet<>();
        Collections.addAll(resourcesToAdd, newOnes);

        // Check if current user has itself the resource accesses he wants to add
        if (currentRole.isPresent() && currentRole.get().getPermissions().containsAll(resourcesToAdd)) {
            LOGGER.debug("User with role {} can add specified resource accesses", currentRole.get().getName());
        } else if (currentRole.isPresent()) {
            String message = "A user can only add resources he has yet. One or more resources doesn't match this requirement.";
            LOGGER.error(message);
            throw new EntityOperationForbiddenException(message);
        } else {
            String message = String.format("Unknown role %s.", securityRole);
            LOGGER.error(message);
            throw new EntityOperationForbiddenException(message);
        }
    }

    /**
     * Add a set of accesses to a role and its descendants(according to PM003)
     *
     * @param role    role on which the modification has been made
     * @param newOnes accesses to add
     * @throws EntityOperationForbiddenException if error occurs!
     */
    private void addResourceAccesses(Role role, ResourcesAccess... newOnes) throws EntityOperationForbiddenException {

        // Check if current user can add resources to specified role
        canManageRole(role);
        canAddResourceAccesses(newOnes);

        if (role.isNative()) {
            // If native role, propagate added resources to all descendants to maintain consistency
            addAndPropagate(role, newOnes);
        } else {
            // Add resource access to current role and manage its parent
            addAndManageParent(role, newOnes);
        }
    }

    /**
     * Add accesses on all inheriting roles
     *
     * @param role              role to manage
     * @param resourcesAccesses accesses to add
     */
    private void addAndPropagate(Role role, ResourcesAccess... resourcesAccesses) {
        // Add accesses
        boolean changed = role.getPermissions().addAll(Sets.newHashSet(resourcesAccesses));
        // Save changes
        roleRepository.save(role);
        if (changed) {
            StringJoiner sj = new StringJoiner(", ");
            Arrays.stream(resourcesAccesses).forEach(ra -> sj.add(ra.getVerb() + "@" + ra.getResource()));
            LOGGER.info(ROLE_GAINED_ACCESS, role.getName(), sj.toString());
            publishResourceAccessEvent(role.getName(), resourcesAccesses);
        }
        // Retrieve its descendants
        Set<Role> sons = roleRepository.findByParentRoleName(role.getName());
        // Propagate
        sons.forEach(son -> addAndPropagate(son, resourcesAccesses));
    }

    /**
     * Add accesses on current role only and change parent if required to maintain consistency
     *
     * @param role              role to manage
     * @param resourcesAccesses accesses to add
     * @throws EntityOperationForbiddenException if parent cannot be found
     */
    private void addAndManageParent(Role role, ResourcesAccess... resourcesAccesses)
        throws EntityOperationForbiddenException {
        // Add accesses
        boolean changed = role.getPermissions().addAll(Sets.newHashSet(resourcesAccesses));
        // Save changes
        roleRepository.save(role);
        if (changed) {
            StringJoiner sj = new StringJoiner(", ");
            Arrays.stream(resourcesAccesses).forEach(ra -> sj.add(ra.getVerb() + "@" + ra.getResource()));
            LOGGER.info(ROLE_GAINED_ACCESS, role.getName(), sj.toString());
            publishResourceAccessEvent(role.getName(), resourcesAccesses);
        }
        // Change parent if required
        manageParentFromAdmin(role);
    }

    /**
     * Inform security starter an (or many) access(es) changed
     *
     * @param resourcesAccesses resource accesses that have changed
     */
    private void publishResourceAccessEvent(String roleName, ResourcesAccess... resourcesAccesses) {

        // Compute concerned microservices
        Set<String> microservices = new HashSet<>();
        for (ResourcesAccess ra : resourcesAccesses) {
            microservices.add(ra.getMicroservice());
        }
        // Publish an event for each concerned microservice
        for (String microservice : microservices) {
            ResourceAccessEvent raEvent = new ResourceAccessEvent(microservice, roleName);
            publisher.publish(raEvent);
        }
    }

    @Override
    public void clearRoleResourcesAccess(Long roleId) throws EntityNotFoundException {
        Optional<Role> roleOpt = roleRepository.findById(roleId);
        if (!roleOpt.isPresent()) {
            throw new EntityNotFoundException(roleId.toString(), Role.class);
        }
        roleOpt.get().getPermissions().clear();
        roleRepository.save(roleOpt.get());
    }

    @Override
    public Page<ProjectUser> retrieveRoleProjectUserList(Long roleId, Pageable pageable)
        throws EntityNotFoundException {
        Optional<Role> roleOpt = roleRepository.findById(roleId);
        if (!roleOpt.isPresent()) {
            throw new EntityNotFoundException(roleId.toString(), Role.class);
        }

        Set<Role> roles = retrieveInheritedRoles(roleOpt.get());
        roles.add(roleOpt.get());
        Set<String> roleNames = roles.stream().map(Role::getName).collect(Collectors.toSet());
        return projectUserRepository.findByRoleNameIn(roleNames, pageable);
    }

    @Override
    public Page<ProjectUser> retrieveRoleProjectUserList(String roleName, Pageable pageable)
        throws EntityNotFoundException {
        Optional<Role> role = roleRepository.findOneByName(roleName);
        if (!role.isPresent()) {
            throw new EntityNotFoundException(roleName, Role.class);
        }
        Set<Role> roles = retrieveInheritedRoles(role.get());
        roles.add(role.get());
        Set<String> roleNames = roles.stream().map(Role::getName).collect(Collectors.toSet());
        return projectUserRepository.findByRoleNameIn(roleNames, pageable);
    }

    @Override
    public boolean isCurrentRoleSuperiorTo(String roleName) throws EntityNotFoundException {
        String securityRole = authResolver.getRole();

        if (RoleAuthority.isSysRole(securityRole)) {
            return true;
        } else {
            Role providedRole;
            Role currentRole;
            try {
                providedRole = retrieveRole(roleName);
                currentRole = retrieveRole(securityRole);
                return securityRole.equals(DefaultRole.PROJECT_ADMIN.toString()) || isHierarchicallyInferior(
                    providedRole,
                    currentRole);
            } catch (EntityNotFoundException e) {
                LOGGER.error("Failed to compare the current role {} with {}, as one of them does not exist",
                             securityRole,
                             roleName,
                             e);
                throw e;
            }
        }
    }

    @Override
    public boolean existRole(Long roleId) {
        return roleRepository.existsById(roleId);
    }

    @Override
    public boolean existRole(Role role) {
        return this.existRole(role.getId());
    }

    @Override
    public boolean existByName(String name) {
        return roleRepository.findOneByName(name).isPresent();
    }

    @Override
    public Role getDefaultRole() {
        return roleRepository.findOneByIsDefault(true).orElse(getRolePublic());
    }

    /**
     * Determines if first role is inferior to second role.
     *
     * @param first  role that should be inferior to second
     * @param second role that should not be inferior to first
     * @return FALSE if: <br/>
     * <ul>
     * <li>second is null</li>
     * <li>first is project admin</li>
     * <li>first equals second</li>
     * <li>first has less privilege than second</li>
     * </ul>
     */
    @Override
    public boolean isHierarchicallyInferior(Role first, Role second) {
        // we consider that null is hierarchically inferior to anyone
        if (first == null) {
            return true;
        }
        if (second == null) {
            return false;
        }
        // we treat project admin by hand as it doesn't really have a hierarchy
        if (RoleAuthority.isProjectAdminRole(first.getName())) {
            return false;
        }
        if (RoleAuthority.isInstanceAdminRole(second.getName())) {
            return true;
        }
        if (RoleAuthority.isProjectAdminRole(second.getName())) {
            return true;
        }
        // case of myself: we are not strictly inferior to ourselves
        if (Objects.equal(second, first)) {
            return false;
        }
        // now lets treat common cases
        RoleLineageAssembler roleLineageAssembler = new RoleLineageAssembler();
        List<Role> ancestors = roleLineageAssembler.of(second).get();
        try (Stream<Role> stream = ancestors.stream()) {
            if (first.isNative()) {
                // if the role is native, then it is into the lineage so we can look for it
                String roleName = first.getName();
                return stream.anyMatch(r -> r.getName().equals(roleName));
            } else {
                // if the role is not a native one, then we need to look for its parent(which is native).
                String parent = first.getParentRole().getName();
                return stream.anyMatch(r -> r.getName().equals(parent));
            }
        }
    }

    /**
     * @return the role public. Create it if not found
     */
    public Role getRolePublic() {
        RoleFactory factory = new RoleFactory();
        return roleRepository.findOneByName(DefaultRole.PUBLIC.toString())
                             .orElseGet(() -> roleRepository.save(factory.createPublic()));
    }

    @Override
    public Set<Role> retrieveInheritedRoles(Role inRole) {
        Set<Role> results = new HashSet<>();
        Set<Role> inheritedRoles = roleRepository.findByParentRoleName(inRole.getName());
        if (inheritedRoles != null) {

            results.addAll(inheritedRoles);

            for (Role role : inheritedRoles) {
                retrieveInheritedRoles(role).forEach(results::add);
            }
        }
        return results;
    }

    /**
     * Remove resource accesses from a role
     */
    @Override
    public void removeResourcesAccesses(String roleName, ResourcesAccess... resourcesAccesses) throws EntityException {

        Optional<Role> roleOpt = roleRepository.findByName(roleName);

        if (!roleOpt.isPresent()) {
            throw new EntityNotFoundException(roleName, Role.class);
        }

        // Check if current user can remove resources to specified role
        canManageRole(roleOpt.get());

        removeResourcesAccesses(roleOpt.get(), resourcesAccesses);
    }

    private void removeResourcesAccesses(Role role, ResourcesAccess[] resourcesAccesses)
        throws EntityOperationForbiddenException {
        // If PROJECT_ADMIN, nothing to do / removal forbidden
        if (role.getName().equals(DefaultRole.PROJECT_ADMIN.toString())) {
            throw new EntityOperationForbiddenException(role.getName(),
                                                        Role.class,
                                                        "Removing resource accesses from role PROJECT_ADMIN is forbidden!");
        }

        // Apply changes and publish changes inside removeAndPropagate or RemoveAndManageParent so we are sure that
        // every updates has been published
        if (role.isNative()) {
            // If native role, propagate removal to native ascendants to maintain consistency
            removeAndPropagate(role, resourcesAccesses);
            // Check non native role parents
            manageParentRoles();
        } else {
            // Else only remove accesses from role and change parent if required (may throw an exception if at least
            // public role cannot be the parent)
            removeAndManageParent(role, resourcesAccesses);
        }
    }

    /**
     * Remove accesses on all inherited roles
     *
     * @param role              role to manage
     * @param resourcesAccesses accesses to remove
     */
    private void removeAndPropagate(Role role, ResourcesAccess... resourcesAccesses) {

        if (role != null) {
            // Remove accesses
            boolean changed = role.getPermissions().removeAll(Sets.newHashSet(resourcesAccesses));
            // Save changes
            roleRepository.save(role);
            // publish event
            if (changed) {
                StringJoiner sj = new StringJoiner(", ");
                Arrays.stream(resourcesAccesses).forEach(ra -> sj.add(ra.getVerb() + "@" + ra.getResource()));
                LOGGER.info(ROLE_LOST_ACCESS, role.getName(), sj.toString());
                publishResourceAccessEvent(role.getName(), resourcesAccesses);
            }
            // Propagate
            removeAndPropagate(role.getParentRole(), resourcesAccesses);
        }
    }

    /**
     * Consider all non native roles to update its native parent after native role changes
     */
    private void manageParentRoles() throws EntityOperationForbiddenException {

        Set<Role> roles = retrieveRoles();
        for (Role role : roles) {
            if (!role.isNative()) {
                manageParentFromAdmin(role);
            }
        }
    }

    /**
     * Remove accesses on current role only and change parent if required to maintain consistency
     *
     * @param role              role to manage
     * @param resourcesAccesses accesses to remove
     * @throws EntityOperationForbiddenException if parent cannot be found
     */
    private void removeAndManageParent(Role role, ResourcesAccess... resourcesAccesses)
        throws EntityOperationForbiddenException {
        // Remove accesses
        boolean changed = role.getPermissions().removeAll(Sets.newHashSet(resourcesAccesses));
        // Save changes
        roleRepository.save(role);
        // publish event
        if (changed) {
            StringJoiner sj = new StringJoiner(", ");
            Arrays.stream(resourcesAccesses).forEach(ra -> sj.add(ra.getVerb() + "@" + ra.getResource()));
            LOGGER.info(ROLE_LOST_ACCESS, role.getName(), sj.toString());
            publishResourceAccessEvent(role.getName(), resourcesAccesses);
        }
        // Change parent if required
        manageParent(role, role.getParentRole());
    }

    /**
     * Found a consistent parent for the current role starting with {@link DefaultRole#ADMIN}, highest available
     * inherited parent.
     *
     * @param role role to consider
     * @throws EntityOperationForbiddenException if no parent role matches
     */
    private void manageParentFromAdmin(Role role) throws EntityOperationForbiddenException {

        // Retrieve default admin role
        Role adminRole = roleRepository.findOneByName(DefaultRole.ADMIN.name()).get();
        // Manage parent
        manageParent(role, adminRole);
    }

    /**
     * Found a consistent parent for the current role
     *
     * @param role       role to consider
     * @param parentRole parent role candidate
     * @throws EntityOperationForbiddenException if no parent role matches
     */
    private void manageParent(Role role, Role parentRole) throws EntityOperationForbiddenException {

        // role must not be null
        Assert.notNull(role, "Role cannot be null");

        // if parent role is null, even public role cannot be the parent of the role
        // throw exception
        // a role must have a parent and cannot have less accesses than public
        if (parentRole == null) {
            String message = String.format(
                "Role %s cannot have less accesses than public role. Accesses removal cancelled.",
                role.getName());
            LOGGER.error(message);
            throw new EntityOperationForbiddenException(message);
        }

        // Check if role is consistent with its parent
        // The parent cannot have more accesses!
        Set<ResourcesAccess> rolePermissions = role.getPermissions();
        Set<ResourcesAccess> parentPermissions = parentRole.getPermissions();

        if (rolePermissions.containsAll(parentPermissions)) {
            // Set parent
            role.setParentRole(parentRole);
            // Save changes
            roleRepository.save(role);
        } else {
            manageParent(role, parentRole.getParentRole());
        }
    }

    @Override
    public Set<Role> retrieveBorrowableRoles() {

        Set<Role> borrowablesRoles = new TreeSet<>(new RoleComparator(this));

        String email = authResolver.getUser();
        Optional<ProjectUser> optionnalUser = projectUserRepository.findOneByEmail(email);
        if (!optionnalUser.isPresent()) {
            return borrowablesRoles;
        }

        ProjectUser user = optionnalUser.get();
        // get Original Role of the user
        Role originalRole = user.getRole();
        List<String> roleNamesAllowedToBorrow = Lists.newArrayList(DefaultRole.ADMIN.toString(),
                                                                   DefaultRole.PROJECT_ADMIN.toString());
        // It is impossible to borrow a role if your original role is not ADMIN or PROJECT_ADMIN or one of their sons
        // To simplify client interraction we returned the actual role of the user even if this role is not borowable.
        // The regards frontend use this role to calculate user ihm rights based on his role.
        if (!roleNamesAllowedToBorrow.contains(originalRole.getName()) && ((originalRole.getParentRole() == null)
                                                                           || !roleNamesAllowedToBorrow.contains(
            originalRole.getParentRole().getName()))) {
            return Sets.newHashSet(originalRole);
        }
        // get ascendants of the original Role
        if (originalRole.getParentRole() != null) {
            // only adds the ascendants of my role's parent as my role's brotherhood is not part of my role's ascendants
            borrowablesRoles.addAll(getAscendants(roleRepository.findOneById(originalRole.getParentRole().getId())));
        } else {
            // handle ProjectAdmin by considering that ADMIN is its parent(projectAdmin is not considered admin's
            // son so no resources accesses are added or removed from him but has to be considered for role borrowing)
            if (originalRole.getName().equals(DefaultRole.PROJECT_ADMIN.toString())) {
                borrowablesRoles.addAll(getAscendants(originalRole));
            } // INSTANCE_ADMIN and PUBLIC do not have ascendants
        }
        // add my original role because i can always borrow my own role
        borrowablesRoles.add(originalRole);
        return borrowablesRoles;
    }

    @Override
    public Set<Role> getDescendants(Role role) {
        // Role entity hierarchy being inverted, parent of ADMIN is REGISTERED_USER
        // so if we want the descendants of REGISTERED_USER, we need to seek for role which parent is REGISTERED_USER and so on.
        Set<Role> children = roleRepository.findByParentRoleName(role.getName());
        Set<Role> descendants = Sets.newHashSet(children);
        for (Role child : children) {
            descendants.addAll(getDescendants(child));
        }
        if (children.isEmpty()) {
            // More over, PROJECT_ADMIN is the descendant of all role, but is not connected to them in a conventional way so lets add him.
            descendants.add(roleRepository.findOneByName(DefaultRole.PROJECT_ADMIN.toString()).get());
        }
        // lets add the role for which we are looking its descendants too
        descendants.add(role);
        return descendants;
    }

    @Override
    public Set<Role> getAscendants(Role role) {
        Set<Role> ascendants = Sets.newHashSet(role);
        // if pRole doesn't have parent then it's finished
        Role parent = role.getParentRole();
        if (parent == null) {
            // except if it's PROJECT_ADMIN
            if (role.getName().equals(DefaultRole.PROJECT_ADMIN.toString())) {
                parent = roleRepository.findOneByName(DefaultRole.ADMIN.toString()).get();
            } else {
                return ascendants;
            }
        } else {
            // We need to load parent from repository to load his permissions.
            parent = roleRepository.findOneById(parent.getId());
        }
        // otherwise lets get pRole's parent and look for his children: Brotherhood
        ascendants.addAll(roleRepository.findByParentRoleName(parent.getName()));
        // now lets add the ascendants of parent
        ascendants.addAll(getAscendants(parent));
        return ascendants;
    }

    @Override
    public Set<Role> retrieveRolesWithResource(Long resourceId) {
        return roleRepository.findByPermissionsId(resourceId);
    }

    /**
     * Inform security starter of a role change
     *
     * @param role role
     */
    private void publishRoleEvent(Role role) {
        RoleEvent roleEvent = new RoleEvent();
        roleEvent.setRole(role.getName());
        publisher.publish(roleEvent);
    }

    private Role saveAndPublish(Role role) {
        Role savedRole = roleRepository.save(role);
        publishRoleEvent(role);
        return savedRole;
    }

    private void deleteAndPublish(Role role) {
        roleRepository.deleteById(role.getId());
        publishRoleEvent(role);
    }

    @Override
    public Optional<Role> getCurrentRole() {
        return roleRepository.findByName(authResolver.getRole());
    }
}
