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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.licence.LicenseService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.project.domain.Project;

public abstract class LicenseServiceTest {

    protected ProjectUser givenUser;

    protected Project givenProject;

    protected LicenseService licenseService;

    protected IProjectUserService userService;

    protected IPublisher publisher;

    protected LicenseServiceTest() {
        givenUser = LicenseTestFactory.unknownUser();
        givenProject = LicenseTestFactory.unknownProject();
    }

    protected LicenseService givenLicenseService() {
        LicenseServiceSetup licenseServiceSetup = new LicenseServiceSetup(givenUser, givenProject);
        licenseServiceSetup.setup();
        userService = licenseServiceSetup.userService;
        publisher = licenseServiceSetup.publisher;
        return licenseServiceSetup.get();
    }
}
