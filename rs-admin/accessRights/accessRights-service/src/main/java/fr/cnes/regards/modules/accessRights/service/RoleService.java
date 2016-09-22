/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.naming.OperationNotSupportedException;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.dao.IRoleRepository;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.ResourcesAccess;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;

@Service
public class RoleService implements IRoleService {

    private final IRoleRepository roleRepository_;

    public RoleService(IRoleRepository pRoleRepository) {
        super();
        roleRepository_ = pRoleRepository;
    }

    @Override
    public List<Role> retrieveRoleList() {
        return roleRepository_.findAll();
    }

    @Override
    public Role createRole(Role pNewRole) throws AlreadyExistingException {
        if (existRole(pNewRole)) {
            throw new AlreadyExistingException(pNewRole.toString());
        }
        return roleRepository_.save(pNewRole);
    }

    @Override
    public Role retrieveRole(Long pRoleId) {
        return roleRepository_.findOne(pRoleId);
    }

    @Override
    public void updateRole(Long pRoleId, Role pUpdatedRole)
            throws NoSuchElementException, OperationNotSupportedException {
        if (!pRoleId.equals(pUpdatedRole.getId())) {
            throw new OperationNotSupportedException();
        }
        if (!existRole(pRoleId)) {
            throw new NoSuchElementException();
        }
        roleRepository_.save(pUpdatedRole);
    }

    @Override
    public void removeRole(Long pRoleId) {
        roleRepository_.delete(pRoleId);
    }

    /**
     * Les droits d’accès d’un utilisateur sont la fusion des droits d’accès de son rôle, des rôles hiérarchiquement
     * liés et de ses propres droits.
     *
     * @see SGDS-CP-12200-0010-CS p. 73
     * @see REGARDS_DSL_ADM_ADM_260
     */
    @Override
    public List<ResourcesAccess> retrieveRoleResourcesAccessList(Long pRoleId) {
        List<Role> roleAndHisAncestors = new ArrayList<>();

        Role role = roleRepository_.findOne(pRoleId);
        roleAndHisAncestors.add(role);

        RoleLineageAssembler roleLineageAssembler = new RoleLineageAssembler();
        roleAndHisAncestors.addAll(roleLineageAssembler.of(role).get());

        List<ResourcesAccess> permissions = roleAndHisAncestors.stream().map(r -> r.getPermissions())
                .flatMap(l -> l.stream()).collect(Collectors.toList());
        return permissions;
    }

    @Override
    public void updateRoleResourcesAccess(Long pRoleId, List<ResourcesAccess> pResourcesAccessList) {
        Role role = roleRepository_.findOne(pRoleId);
        List<ResourcesAccess> permissions = role.getPermissions();

        // // Finder method
        // // Pass the id and the list to search, returns the element with passed id
        // BiFunction<Long, List<ResourcesAccess>, List<ResourcesAccess>> find = (id, list) -> {
        // return list.stream().filter(e -> e.getId().equals(id)).collect(Collectors.toList());
        // };
        // BiFunction<Long, List<ResourcesAccess>, Boolean> contains = (id, list) -> {
        // return !find.apply(id, list).isEmpty();
        // };
        // If an element with the same id is found in the pResourcesAccessList list, replace with it
        // Else keep the old element
        // permissions.replaceAll(p -> contains.apply(p.getId()) ? find.apply(p.getId()).get(0) : p);

        // permissions.replaceAll(pResourcesAccessList);
        permissions = Stream.concat(permissions.stream(), pResourcesAccessList.stream()).distinct()
                .collect(Collectors.toList());

        role.setPermissions(permissions);
        roleRepository_.save(role);
    }

    @Override
    public void clearRoleResourcesAccess(Long pRoleId) {
        Role role = roleRepository_.findOne(pRoleId);
        role.setPermissions(new ArrayList<>());
        roleRepository_.save(role);
    }

    @Override
    public List<ProjectUser> retrieveRoleProjectUserList(Long pRoleId) {
        Role role = roleRepository_.findOne(pRoleId);
        return role.getProjectUsers();
    }

    @Override
    public boolean existRole(Long pRoleId) {
        return roleRepository_.exists(pRoleId);
    }

    @Override
    public boolean existRole(Role pRole) {
        return roleRepository_.exists(pRole.getId());
    }

    @Override
    public Role getDefaultRole() {
        return roleRepository_.findByIsDefault(true);
    }

    /**
     * Return true if {@link pRole} is an ancestor of {@link pOther} through the {@link Role#getParentRole()} chain.
     */
    @Override
    public boolean isHierarchicallyInferior(Role pRole, Role pOther) {

        RoleLineageAssembler roleLineageAssembler = new RoleLineageAssembler();
        List<Role> ancestors = roleLineageAssembler.of(pOther).get();

        return ancestors.contains(pRole);
    }

}
