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
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events;

import org.springframework.context.ApplicationEvent;

import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Use this event in order to react to when the admin denies access of a project user.
 *
 * @author Xavier-Alexandre Brochard
 */
@SuppressWarnings("serial")
public class OnInactiveEvent extends ApplicationEvent {

    /**
     * The project user
     */
    private final ProjectUser projectUser;

    /**
     * @param pProjectUser
     */
    public OnInactiveEvent(ProjectUser pProjectUser) {
        super(pProjectUser);
        projectUser = pProjectUser;
    }

    /**
     * @return the projectUser
     */
    public ProjectUser getProjectUser() {
        return projectUser;
    }

}