/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.role;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.projects.RoleFactory;
import fr.cnes.regards.modules.accessrights.domain.projects.RoleLineageAssembler;
import fr.cnes.regards.modules.project.domain.event.NewProjectConnectionEvent;

/**
 * {@link IRoleService} implementation
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 */
@Service
@ImportResource({ "classpath*:defaultRoles.xml" })
@MultitenantTransactional
public class RoleService implements IRoleService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RoleService.class);

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
     * Security service
     */
    private final JWTService jwtService;

    /**
     * AMQP Message subscriber
     */
    private final ISubscriber subscriber;

    /**
     * The default roles. Autowired by Spring.
     */
    @Resource
    private List<Role> defaultRoles;

    public RoleService(@Value("${spring.application.name}") final String pMicroserviceName,
            final IRoleRepository pRoleRepository, final IProjectUserRepository pProjectUserRepository,
            final ITenantResolver pTenantResolver, IRuntimeTenantResolver pRuntimeTenantResolver,
            final JWTService pJwtService, final ISubscriber pSubscriber) {
        super();
        roleRepository = pRoleRepository;
        projectUserRepository = pProjectUserRepository;
        tenantResolver = pTenantResolver;
        runtimeTenantResolver = pRuntimeTenantResolver;
        jwtService = pJwtService;
        microserviceName = pMicroserviceName;
        subscriber = pSubscriber;
    }

    @PostConstruct
    public void init() {
        subscriber.subscribeTo(NewProjectConnectionEvent.class,
                               new NewProjectConnectionEventHandler(runtimeTenantResolver, this));
        initDefaultRoles();
    }

    private class NewProjectConnectionEventHandler implements IHandler<NewProjectConnectionEvent> {

        private final IRoleService roleService;

        private final IRuntimeTenantResolver runtimeTenantResolver;

        public NewProjectConnectionEventHandler(IRuntimeTenantResolver pRuntimeTenantResolver,
                IRoleService pRoleService) {
            super();
            roleService = pRoleService;
            runtimeTenantResolver = pRuntimeTenantResolver;
        }

        /**
         *
         * Initialize default roles in the new project connection
         *
         * @see fr.cnes.regards.framework.amqp.domain.IHandler#handle(fr.cnes.regards.framework.amqp.domain.TenantWrapper)
         * @since 1.0-SNAPSHOT
         */
        @Override
        public void handle(final TenantWrapper<NewProjectConnectionEvent> pWrapper) {
            runtimeTenantResolver.forceTenant(pWrapper.getTenant());
            roleService.initDefaultRoles();
        }
    }

    /**
     * Ensure the existence of default roles. If not, add them from their bean definition in defaultRoles.xml
     */
    // FIXME method à revoir avec IRuntimeTenantResolver
    @Override
    public void initDefaultRoles() {

        // Define a consumer injecting the passed tenant in the context
        final Consumer<? super String> injectTenant = tenant -> {
            try {
                jwtService.injectToken(tenant, RoleAuthority.getSysRole(microserviceName), microserviceName);
            } catch (final JwtException e) {
                LOG.error(e.getMessage(), e);
            }
        };

        // Return the role with same name in db if exists
        final UnaryOperator<Role> replaceWithRoleFromDb = r -> {
            try {
                return retrieveRole(r.getName());
            } catch (final EntityNotFoundException e) {
                LOG.debug("Could not find a role in DB, falling back to xml definition.", e);
                return r;
            }
        };

        // For passed role, replace parent with its equivalent from the defaultRoles list
        final Consumer<Role> setParentFromDefaultRoles = r -> {
            if (r.getParentRole() != null) {
                final Role parent = defaultRoles.stream().filter(el -> el.getName().equals(r.getParentRole().getName()))
                        .findFirst().orElse(null);
                r.setParentRole(parent);
            }
        };

        // Define a consumer creating if needed all default roles on current tenant
        final Consumer<? super String> createDefaultRolesOnTenant = t -> {
            // Replace all default roles with their db version if exists
            defaultRoles.replaceAll(replaceWithRoleFromDb);
            // Re-plug the parent roles
            defaultRoles.forEach(setParentFromDefaultRoles);
            // Save everything
            defaultRoles.forEach(roleRepository::save);
        };

        // For each tenant, inject tenant in context and create (if needed) default roles
        try (Stream<String> tenantsStream = tenantResolver.getAllTenants().stream()) {
            tenantsStream.peek(injectTenant).forEach(createDefaultRolesOnTenant);
        }
    }

    @Override
    public Set<Role> retrieveRoles() {
        try (Stream<Role> stream = StreamSupport.stream(roleRepository.findAllDistinctLazy().spliterator(), true)) {
            return stream.collect(Collectors.toSet());
        }
    }

    @Override
    public Role createRole(final Role pNewRole) throws EntityAlreadyExistsException {
        if (existByName(pNewRole.getName())) {
            throw new EntityAlreadyExistsException(pNewRole.getName());
        }
        return roleRepository.save(pNewRole);
    }

    @Override
    public Role retrieveRole(final String pRoleName) throws EntityNotFoundException {
        return roleRepository.findOneByName(pRoleName)
                .orElseThrow(() -> new EntityNotFoundException(pRoleName, Role.class));
    }

    @Override
    public Role updateRole(final Long pRoleId, final Role pUpdatedRole) throws EntityException {
        if (!pRoleId.equals(pUpdatedRole.getId())) {
            throw new EntityInconsistentIdentifierException(pRoleId, pUpdatedRole.getId(), Role.class);
        }
        if (!existRole(pRoleId)) {
            throw new EntityNotFoundException(pRoleId.toString(), Role.class);
        }
        return roleRepository.save(pUpdatedRole);
    }

    @Override
    public void removeRole(final Long pRoleId) throws EntityOperationForbiddenException {
        final Role previous = roleRepository.findOne(pRoleId);
        if ((previous != null) && previous.isNative()) {
            throw new EntityOperationForbiddenException(pRoleId.toString(), Role.class, NATIVE_ROLE_NOT_REMOVABLE);
        }
        roleRepository.delete(pRoleId);
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
        final Role role = roleRepository.findOne(pRoleId);
        return role.getPermissions();
    }

    @Override
    public Role updateRoleResourcesAccess(final Long pRoleId, final Set<ResourcesAccess> pResourcesAccesses)
            throws EntityNotFoundException, EntityOperationForbiddenException {
        if (!existRole(pRoleId)) {
            throw new EntityNotFoundException(pRoleId.toString(), Role.class);
        }
        final Role role = roleRepository.findOne(pRoleId);
        final Set<ResourcesAccess> permissions = role.getPermissions();

        // extract which one are to be removed
        Set<ResourcesAccess> toBeRemoved = new HashSet<>(permissions);
        toBeRemoved.removeAll(pResourcesAccesses);
        // remove them by handling descendancy
        removeResourcesAccesses(role, toBeRemoved.toArray(new ResourcesAccess[toBeRemoved.size()]));

        // extract which ResourcesAccess is really new
        Set<ResourcesAccess> newOnes = new HashSet<>(pResourcesAccesses);
        newOnes.removeAll(permissions);
        // add the ResourceAccesses by handling descendancy
        addResourceAccesses(role, newOnes.toArray(new ResourcesAccess[newOnes.size()]));

        return role;
    }

    @Override
    public void addResourceAccesses(Long pRoleId, ResourcesAccess... pNewOnes) throws EntityNotFoundException {
        Role role = roleRepository.findOne(pRoleId);
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
     */
    private void addResourceAccesses(Role pRole, ResourcesAccess... pNewOnes) {
        Set<Role> descendants = getDescendants(pRole);
        Set<Role> nativeDescendants = descendants.stream().filter(role -> role.isNative()).collect(Collectors.toSet());
        // access is added to this role, so we add it
        descendants.add(pRole);
        for (Role descendant : descendants) {
            descendant.getPermissions().addAll(Lists.newArrayList(pNewOnes));
            changeParent(descendant, nativeDescendants);
            roleRepository.save(descendant);
        }
    }

    /**
     * Used with {@link RoleService#addResourceAccesses(Role, Set)} so we just need to get the descendants of a given
     * role until our actual role as we cannot add accesses that we do not have.
     *
     * @param pRole
     * @return Set of descendants
     */
    private Set<Role> getDescendants(Role pRole) {
        // get sons of this role
        if (isCurrentUserRole(pRole)) {
            // if its the current role then
            return roleRepository.findByParentRoleName(pRole.getName());
        }
        Set<Role> descendants = roleRepository.findByParentRoleName(pRole.getName());
        // for each son get its descendants
        for (Role son : descendants) {
            descendants.addAll(getDescendants(son));
        }
        return descendants;
    }

    /**
     * Check if the given role is the current role(real one or borrowed) of the user
     *
     * @param pRole
     * @return true if the given role is the one of the user
     */
    private boolean isCurrentUserRole(Role pRole) {
        return jwtService.getActualRole().equals(pRole.getName());
    }

    /**
     * Check if the pDescendant should change of parent or not. If pDescendant is a native role then it doesn't change.
     * Otherwise, it may change to a role closer to ADMIN among the native roles
     *
     * @param pDescendant
     *            role that may change of parent
     * @param pNativeDescendants
     *            set of native role among the descendants(without pDescendant's parent) of pDescendant(troncated to the
     *            current role of the user asking for the change) passed as parameter to avoid looking for them all the
     *            time.
     */
    private void changeParent(Role pDescendant, Set<Role> pNativeDescendants) {
        if (!pDescendant.isNative()) {
            // check for the native role which has the most resource accesses that do not contains all of the
            // descendant accesses or has the same ones. One of the cases is encountered as Project Admin has all
            // the resource accesses available.
            // case one of the native roles has the same accesses:
            List<Role> nativesWithSameAccesses = pNativeDescendants.stream()
                    .filter(nativeRole -> (nativeRole.getPermissions().size() == pDescendant.getPermissions().size())
                            && nativeRole.getPermissions().containsAll(pDescendant.getPermissions()))
                    .collect(Collectors.toList());
            if (!nativesWithSameAccesses.isEmpty()) {
                Role candidate = nativesWithSameAccesses.get(0);
                if (nativesWithSameAccesses.size() != 1) {
                    // when there is multiple possibilities: choose the one that is the closer from ADMIN
                    candidate = searchBetterParent(candidate, nativesWithSameAccesses);
                }
                pDescendant.setParentRole(candidate);
            } else {
                // case none of the native roles has the same accesses:
                List<Role> nativeCandidates = pNativeDescendants.stream()
                        .filter(nativeRole -> pDescendant.getPermissions().containsAll(nativeRole.getPermissions()))
                        .collect(Collectors.toList());
                if (!nativeCandidates.isEmpty()) {
                    pDescendant.setParentRole(getRightCandidate(nativeCandidates));
                }
            }
        }
    }

    /**
     * Used by {@link RoleService#changeParent(Role, Set)}.
     *
     * Look for a native role that is less likely to be changed in the future.
     *
     * @param pCandidate
     *            initial candidate
     * @param pNativesWithSameAccesses
     *            list of possible candidate.
     * @return pCandantite if there is no better choice, one of its descendants otherwise
     */
    private Role searchBetterParent(final Role pCandidate, List<Role> pNativesWithSameAccesses) {

        // we have at most one son as the hierarchy of native roles is a linear. Moreover, we are looking for the son of
        // the candidate because he is less likely to have his accesses reduced in the future.
        Optional<Role> sonsOfCandidateAmongNativesWithSameAccesses = pNativesWithSameAccesses.stream()
                .filter(otherCandidate -> otherCandidate.getParentRole().equals(pCandidate)).findFirst();
        if (sonsOfCandidateAmongNativesWithSameAccesses.isPresent()) {
            return searchBetterParent(sonsOfCandidateAmongNativesWithSameAccesses.get(), pNativesWithSameAccesses);
        }
        return pCandidate;

    }

    /**
     * Used by {@link RoleService#changeParent(Role, Set)} to determine which one of the candidates has the most
     * resource accesses
     *
     * @param pNativeCandidates
     * @return the role having the most resource accesses between the pNativeCandidates
     */
    private Role getRightCandidate(List<Role> pNativeCandidates) {
        Role candidate = pNativeCandidates.get(0);
        for (int i = 1; i < pNativeCandidates.size(); i++) {
            if (pNativeCandidates.get(i).getPermissions().size() > candidate.getPermissions().size()) {
                candidate = pNativeCandidates.get(i);
            }
        }
        return candidate;
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
     * Return true if {@link pRole} is an ancestor of {@link pOther} through the {@link Role#getParentRole()} chain.
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

            inheritedRoles.forEach(r -> results.add(r));

            for (final Role role : inheritedRoles) {
                retrieveInheritedRoles(role).forEach(r -> results.add(r));
            }
        }
        return results;
    }

    @Override
    public Role retrieveRole(Long pRoleId) throws EntityNotFoundException {
        Role role = roleRepository.findOne(pRoleId);
        if (role == null) {
            throw new EntityNotFoundException(pRoleId, Role.class);
        }
        return role;
    }

    @Override
    public void removeResourcesAccesses(Role pRole, ResourcesAccess... pResourcesAccesses)
            throws EntityOperationForbiddenException {
        if (pRole.getName().equals(DefaultRole.PROJECT_ADMIN.toString())) {
            throw new EntityOperationForbiddenException(pRole.getName(), Role.class,
                    "Removing resource accesses from role PROJECT_ADMIN is forbidden!");
        }
        Set<Role> descendants = getDescendants(pRole);
        Set<Role> nativeDescendants = descendants.stream().filter(role -> role.isNative()).collect(Collectors.toSet());
        // lets add this role to the list so it is processed with the others
        descendants.add(pRole);
        for (Role descendant : descendants) {
            descendant.getPermissions().removeAll(Lists.newArrayList(pResourcesAccesses));
            // dynamic roles might change of parent when we are removing accesses
            changeParent(descendant, nativeDescendants);
            roleRepository.save(descendant);
        }
    }

    @Override
    public Set<Role> retrieveBorrowableRoles() throws JwtException {
        // get User
        JWTAuthentication authentication = jwtService.getCurrentToken();
        String email = authentication.getName();
        ProjectUser user = projectUserRepository.findOneByEmail(email).get();
        // get Original Role of the user
        Role originalRole = user.getRole();
        List<String> roleNamesAllowedToBorrow = Lists.newArrayList(DefaultRole.ADMIN.toString(),
                                                                   DefaultRole.PROJECT_ADMIN.toString());
        // It is impossible to borrow a role if your original role is not ADMIN or PROJECT_ADMIN or one of their sons
        if (!roleNamesAllowedToBorrow.contains(originalRole.getName()) && ((originalRole.getParentRole() == null)
                || !roleNamesAllowedToBorrow.contains(originalRole.getParentRole().getName()))) {
            return Sets.newHashSet();
        }
        // get ascendants of the original Role
        Set<Role> ascendants = new HashSet<>();
        if (originalRole.getParentRole() != null) {
            // only adds the ascendants of my role's parent as my role's brotherhood is not part of my role's ascendants
            ascendants.addAll(getAscendants(originalRole.getParentRole()));
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
    private Set<Role> getAscendants(Role pRole) {
        Set<Role> ascendants = Sets.newHashSet(pRole);
        // if pRole doesn't have parent then it's finished
        if (pRole.getParentRole() == null) {
            return ascendants;
        }
        // otherwise lets get pRole's parent and look for his children: Brotherhood
        Role parent = pRole.getParentRole();
        ascendants.addAll(roleRepository.findByParentRoleName(pRole.getParentRole().getName()));
        // now lets add the ascendants of parent
        ascendants.addAll(getAscendants(parent));
        return ascendants;
    }

}
