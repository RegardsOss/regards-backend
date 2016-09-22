package fr.cnes.regards.modules.accessRights.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.cnes.regards.modules.accessRights.domain.Role;

/**
 * Crawls through a role's {@link Role#getParentRole()} streak and builds the list of all visited roles like so,
 * establishing the passed role's lineage.
 *
 * @author xbrochard
 *
 */
public class RoleLineageAssembler {

    private Role role_;

    public RoleLineageAssembler on(Role pRole) {
        role_ = pRole;
        return this;
    }

    public List<Role> get() {
        List<Role> ancestors = new ArrayList<>();
        Iterator<Role> iterator = new RoleLineageIterator(role_);

        while (iterator.hasNext()) {
            Role role = iterator.next();
            ancestors.add(role);
        }

        return ancestors;
    }

}
