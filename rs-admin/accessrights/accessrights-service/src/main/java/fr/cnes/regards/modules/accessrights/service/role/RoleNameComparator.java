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
package fr.cnes.regards.modules.accessrights.service.role;

import fr.cnes.regards.modules.accessrights.domain.projects.Role;

import java.util.Comparator;
import java.util.Objects;

/**
 * @author Stephane Cortine
 */
public class RoleNameComparator implements Comparator<Role> {

    @Override
    public int compare(Role role, Role other) {
        Objects.requireNonNull(role, "Role must not be Null");
        Objects.requireNonNull(other, "Role must not be Null");

        if (Objects.equals(role, other)) {
            return 0;
        }
        //compareTo from String return positive or negative value, we just want to return 1 or -1
        return role.getName().compareTo(other.getName()) > 0 ? 1 : -1;
    }

}
