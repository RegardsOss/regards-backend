/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.role;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.listener.exception.ListenerExecutionFailedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionFailed;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.event.ResourceAccessEvent;
import fr.cnes.regards.framework.security.event.ResourceAccessInit;
import fr.cnes.regards.framework.security.event.RoleEvent;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.SecurityUtils;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.projects.RoleFactory;
import fr.cnes.regards.modules.accessrights.domain.projects.RoleLineageAssembler;

/**
 * {@link IRoleService} implementation
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 */
@Service
@MultitenantTransactional
public class RoleService implements IRoleService {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleService.class);

    /**
     * Error message
     */
    private static final String NATIVE_ROLE_NOT_REMOVABLE = "Modifications on native roles are forbidden";

    /**
     * Current microservice name
     */
    private final String microserviceName;

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
     * AMQP instance message subscriber
     */
    private final IInstanceSubscriber instanceSubscriber;

    /**
     * AMQP instance message publisher
     */
    private final IInstancePublisher instancePublisher;

    /**
     * AMQP tenant publisher
     */
    private final IPublisher publisher;

    public RoleService(@Value("${spring.application.name}") final String pMicroserviceName,
            final IRoleRepository pRoleRepository, final IProjectUserRepository pProjectUserRepository,
            final ITenantResolver pTenantResolver, final IRuntimeTenantResolver pRuntimeTenantResolver,
            final IInstanceSubscriber pInstanceSubscriber, final IInstancePublisher instancePublisher,
            final IPublisher pPublisher) {
        super();
        roleRepository = pRoleRepository;
        projectUserRepository = pProjectUserRepository;
        tenantResolver = pTenantResolver;
        runtimeTenantResolver = pRuntimeTenantResolver;
        microserviceName = pMicroserviceName;
        instanceSubscriber = pInstanceSubscriber;
        this.instancePublisher = instancePublisher;
        publisher = pPublisher;
    }

    @PostConstruct
    public void init() {
        // Ensure the existence of default roles. If not, add them from their bean definition in defaultRoles.xml
        for (final String tenant : tenantResolver.getAllActiveTenants()) {
            initDefaultRoles(tenant);
        }
        instanceSubscriber.subscribeTo(TenantConnectionReady.class, new TenantConnectionReadyEventHandler());
    }

    /**
     * Handle a new tenant connection to initialize default roles
     *
     * @author Marc Sordi
     *
     */
    private class TenantConnectionReadyEventHandler implements IHandler<TenantConnectionReady> {

        /**
         * Initialize default roles in the new project connection
         *
         * @see fr.cnes.regards.framework.amqp.domain.IHandler#handle(fr.cnes.regards.framework.amqp.domain.TenantWrapper)
         * @since 1.0-SNAPSHOT
         */
        @Override
        public void handle(final TenantWrapper<TenantConnectionReady> pWrapper) {
            if (microserviceName.equals(pWrapper.getContent().getMicroserviceName())) {

                // Retrieve new tenant to manage
                String tenant = pWrapper.getContent().getTenant();
                try {
                    // Init default role for this tenant
                    initDefaultRoles(tenant);
                    // Populate default roles with resources informing security starter to process
                    publisher.publish(new ResourceAccessInit());
                } catch (ListenerExecutionFailedException e) {
                    LOGGER.error("Cannot initialize connection  for tenant " + tenant, e);
                    instancePublisher.publish(new TenantConnectionFailed(tenant, microserviceName));
                }
            }
        }
    }

    /**
     * Init default roles for a specified tenant
     *
     * @param tenant
     *            tenant
     */
    private void initDefaultRoles(String tenant) {

        // Init factory to create missing roles
        RoleFactory roleFactory = new RoleFactory().doNotAutoCreateParents();

        // Set working tenant
        runtimeTenantResolver.forceTenant(tenant);
        // Manage public
        Role publicRole = createOrLoadDefaultRole(roleFactory.createPublic(), null);
        // Manage registered user
        Role registeredUserRole = createOrLoadDefaultRole(roleFactory.createRegisteredUser(), publicRole);
        // Manage admin
        createOrLoadDefaultRole(roleFactory.createAdmin(), registeredUserRole);
        // Manage project admin
        createOrLoadDefaultRole(roleFactory.createProjectAdmin(), null);
        // Manage instance admin
        createOrLoadDefaultRole(roleFactory.createInstanceAdmin(), null);
    }

    /**
     * Create or load a default role
     *
     * @param defaultRole
     *            default role to create
     * @param parentRole
     *            parent role to attach
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

    @Override
    public Set<Role> retrieveRoles() {
        try (Stream<Role> stream = StreamSupport.stream(roleRepository.findAllDistinctLazy().spliterator(), true)) {
            return stream.collect(Collectors.toSet());
        }
    }

    @Override
    public Role createRole(final Role role) throws EntityException {
        if (existByName(role.getName())) {
            throw new EntityAlreadyExistsException(role.getName());
        }

        if ((role.getParentRole() == null) || (role.getParentRole().getName() == null)) {
            throw new EntityException("A parent role is required to create a new role.");
        }

        // If parent role is a native role. Copy resources from the parent role.
        final Optional<Role> roleOpt = roleRepository.findOneByName(role.getParentRole().getName());
        if (!roleOpt.isPresent()) {
            throw new EntityNotFoundException(role.getParentRole().getName(), Role.class);
        }

        final Role parentRole = roleOpt.get();
        Role newCreatedRole;
        if (parentRole.isNative()) {
            newCreatedRole = roleRepository.save(role);
            newCreatedRole.setPermissions(Sets.newHashSet(parentRole.getPermissions()));
        } else {
            // Retrieve parent native role of the given parent role.
            if (!parentRole.getParentRole().isNative()) {
                throw new EntityException(
                        "There is no native parent associated to the given parent role " + parentRole.getName());
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
    public Role retrieveRole(final String pRoleName) throws EntityNotFoundException {
        return roleRepository.findOneByName(pRoleName)
                .orElseThrow(() -> new EntityNotFoundException(pRoleName, Role.class));
    }

    @Override
    public Role updateRole(final String pRoleName, final Role pUpdatedRole) throws EntityException {
        if (!pRoleName.equals(pUpdatedRole.getName())) {
            throw new EntityInconsistentIdentifierException(pRoleName, pUpdatedRole.getName(), Role.class);
        }
        if (!existRole(pUpdatedRole)) {
            throw new EntityNotFoundException(pRoleName, Role.class);
        }
        return saveAndPublish(pUpdatedRole);
    }

    @Override
    public void removeRole(final Long pRoleId) throws EntityException {
        final Role previous = roleRepository.findOne(pRoleId);
        if ((previous != null) && previous.isNative()) {
            throw new EntityOperationForbiddenException(pRoleId.toString(), Role.class, NATIVE_ROLE_NOT_REMOVABLE);
        } else if (previous == null) {
            throw new EntityNotFoundException(pRoleId, Role.class);
        } else {
            deleteAndPublish(previous);
        }
    }

    @Override
    public void removeRole(final String pRoleName) throws EntityException {
        final Optional<Role> role = roleRepository.findOneByName(pRoleName);
        if (!role.isPresent()) {
            throw new EntityNotFoundException(pRoleName, Role.class);
        } else if (role.get().isNative()) {
            throw new EntityOperationForbiddenException(pRoleName, Role.class, NATIVE_ROLE_NOT_REMOVABLE);
        } else {
            deleteAndPublish(role.get());
        }

    }

    /**
     * Each role contains all its permission.
     *
     * @see PM003
     */
    @Override
    public Set<ResourcesAccess> retrieveRoleResourcesAccesses(final Long pRoleId) throws EntityNotFoundException {
        if (!existRole(pRoleId)) {
            throw new EntityNotFoundException(pRoleId.toString(), Role.class);
        }
        final Role role = roleRepository.findOneById(pRoleId);
        return role.getPermissions();
    }

    @Override
    public Role updateRoleResourcesAccess(final Long pRoleId, final Set<ResourcesAccess> pResourcesAccesses)
            throws EntityException {
        if (!existRole(pRoleId)) {
            throw new EntityNotFoundException(pRoleId.toString(), Role.class);
        }
        final Role role = roleRepository.findOne(pRoleId);
        final Set<ResourcesAccess> permissions = role.getPermissions();

        // extract which one are to be removed
        final Set<ResourcesAccess> toBeRemoved = new HashSet<>(permissions);
        toBeRemoved.removeAll(pResourcesAccesses);
        // remove them by handling descendancy
        removeResourcesAccesses(role.getName(), toBeRemoved.toArray(new ResourcesAccess[toBeRemoved.size()]));

        // extract which ResourcesAccess is really new
        final Set<ResourcesAccess> newOnes = new HashSet<>(pResourcesAccesses);
        newOnes.removeAll(permissions);
        // add the ResourceAccesses by handling descendancy
        addResourceAccesses(role, newOnes.toArray(new ResourcesAccess[newOnes.size()]));

        return role;
    }

    @Override
    public void addResourceAccesses(final Long pRoleId, final ResourcesAccess... pNewOnes) throws EntityException {
        final Role role = roleRepository.findOneById(pRoleId);
        if (role == null) {
            throw new EntityNotFoundException(pRoleId, Role.class);
        }
        addResourceAccesses(role, pNewOnes);
    }

    /**
     * Add a set of accesses to a role and its descendants(according to PM003)
     *
     * @param pRole
     *            role on which the modification has been made
     * @param pNewOnes
     *            accesses to add
     * @throws EntityOperationForbiddenException if error occurs!
     */
    private void addResourceAccesses(final Role pRole, final ResourcesAccess... pNewOnes)
            throws EntityOperationForbiddenException {

        if (pRole.isNative()) {
            // If native role, propagate added resources to all descendants to maintain consistency
            addAndPropagate(pRole, pNewOnes);
        } else {
            // Add resource access to current role and manage its parent
            addAndManageParent(pRole, pNewOnes);
        }

        // Publish changes
        publishResourceAccessEvent(pNewOnes);
    }

    /**
     * Add accesses on all inheriting roles
     *
     * @param pRole role to manage
     * @param pResourcesAccesses accesses to add
     */
    private void addAndPropagate(final Role pRole, final ResourcesAccess... pResourcesAccesses) {
        // Add accesses
        pRole.getPermissions().addAll(Sets.newHashSet(pResourcesAccesses));
        // Save changes
        roleRepository.save(pRole);
        // Retrieve its descendants
        Set<Role> sons = roleRepository.findByParentRoleName(pRole.getName());
        // Propagate
        sons.forEach(son -> addAndPropagate(son, pResourcesAccesses));
    }

    /**
     * Add accesses on current role only and change parent if required to maintain consistency
     * @param pRole role to manage
     * @param pResourcesAccesses accesses to add
     * @throws EntityOperationForbiddenException if parent cannot be found
     */
    private void addAndManageParent(final Role pRole, final ResourcesAccess... pResourcesAccesses)
            throws EntityOperationForbiddenException {
        // Add accesses
        pRole.getPermissions().addAll(Sets.newHashSet(pResourcesAccesses));
        // Save changes
        roleRepository.save(pRole);
        // Change parent if required
        manageParentFromAdmin(pRole);
    }

    /**
     * Inform security starter an (or many) access(es) changed
     *
     * @param pResourcesAccesses
     *            resource accesses that have changed
     */
    private void publishResourceAccessEvent(final ResourcesAccess... pResourcesAccesses) {

        // Compute concerned microservices
        Set<String> microservices = new HashSet<>();
        for (ResourcesAccess ra : pResourcesAccesses) {
            microservices.add(ra.getMicroservice());
        }
        // Publish an event for each concerned microservice
        for (String microservice : microservices) {
            ResourceAccessEvent raEvent = new ResourceAccessEvent();
            raEvent.setMicroservice(microservice);
            publisher.publish(raEvent);
        }
    }

    @Override
    public void clearRoleResourcesAccess(final Long pRoleId) throws EntityNotFoundException {
        if (!existRole(pRoleId)) {
            throw new EntityNotFoundException(pRoleId.toString(), Role.class);
        }
        final Role role = roleRepository.findOne(pRoleId);
        role.getPermissions().clear();
        roleRepository.save(role);
    }

    @Override
    public Page<ProjectUser> retrieveRoleProjectUserList(final Long pRoleId, final Pageable pPageable)
            throws EntityNotFoundException {
        if (!existRole(pRoleId)) {
            throw new EntityNotFoundException(pRoleId.toString(), Role.class);
        }
        final Role role = roleRepository.findOne(pRoleId);
        final Set<Role> roles = retrieveInheritedRoles(role);
        roles.add(role);
        final Set<String> roleNames = roles.stream().map(r -> r.getName()).collect(Collectors.toSet());
        return projectUserRepository.findByRoleNameIn(roleNames, pPageable);
    }

    @Override
    public boolean existRole(final Long pRoleId) {
        return roleRepository.exists(pRoleId);
    }

    @Override
    public boolean existRole(final Role pRole) {
        return roleRepository.exists(pRole.getId());
    }

    @Override
    public boolean existByName(final String pName) {
        return roleRepository.findOneByName(pName).isPresent();
    }

    @Override
    public Role getDefaultRole() {
        return roleRepository.findOneByIsDefault(true).orElse(getRolePublic());
    }

    /**
     * Return true if pRole is an ancestor of pOther through the {@link Role#getParentRole()} chain.
     */
    @Override
    public boolean isHierarchicallyInferior(final Role pRole, final Role pOther) {
        final RoleLineageAssembler roleLineageAssembler = new RoleLineageAssembler();
        final List<Role> ancestors = roleLineageAssembler.of(pOther).get();
        try (Stream<Role> stream = ancestors.stream()) {
            return stream.anyMatch(r -> r.getName().equals(pRole.getName()));
        }
    }

    /**
     * @return the role public. Create it if not found
     */
    public Role getRolePublic() {
        final RoleFactory factory = new RoleFactory();
        return roleRepository.findOneByName(DefaultRole.PUBLIC.toString())
                .orElseGet(() -> roleRepository.save(factory.createPublic()));
    }

    @Override
    public Set<Role> retrieveInheritedRoles(final Role pRole) {
        final Set<Role> results = new HashSet<>();
        final Set<Role> inheritedRoles = roleRepository.findByParentRoleName(pRole.getName());
        if (inheritedRoles != null) {

            inheritedRoles.forEach(results::add);

            for (final Role role : inheritedRoles) {
                retrieveInheritedRoles(role).forEach(results::add);
            }
        }
        return results;
    }

    /**
     * Retrieve a role
     */
    @Override
    public Role retrieveRole(final Long pRoleId) throws EntityNotFoundException {
        final Role role = roleRepository.findOne(pRoleId);
        if (role == null) {
            throw new EntityNotFoundException(pRoleId, Role.class);
        }
        return role;
    }

    /**
     * Remove resource accesses from a role
     */
    @Override
    public void removeResourcesAccesses(final String pRoleName, final ResourcesAccess... pResourcesAccesses)
            throws EntityException {

        final Optional<Role> roleOpt = roleRepository.findByName(pRoleName);

        if (!roleOpt.isPresent()) {
            throw new EntityNotFoundException(pRoleName, Role.class);
        }

        Role role = roleOpt.get();

        // If PROJECT_ADMIN, nothing to do / removal forbidden
        if (role.getName().equals(DefaultRole.PROJECT_ADMIN.toString())) {
            throw new EntityOperationForbiddenException(role.getName(), Role.class,
                    "Removing resource accesses from role PROJECT_ADMIN is forbidden!");
        }

        if (role.isNative()) {
            // If native role, propagate removal to native ascendants to maintain consistency
            removeAndPropagate(role, pResourcesAccesses);
            // Check non native role parents
            manageParentRoles();
        } else {
            // Else only remove accesses from role and change parent if required (may throw an exception if at least public role cannot be the parent)
            removeAndManageParent(role, pResourcesAccesses);
        }

        // Publish changes
        publishResourceAccessEvent(pResourcesAccesses);
    }

    /**
     * Remove accesses on all inherited roles
     *
     * @param pRole role to manage
     * @param pResourcesAccesses accesses to remove
     */
    private void removeAndPropagate(final Role pRole, final ResourcesAccess... pResourcesAccesses) {

        if (pRole != null) {
            // Remove accesses
            pRole.getPermissions().removeAll(Sets.newHashSet(pResourcesAccesses));
            // Save changes
            roleRepository.save(pRole);
            // Propagate
            removeAndPropagate(pRole.getParentRole(), pResourcesAccesses);
        }
    }

    /**
     * Consider all non native roles to update its native parent after native role changes
     * @throws EntityOperationForbiddenException
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
     * @param pRole role to manage
     * @param pResourcesAccesses accesses to remove
     * @throws EntityOperationForbiddenException if parent cannot be found
     */
    private void removeAndManageParent(final Role pRole, final ResourcesAccess... pResourcesAccesses)
            throws EntityOperationForbiddenException {
        // Remove accesses
        pRole.getPermissions().removeAll(Sets.newHashSet(pResourcesAccesses));
        // Save changes
        roleRepository.save(pRole);
        // Change parent if required
        manageParent(pRole, pRole.getParentRole());
    }

    /**
     * Found a consistent parent for the current role starting with {@link DefaultRole#ADMIN}, highest available inherited parent.
     *
     * @param role role to consider
     * @throws EntityOperationForbiddenException if no parent role matches
     */
    private void manageParentFromAdmin(final Role role) throws EntityOperationForbiddenException {

        // Retrieve default admin role
        Role adminRole = roleRepository.findOneByName(DefaultRole.ADMIN.name()).get();
        // Manage parent
        manageParent(role, adminRole);
    }

    /**
     * Found a consistent parent for the current role
     *
     * @param role role to consider
     * @param parentRole parent role candidate
     * @throws EntityOperationForbiddenException if no parent role matches
     */
    private void manageParent(final Role role, final Role parentRole) throws EntityOperationForbiddenException {

        // role must not be null
        Assert.notNull(role);

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

        final String email = SecurityUtils.getActualUser();
        final ProjectUser user = projectUserRepository.findOneByEmail(email).get();
        // get Original Role of the user
        final Role originalRole = user.getRole();
        final List<String> roleNamesAllowedToBorrow = Lists.newArrayList(DefaultRole.ADMIN.toString(),
                                                                         DefaultRole.PROJECT_ADMIN.toString());
        // It is impossible to borrow a role if your original role is not ADMIN or PROJECT_ADMIN or one of their sons
        if (!roleNamesAllowedToBorrow.contains(originalRole.getName()) && ((originalRole.getParentRole() == null)
                || !roleNamesAllowedToBorrow.contains(originalRole.getParentRole().getName()))) {
            return Sets.newHashSet();
        }
        // get ascendants of the original Role
        final Set<Role> ascendants = new HashSet<>();
        if (originalRole.getParentRole() != null) {
            // only adds the ascendants of my role's parent as my role's brotherhood is not part of my role's ascendants
            ascendants.addAll(getAscendants(roleRepository.findOneById(originalRole.getParentRole().getId())));
        } else {
            // handle ProjectAdmin by considering that ADMIN is its parent(projectAdmin is not considered admin's
            // son so no resources accesses are added or removed from him but has to be considered for role borrowing)
            if (originalRole.getName().equals(DefaultRole.PROJECT_ADMIN.toString())) {
                ascendants.addAll(getAscendants(originalRole));
            } // INSTANCE_ADMIN and PUBLIC do not have ascendants
        }
        // add my original role because i can always borrow my own role
        ascendants.add(originalRole);
        return ascendants;
    }

    /**
     * Retrieve ascendants(parent and uncles) and brotherhood of the given role
     *
     * @param pRole
     * @return All ascendants of the given role
     */
    private Set<Role> getAscendants(final Role pRole) {
        final Set<Role> ascendants = Sets.newHashSet(pRole);
        // if pRole doesn't have parent then it's finished
        Role parent = pRole.getParentRole();
        if (parent == null) {
            // except if it's PROJECT_ADMIN
            if (pRole.getName().equals(DefaultRole.PROJECT_ADMIN.toString())) {
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
    public Set<Role> retrieveRolesWithResource(final Long pResourceId) {
        return roleRepository.findByPermissionsId(pResourceId);
    }

    /**
     * Inform security starter of a role change
     *
     * @param role
     *            role
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
        roleRepository.delete(role.getId());
        publishRoleEvent(role);
    }
}
