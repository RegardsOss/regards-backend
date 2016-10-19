package fr.cnes.regards.modules.accessRights.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fr.cnes.regards.modules.accessRights.domain.projects.Role;

/**
 * Crawls through a role's {@link Role#getParentRole()} streak and builds the list of all visited roles like so,
 * establishing the passed role's lineage.
 *
 * @author xbrochard
 *
 */
public class RoleLineageAssembler {

    private Role role;

    public RoleLineageAssembler of(final Role pRole) {
        role = pRole;
        return this;
    }

    public List<Role> get() {
        final List<Role> ancestors = new ArrayList<>();
        final Iterator<Role> iterator = new RoleLineageIterator(role);

        while (iterator.hasNext()) {
            final Role nextRole = iterator.next();
            ancestors.add(nextRole);
        }

        return ancestors;
    }

}
