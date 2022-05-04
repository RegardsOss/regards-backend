/*
 * Copyright 2017-20XX CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.rest;

import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author Thomas Fache
 **/
public class ProjectUserSetup {

    private final IProjectUserRepository userRepository;

    public ProjectUserSetup(IProjectUserRepository projectUserRepository) {
        userRepository = projectUserRepository;
    }

    public ProjectUser addUser(String mail, Role role, List<MetaData> metadata, List<String> accessGroups) {
        return userRepository.save(createUser(mail, role, metadata, accessGroups));
    }

    private ProjectUser createUser(String mail, Role role, List<MetaData> metadata, List<String> accessGroups) {
        ProjectUser user = new ProjectUser(mail, role, new ArrayList<>(), new HashSet<>(metadata));
        user.setAccessGroups(new HashSet<>(accessGroups));
        return user;
    }

}
