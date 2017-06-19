/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.role;

import java.util.Comparator;

import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * @author Marc Sordi
 *
 */
public class RoleComparator implements Comparator<Role> {

    private final IRoleService roleService;

    public RoleComparator(IRoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    public int compare(Role role, Role other) {

        // Compare with native role
        Role refRole = role;
        if (!role.isNative()) {
            refRole = role.getParentRole();
        }

        if (roleService.isHierarchicallyInferior(refRole, other)) {
            return -1;
        } else {
            return 1;
        }

    }

}
