/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.InvalidValueException;
import fr.cnes.regards.framework.module.rest.exception.OperationForbiddenException;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.core.utils.RegardsStreamUtils;

/**
 * {@link IRoleService} implementation
 *
 * @author Xavier-Alexandre Brochard
 *
 */
@Service
public class RoleService implements IRoleService {

    /**
     * Error message
     */
    private static final String NATIVE_ROLE_NOT_REMOVABLE = "Modifications on native roles are forbidden";

    /**
     * CRUD repository managing {@link Role}s. Autowired by Spring.
     */
    private final IRoleRepository roleRepository;

    /**
     * The default roles. Autowired by Spring.
     */
    @Resource
    private List<Role> defaultRoles;

    public RoleService(final IRoleRepository pRoleRepository) {
        super();
        roleRepository = pRoleRepository;
    }

    @PostConstruct
    public void init() throws AlreadyExistingException {
        // Ensure the existence of default roles
        // If not, add them from their bean definition in defaultRoles.xml
        // Get all projects in database
        // for (final Role role : defaultRoles) {
        // if (!existRole(role)) {
        // createRole(role);
        // }
        // }
    }

    @Override
    public List<Role> retrieveRoleList() {
        try (Stream<Role> stream = StreamSupport.stream(roleRepository.findAll().spliterator(), true)) {
            return stream.collect(Collectors.toList());
        }
    }

    @Override
    public Role createRole(final Role pNewRole) throws AlreadyExistingException {
        if (existRole(pNewRole)) {
            throw new AlreadyExistingException(pNewRole.toString());
        }
        return roleRepository.save(pNewRole);
    }

    @Override
    public Role retrieveRole(final Long pRoleId) {
        return roleRepository.findOne(pRoleId);
    }

    @Override
    public Role retrieveRole(final String pRoleName) {
        return roleRepository.findOneByName(pRoleName);
    }

    @Override
    public void updateRole(final Long pRoleId, final Role pUpdatedRole)
            throws EntityNotFoundException, InvalidValueException {
        if (!pRoleId.equals(pUpdatedRole.getId())) {
            throw new InvalidValueException();
        }
        if (!existRole(pRoleId)) {
            throw new EntityNotFoundException(pRoleId.toString(), Role.class);
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
    public List<ResourcesAccess> retrieveRoleResourcesAccessList(final Long pRoleId) throws EntityNotFoundException {
        if (!existRole(pRoleId)) {
            throw new EntityNotFoundException(pRoleId.toString(), Role.class);
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
            throws EntityNotFoundException {
        if (!existRole(pRoleId)) {
            throw new EntityNotFoundException(pRoleId.toString(), Role.class);
        }
        final Role role = roleRepository.findOne(pRoleId);
        final List<ResourcesAccess> permissions = role.getPermissions();

        try (final Stream<ResourcesAccess> previous = permissions.stream();
                final Stream<ResourcesAccess> toMerge = pResourcesAccessList.stream()) {
            final Predicate<ResourcesAccess> filter = RegardsStreamUtils.distinctByKey(r -> r.getId());
            role.setPermissions(Stream.concat(toMerge, previous).filter(filter).collect(Collectors.toList()));
            roleRepository.save(role);
        }

        return role;
    }

    @Override
    public void clearRoleResourcesAccess(final Long pRoleId) throws EntityNotFoundException {
        if (!existRole(pRoleId)) {
            throw new EntityNotFoundException(pRoleId.toString(), Role.class);
        }
        final Role role = roleRepository.findOne(pRoleId);
        role.setPermissions(new ArrayList<>());
        roleRepository.save(role);
    }

    @Override
    public List<ProjectUser> retrieveRoleProjectUserList(final Long pRoleId) throws EntityNotFoundException {
        if (!existRole(pRoleId)) {
            throw new EntityNotFoundException(pRoleId.toString(), Role.class);
        }
        final List<Role> roleAndHisAncestors = new ArrayList<>();

        final Role role = roleRepository.findOne(pRoleId);
        roleAndHisAncestors.add(role);

        final RoleLineageAssembler roleLineageAssembler = new RoleLineageAssembler();
        roleAndHisAncestors.addAll(roleLineageAssembler.of(role).get());

        try (final Stream<Role> stream = roleAndHisAncestors.stream()) {
            return stream.map(r -> r.getProjectUsers()).flatMap(l -> l.stream()).collect(Collectors.toList());
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
    public Role getDefaultRole() {
        return roleRepository.findByIsDefault(true);
    }

    /**
     * Return true if {@link pRole} is an ancestor of {@link pOther} through the {@link Role#getParentRole()} chain.
     */
    @Override
    public boolean isHierarchicallyInferior(final Role pRole, final Role pOther) {

        final RoleLineageAssembler roleLineageAssembler = new RoleLineageAssembler();
        final List<Role> ancestors = roleLineageAssembler.of(pOther).get();

        return ancestors.contains(pRole);
    }

}
