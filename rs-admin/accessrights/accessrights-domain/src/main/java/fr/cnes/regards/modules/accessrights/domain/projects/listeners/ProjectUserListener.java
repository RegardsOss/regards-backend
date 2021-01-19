/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.domain.projects.listeners;

import java.time.OffsetDateTime;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Entity listener<br>
 * Allows to intercept the entity manager in the lifecycle of an entity.
 *
 * @author Xavier-Alexandre Brochard
 */
public class ProjectUserListener {

    /**
     * Automatically set lastUpate before any database persistence. This is intended to be the only place where lastUpdate
     * is updated.
     *
     * @param pProjectUser
     *            The listened project user
     */
    @PreUpdate
    @PrePersist
    public void setLastUpdate(final ProjectUser pProjectUser) {
        pProjectUser.setLastUpdate(OffsetDateTime.now());
    }
}
