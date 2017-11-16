/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.service;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.LicenseDTO;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.licence.LicenseService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class LicenseServiceTest {

    private IProjectsClient projectClient;

    private IProjectUserService projectUserService;

    private IAuthenticationResolver authResolver;

    private LicenseService licenseService;

    private Project projectWithLicense;

    private ProjectUser currentUser;

    private Project projectWithoutLicense;

    @Before
    public void init() throws EntityNotFoundException {

        projectClient = Mockito.mock(IProjectsClient.class);
        projectUserService = Mockito.mock(IProjectUserService.class);

        projectWithLicense = new Project("desc", "icon", true, "WITH_LICENSE");
        projectWithLicense.setLicenseLink("URL");
        projectWithoutLicense = new Project("desc", "icon", true, "WITHOUT_LICENSE");
        Mockito.when(projectClient.retrieveProject(projectWithLicense.getName()))
                .thenReturn(new ResponseEntity<Resource<Project>>(HateoasUtils.wrap(projectWithLicense),
                        HttpStatus.OK));
        Mockito.when(projectClient.retrieveProject(projectWithoutLicense.getName()))
                .thenReturn(new ResponseEntity<Resource<Project>>(HateoasUtils.wrap(projectWithoutLicense),
                        HttpStatus.OK));

        currentUser = new ProjectUser("", new Role(DefaultRole.PUBLIC.toString(), null), new ArrayList<>(),
                new ArrayList<>());
        Mockito.when(projectUserService.retrieveCurrentUser()).thenReturn(currentUser);

        authResolver = Mockito.mock(IAuthenticationResolver.class);
        Mockito.when(authResolver.getRole()).thenReturn(currentUser.getRole().getName());

        licenseService = new LicenseService(projectUserService, projectClient, authResolver);
    }

    @Test
    public void testRetrieveState() throws EntityNotFoundException {

        LicenseDTO dto = licenseService.retrieveLicenseState(projectWithLicense.getName());
        Assert.assertEquals(projectWithLicense.getLicenceLink(), dto.getLicenceLink());
        Assert.assertEquals(currentUser.isLicenseAccepted(), dto.isAccepted());

        dto = licenseService.retrieveLicenseState(projectWithoutLicense.getName());
        Assert.assertEquals("", dto.getLicenceLink());
        Assert.assertEquals(true, dto.isAccepted());
    }

    @Test
    public void testAccept() throws EntityException {
        LicenseDTO dto = licenseService.acceptLicense(projectWithLicense.getName());
        Assert.assertEquals(projectWithLicense.getLicenceLink(), dto.getLicenceLink());
        Assert.assertEquals(true, currentUser.isLicenseAccepted());
    }

}
