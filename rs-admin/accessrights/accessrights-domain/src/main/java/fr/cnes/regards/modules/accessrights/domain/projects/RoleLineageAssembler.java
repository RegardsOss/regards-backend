package fr.cnes.regards.modules.accessrights.domain.projects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Crawls through a role's {@link Role#getParentRole()} streak and builds the list of all visited roles like so,
 * establishing the passed role's lineage.
 *
 * @author xbrochard
 * @author Sébastien Binda
 *
 */
public class RoleLineageAssembler {

    /**
     * The root role
     */
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
