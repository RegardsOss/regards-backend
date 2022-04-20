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
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.licence.LicenseService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Fache
 **/
public class LicenseServiceSetup {

    private final ProjectUser givenUser;

    private final Project givenProject;

    public IProjectUserService userService;

    public IPublisher publisher;

    private LicenseService licenseService;

    public LicenseServiceSetup(ProjectUser forUser, Project forProject) {
        givenUser = forUser;
        givenProject = forProject;
        licenseService = new LicenseService();
    }

    public LicenseService get() {
        return licenseService;
    }

    public void setup() {
        licenseService = new LicenseService();
        ReflectionTestUtils.setField(licenseService, "projectUserService", mockUserAccesses());
        ReflectionTestUtils.setField(licenseService, "projectsClient", mockProject());
        ReflectionTestUtils.setField(licenseService, "authResolver", mockAuthentification());
        ReflectionTestUtils.setField(licenseService, "runtimeTenantResolver", mockTenant());
        ReflectionTestUtils.setField(licenseService, "publisher", mockPublisher());
    }

    private IPublisher mockPublisher() {
        publisher = mock(IPublisher.class);
        return publisher;
    }

    private IProjectUserService mockUserAccesses() {
        userService = mock(IProjectUserService.class);
        mockUser();
        return userService;
    }

    private void mockUser() {
        try {
            when(userService.retrieveCurrentUser()).thenAnswer(i -> fakeUser());
        } catch (EntityNotFoundException e) {
            fail("Problem while mocking current user");
        }
    }

    private ProjectUser fakeUser() throws EntityNotFoundException {
        if (givenUser.equals(LicenseTestFactory.aPublicUser())) {
            throw new EntityNotFoundException("public user not handled.");
        } else if (givenUser.equals(LicenseTestFactory.anInstanceAdmin())) {
            throw new EntityNotFoundException("instance admin not handled.");
        }
        return givenUser;
    }

    private IProjectsClient mockProject() {
        IProjectsClient projectClient = mock(IProjectsClient.class);
        when(projectClient.retrieveProject(givenProject.getName())).thenAnswer(i -> fakeProject());
        return projectClient;
    }

    private ResponseEntity<EntityModel<Project>> fakeProject() throws EntityNotFoundException {
        if (givenProject.getName().equals(LicenseTestFactory.A_MISSING_PROJECT)) {
            throw new EntityNotFoundException("not found");
        }
        return projectResponse(givenProject);
    }

    private ResponseEntity<EntityModel<Project>> projectResponse(Project project) {
        int status = HttpStatus.OK.value();
        HttpHeaders headers = new HttpHeaders();
        EntityModel<Project> body = new EntityModel<>(project);
        return ResponseEntity.status(status).headers(headers).body(body);
    }

    private IAuthenticationResolver mockAuthentification() {
        IAuthenticationResolver authResolver = mock(IAuthenticationResolver.class);
        mockRole(authResolver);
        mockAuthUser(authResolver);
        return authResolver;
    }

    private void mockRole(IAuthenticationResolver authResolver) {
        when(authResolver.getRole()).thenReturn(givenUser.getRole().getName());
    }

    private void mockAuthUser(IAuthenticationResolver authResolver) {
        when(authResolver.getUser()).thenReturn(givenUser.getEmail());
    }

    private IRuntimeTenantResolver mockTenant() {
        IRuntimeTenantResolver tenantResolver = mock(IRuntimeTenantResolver.class);
        when(tenantResolver.getTenant()).thenReturn(givenProject.getName());
        return tenantResolver;
    }

}
