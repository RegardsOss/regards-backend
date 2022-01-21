/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Crawls through a role's {@link Role#getParentRole()} streak and builds the list of all visited roles like so,
 * establishing the given role's lineage. This lineage only contains native role.
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
