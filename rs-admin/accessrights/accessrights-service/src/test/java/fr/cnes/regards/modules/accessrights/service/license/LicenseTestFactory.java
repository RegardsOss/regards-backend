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
package fr.cnes.regards.modules.accessrights.service.license;

import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.project.domain.Project;

/**
 * @author Thomas Fache
 **/
public class LicenseTestFactory {

    public static final String A_MISSING_PROJECT = "A_MISSING_PROJECT";

    protected static final String A_PROJECT_WITH_LICENSE = "A_PROJECT";

    protected static final String A_PROJECT_WITHOUT_LICENSE = "A_PROJECT_WITHOUT_LICENSE";

    protected static final String LINK_TO_LICENSE = "link/to/license";

    protected static final long EXPLOIT_ID = 1L;

    protected static final long INSTANCE_ADMIN_ID = 2L;

    public static ProjectUser anInstanceAdmin() {
        ProjectUser user = new ProjectUser();
        user.setId(INSTANCE_ADMIN_ID);
        user.setRole(new Role(DefaultRole.INSTANCE_ADMIN.toString()));
        user.setEmail("admin@email.fr");
        return user;
    }

    public static ProjectUser anExploitWithoutLicense() {
        ProjectUser user = new ProjectUser();
        user.setId(EXPLOIT_ID);
        user.setRole(new Role(DefaultRole.EXPLOIT.name()));
        user.setEmail("exploit@email.fr");
        user.setLicenseAccepted(false);
        return user;
    }

    public static ProjectUser unknownUser() {
        ProjectUser user = new ProjectUser();
        user.setRole(new Role(DefaultRole.PUBLIC.name()));
        user.setId(-1L);
        user.setEmail("unknown@email.fr");
        return user;
    }

    public static Project aProjectWithLicence() {
        Project aProject = new Project();
        aProject.setName(A_PROJECT_WITH_LICENSE);
        aProject.setLicenseLink(LINK_TO_LICENSE);
        return aProject;
    }

    public static Project aProjectWithEmptyLicense() {
        Project aProject = new Project();
        aProject.setName(A_PROJECT_WITHOUT_LICENSE);
        aProject.setLicenseLink("");
        return aProject;
    }

    public static Project aProjectWithoutLicense() {
        Project aProject = new Project();
        aProject.setName(A_PROJECT_WITHOUT_LICENSE);
        return aProject;
    }

    public static Project aMissingProject() {
        Project aProject = new Project();
        aProject.setName(A_MISSING_PROJECT);
        return aProject;
    }

    public static Project unknownProject() {
        Project aProject = new Project();
        aProject.setName("UNKNOWN_PROJECT");
        return aProject;
    }
}
