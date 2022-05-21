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
package fr.cnes.regards.modules.access.services.domain.user;

import fr.cnes.regards.modules.accessrights.domain.projects.Role;

public class ProjectUserUpdateDto extends ProjectUserBaseDto {

    private Long id;

    private Role role;

    public Long getId() {
        return id;
    }

    public ProjectUserUpdateDto setId(Long id) {
        this.id = id;
        return this;
    }

    public Role getRole() {
        return role;
    }

    public ProjectUserUpdateDto setRole(Role role) {
        this.role = role;
        return this;
    }

}
