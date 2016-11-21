/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.role;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.InvalidValueException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.OperationForbiddenException;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.projects.RoleFactory;
import fr.cnes.regards.modules.core.utils.RegardsStreamUtils;

/**
 * {@link IRoleService} implementation
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 *
 */
@Service
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
     * Security service
     */
    private final JWTService jwtService;

    /**
     * The default roles. Autowired by Spring.
     */
    @Resource
    private List<Role> defaultRoles;

    public RoleService(@Value("${spring.application.name}") final String pMicroserviceName,
            final IRoleRepository pRoleRepository, final IProjectUserRepository pProjectUserRepository,
            final ITenantResolver pTenantResolver, final JWTService pJwtService) {
        super();
        roleRepository = pRoleRepository;
        projectUserRepository = pProjectUserRepository;
        tenantResolver = pTenantResolver;
        jwtService = pJwtService;
        microserviceName = pMicroserviceName;
    }

    /**
     * Init medthod
     */
    @PostConstruct
    public void init() {
        // Ensure the existence of default roles
        // If not, add them from their bean definition in defaultRoles.xml

        // Define a consumer injecting the passed tenant in the context
        final Consumer<? super String> injectTenant = tenant -> {
            try {
                jwtService.injectToken(tenant, RoleAuthority.getSysRole(microserviceName));
            } catch (final JwtException e) {
                LOG.error(e.getMessage(), e);
            }
        };

        // Return the role with same name in db if exists
        final UnaryOperator<Role> replaceWithRoleFromDb = r -> {
            try {
                return retrieveRole(r.getName());
            } catch (final ModuleEntityNotFoundException e) {
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
        final Consumer<? super String> createDefaultRolesOnTenantNew = t -> {
            // Replace all default roles with their db version if exists
            defaultRoles.replaceAll(replaceWithRoleFromDb);
            // Re-plug the parent roles
            defaultRoles.forEach(setParentFromDefaultRoles);
            // Save everything
            defaultRoles.forEach(roleRepository::save);
        };

        // For each tenant, inject tenant in context and create (if needed) default roles
        try (Stream<String> tenantsStream = tenantResolver.getAllTenants().stream()) {
            tenantsStream.peek(injectTenant).forEach(createDefaultRolesOnTenantNew);
        }

    }

    @Override
    public List<Role> retrieveRoleList() {
        try (Stream<Role> stream = StreamSupport.stream(roleRepository.findAll().spliterator(), true)) {
            return stream.collect(Collectors.toList());
        }
    }

    @Override
    public Role createRole(final Role pNewRole) throws AlreadyExistingException {
        if (existByName(pNewRole.getName())) {
            throw new AlreadyExistingException(pNewRole.getName());
        }
        return roleRepository.save(pNewRole);
    }

    @Override
    public Role retrieveRole(final String pRoleName) throws ModuleEntityNotFoundException {
        return roleRepository.findOneByName(pRoleName)
                .orElseThrow(() -> new ModuleEntityNotFoundException(pRoleName, Role.class));
    }

    @Override
    public void updateRole(final Long pRoleId, final Role pUpdatedRole)
            throws ModuleEntityNotFoundException, InvalidValueException {
        if (!pRoleId.equals(pUpdatedRole.getId())) {
            throw new InvalidValueException();
        }
        if (!existRole(pRoleId)) {
            throw new ModuleEntityNotFoundException(pRoleId.toString(), Role.class);
        }
        roleRepository.save(pUpdatedRole);
    }

    @Override
    public void removeRole(final Long pRoleId) throws OperationForbiddenException {
        final Role previous = roleRepository.findOne(pRoleId);
        if ((previous != null) && previous.isNative()) {
            throw new OperationForbiddenException(pRoleId.toString(), Role.class, NATIVE_ROLE_NOT_REMOVABLE);
        }
        roleRepository.delete(pRoleId);
    }

    /**
     * Les droits d’accès d’un utilisateur sont la fusion des droits d’accès de son rôle, des rôles hiérarchiquement
     * liés et de ses propres droits.
     *
     * @see SGDS-CP-12200-0010-CS p. 73
     * @see REGARDS_DSL_ADM_ADM_260
     */
    @Override
    public List<ResourcesAccess> retrieveRoleResourcesAccessList(final Long pRoleId)
            throws ModuleEntityNotFoundException {
        if (!existRole(pRoleId)) {
            throw new ModuleEntityNotFoundException(pRoleId.toString(), Role.class);
        }

        final List<Role> roleAndHisAncestors = new ArrayList<>();

        final Role role = roleRepository.findOne(pRoleId);
        roleAndHisAncestors.add(role);

        final RoleLineageAssembler roleLineageAssembler = new RoleLineageAssembler();
        roleAndHisAncestors.addAll(roleLineageAssembler.of(role).get());

        return roleAndHisAncestors.stream().map(r -> r.getPermissions()).flatMap(l -> l.stream())
                .collect(Collectors.toList());
    }

    @Override
    public Role updateRoleResourcesAccess(final Long pRoleId, final List<ResourcesAccess> pResourcesAccessList)
            throws ModuleEntityNotFoundException {
        if (!existRole(pRoleId)) {
            throw new ModuleEntityNotFoundException(pRoleId.toString(), Role.class);
        }
        final Role role = roleRepository.findOne(pRoleId);
        final List<ResourcesAccess> permissions = role.getPermissions();
        final Predicate<ResourcesAccess> filter = RegardsStreamUtils.distinctByKey(r -> r.getId());

        try (final Stream<ResourcesAccess> previous = permissions.stream();
                final Stream<ResourcesAccess> toMerge = pResourcesAccessList.stream();
                final Stream<ResourcesAccess> merged = Stream.concat(toMerge, previous)) {
            role.setPermissions(merged.filter(filter).collect(Collectors.toList()));
            roleRepository.save(role);
        }

        return role;
    }

    @Override
    public void clearRoleResourcesAccess(final Long pRoleId) throws ModuleEntityNotFoundException {
        if (!existRole(pRoleId)) {
            throw new ModuleEntityNotFoundException(pRoleId.toString(), Role.class);
        }
        final Role role = roleRepository.findOne(pRoleId);
        role.setPermissions(new ArrayList<>());
        roleRepository.save(role);
    }

    @Override
    public List<ProjectUser> retrieveRoleProjectUserList(final Long pRoleId) throws ModuleEntityNotFoundException {
        if (!existRole(pRoleId)) {
            throw new ModuleEntityNotFoundException(pRoleId.toString(), Role.class);
        }
        final List<Role> roleAndHisAncestors = new ArrayList<>();

        final Role role = roleRepository.findOne(pRoleId);
        roleAndHisAncestors.add(role);

        final RoleLineageAssembler roleLineageAssembler = new RoleLineageAssembler();
        roleAndHisAncestors.addAll(roleLineageAssembler.of(role).get());

        try (final Stream<Role> stream = roleAndHisAncestors.stream()) {
            return stream.map(r -> projectUserRepository.findByRoleName(r.getName())).flatMap(l -> l.stream())
                    .collect(Collectors.toList());
        }
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

}
