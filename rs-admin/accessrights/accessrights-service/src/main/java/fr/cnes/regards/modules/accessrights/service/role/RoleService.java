/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.role;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.*;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.SecurityUtils;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
     * AMQP Message subscriber
     */
    private final IInstanceSubscriber instanceSubscriber;

    /**
     * The default roles. Autowired by Spring.
     */
    @Resource
    private List<Role> defaultRoles;

    public RoleService(@Value("${spring.application.name}") final String pMicroserviceName,
            final IRoleRepository pRoleRepository, final IProjectUserRepository pProjectUserRepository,
            final ITenantResolver pTenantResolver, final IRuntimeTenantResolver pRuntimeTenantResolver,
            final IInstanceSubscriber pInstanceSubscriber) {
        super();
        roleRepository = pRoleRepository;
        projectUserRepository = pProjectUserRepository;
        tenantResolver = pTenantResolver;
        runtimeTenantResolver = pRuntimeTenantResolver;
        microserviceName = pMicroserviceName;
        instanceSubscriber = pInstanceSubscriber;
    }

    @PostConstruct
    public void init() {
        instanceSubscriber.subscribeTo(TenantConnectionReady.class,
                                       new TenantConnectionReadyEventHandler(runtimeTenantResolver));
        initDefaultRoles();
    }

    private class TenantConnectionReadyEventHandler implements IHandler<TenantConnectionReady> {

        private final IRuntimeTenantResolver runtimeTenantResolver;

        public TenantConnectionReadyEventHandler(final IRuntimeTenantResolver pRuntimeTenantResolver) {
            runtimeTenantResolver = pRuntimeTenantResolver;
        }

        /**
         * Initialize default roles in the new project connection
         *
         * @see fr.cnes.regards.framework.amqp.domain.IHandler#handle(fr.cnes.regards.framework.amqp.domain.TenantWrapper)
         * @since 1.0-SNAPSHOT
         */
        @Override
        public void handle(final TenantWrapper<TenantConnectionReady> pWrapper) {
            if (microserviceName.equals(pWrapper.getContent().getMicroserviceName())) {
                runtimeTenantResolver.forceTenant(pWrapper.getContent().getTenant());
                initDefaultRoles();
            }
        }
    }

    /**
     * Ensure the existence of default roles. If not, add them from their bean definition in defaultRoles.xml
     */
    public void initDefaultRoles() {

        // Return the role with same name in database if exists
        final UnaryOperator<Role> replaceWithRoleFromDb = r -> {
            final Optional<Role> dbRole = roleRepository.findOneByName(r.getName());
            return dbRole.isPresent() ? dbRole.get() : r;
        };

        // For passed role, replace parent with its equivalent from the defaultRoles list
        final Consumer<Role> setParentFromDefaultRoles = r -> {
            if (r.getParentRole() != null) {
                final Role parent = defaultRoles.stream().filter(el -> el.getName().equals(r.getParentRole().getName()))
                        .findFirst().orElse(null);
                r.setParentRole(parent);
            }
        };

        for (final String tenant : tenantResolver.getAllActiveTenants()) {
            // Work on tenant
            runtimeTenantResolver.forceTenant(tenant);
            // Replace all default roles with their db version if exists
            defaultRoles.replaceAll(replaceWithRoleFromDb);
            // Re-plug the parent roles
            defaultRoles.forEach(setParentFromDefaultRoles);
            // Save everything
            defaultRoles.forEach(roleRepository::save);
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
    public Role createRoleWithNativeParentPermissions(final Role pRoleToCreate) throws EntityException {
        if (existByName(pRoleToCreate.getName())) {
            throw new EntityAlreadyExistsException(pRoleToCreate.getName());
        }

        if ((pRoleToCreate.getParentRole() == null) || (pRoleToCreate.getParentRole().getName() == null)) {
            throw new EntityException("Parent role required to create a new role.");
        }

        // If parent role is a native role. Copy resources from the parent role.
        final Optional<Role> roleOpt = roleRepository.findOneByName(pRoleToCreate.getParentRole().getName());
        if (!roleOpt.isPresent()) {
            throw new EntityNotFoundException(pRoleToCreate.getParentRole().getName(), Role.class);
        }

        final Role parentRole = roleOpt.get();
        Role newCreatedRole;
        if (parentRole.isNative()) {
            newCreatedRole = roleRepository.save(pRoleToCreate);
            newCreatedRole.setPermissions(Sets.newHashSet(parentRole.getPermissions()));
        } else {
            // Retrieve parent native role of the given parent role.
            if (!parentRole.getParentRole().isNative()) {
                throw new EntityException(
                        "There is no native parent associated to the given parent role " + parentRole.getName());
            }

            newCreatedRole = new Role(pRoleToCreate.getName(), parentRole.getParentRole());
            if (pRoleToCreate.getAuthorizedAddresses() != null) {
                newCreatedRole.setAuthorizedAddresses(pRoleToCreate.getAuthorizedAddresses());
            }
            newCreatedRole = roleRepository.save(newCreatedRole);
            newCreatedRole.setPermissions(Sets.newHashSet(parentRole.getPermissions()));
        }

        return roleRepository.save(newCreatedRole);
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
        return roleRepository.save(pUpdatedRole);
    }

    @Override
    public void removeRole(final Long pRoleId) throws EntityException {
        final Role previous = roleRepository.findOne(pRoleId);
        if ((previous != null) && previous.isNative()) {
            throw new EntityOperationForbiddenException(pRoleId.toString(), Role.class, NATIVE_ROLE_NOT_REMOVABLE);
        } else
            if (previous == null) {
                throw new EntityNotFoundException(pRoleId, Role.class);
            }
        roleRepository.delete(pRoleId);
    }

    @Override
    public void removeRole(final String pRoleName) throws EntityException {
        final Optional<Role> role = roleRepository.findOneByName(pRoleName);
        if (!role.isPresent()) {
            throw new EntityNotFoundException(pRoleName, Role.class);
        } else
            if (role.get().isNative()) {
                throw new EntityOperationForbiddenException(pRoleName, Role.class, NATIVE_ROLE_NOT_REMOVABLE);
            } else {
                roleRepository.delete(role.get().getId());
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
    public void addResourceAccesses(final Long pRoleId, final ResourcesAccess... pNewOnes)
            throws EntityNotFoundException {
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
     */
    private void addResourceAccesses(final Role pRole, final ResourcesAccess... pNewOnes) {
        final Set<Role> descendants = getDescendants(pRole);
        final Set<Role> nativeDescendants = descendants.stream().filter(role -> role.isNative())
                .collect(Collectors.toSet());
        // access is added to this role, so we add it
        descendants.add(pRole);
        for (final Role descendant : descendants) {
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
    private Set<Role> getDescendants(final Role pRole) {
        // get sons of this role
        final Set<Role> sons = roleRepository.findByParentRoleName(pRole.getName());
        final Set<Role> descendants = Sets.newHashSet();
        descendants.addAll(sons);
        final Set<Role> newDescendants = Sets.newHashSet(descendants);
        // for each son get its descendants
        for (final Role son : descendants) {
            descendants.addAll(getDescendants(son));
        }
        return descendants;
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
    private void changeParent(final Role pDescendant, final Set<Role> pNativeDescendants) {
        if (!pDescendant.isNative()) {
            // check for the native role which has the most resource accesses that do not contains all of the
            // descendant accesses or has the same ones. One of the cases is encountered as Project Admin has all
            // the resource accesses available.
            // case one of the native roles has the same accesses:
            final List<Role> nativesWithSameAccesses = pNativeDescendants.stream()
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
                final List<Role> nativeCandidates = pNativeDescendants.stream()
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
    private Role searchBetterParent(final Role pCandidate, final List<Role> pNativesWithSameAccesses) {

        // we have at most one son as the hierarchy of native roles is a linear. Moreover, we are looking for the son of
        // the candidate because he is less likely to have his accesses reduced in the future.
        final Optional<Role> sonsOfCandidateAmongNativesWithSameAccesses = pNativesWithSameAccesses.stream()
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
    private Role getRightCandidate(final List<Role> pNativeCandidates) {
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

            inheritedRoles.forEach(r -> results.add(r));

            for (final Role role : inheritedRoles) {
                retrieveInheritedRoles(role).forEach(r -> results.add(r));
            }
        }
        return results;
    }

    @Override
    public Role retrieveRole(final Long pRoleId) throws EntityNotFoundException {
        final Role role = roleRepository.findOne(pRoleId);
        if (role == null) {
            throw new EntityNotFoundException(pRoleId, Role.class);
        }
        return role;
    }

    private void removeResourcesAccesses(final Role pRole, final ResourcesAccess... pResourcesAccesses)
            throws EntityException {
        if (pRole.getName().equals(DefaultRole.PROJECT_ADMIN.toString())) {
            throw new EntityOperationForbiddenException(pRole.getName(), Role.class,
                    "Removing resource accesses from role PROJECT_ADMIN is forbidden!");
        }

        final Role father = pRole.getParentRole();
        if (father != null) {
            final Set<Role> ascendants = getAscendants(roleRepository.findOneById(pRole.getParentRole().getId()));
            final Set<Role> nativeAscendants = ascendants.stream().filter(a -> a.isNative())
                    .collect(Collectors.toSet());
            if (pRole.isNative()) {
                nativeAscendants.add(pRole); // adding myself so i am processed with the others
                // remove the acces from the native ascendants
                for (final Role nativeAscendant : nativeAscendants) {
                    nativeAscendant.getPermissions().removeAll(Sets.newHashSet(pResourcesAccesses));
                    roleRepository.save(nativeAscendant);
                }
                // custom role parent won't change on removal
            } else {
                // in case of a custom role, removal is not propagated to other roles
                pRole.getPermissions().removeAll(Sets.newHashSet(pResourcesAccesses));
                // but a custom role can change parent
                changeParent(pRole, nativeAscendants);
                roleRepository.save(pRole);
            }
        } else {
            pRole.getPermissions().removeAll(Sets.newHashSet(pResourcesAccesses));
            roleRepository.save(pRole);
        }
    }

    @Override
    public void removeResourcesAccesses(final String pRoleName, final ResourcesAccess... pResourcesAccesses)
            throws EntityException {

        final Optional<Role> roleOpt = roleRepository.findByName(pRoleName);

        if (!roleOpt.isPresent()) {
            throw new EntityNotFoundException(pRoleName, Role.class);
        }

        removeResourcesAccesses(roleOpt.get(), pResourcesAccesses);
    }

    @Override
    public Set<Role> retrieveBorrowableRoles() throws JwtException {

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

}
