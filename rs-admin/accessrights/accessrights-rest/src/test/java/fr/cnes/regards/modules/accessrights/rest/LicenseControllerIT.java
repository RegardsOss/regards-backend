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
package fr.cnes.regards.modules.accessrights.rest;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.projects.events.LicenseAction;
import fr.cnes.regards.modules.accessrights.domain.projects.events.LicenseEvent;
import fr.cnes.regards.modules.accessrights.service.projectuser.QuotaHelperService;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test {@link LicenseController}
 *
 * @author Marc Sordi
 */
@MultitenantTransactional
@TestPropertySource(
    properties = { "spring.jpa.properties.hibernate.default_schema=license", "regards.amqp.enabled=true" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
public class LicenseControllerIT extends AbstractRegardsTransactionalIT {

    @MockBean
    private QuotaHelperService quotaHelperService;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IProjectUserRepository projectUserRepository;

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private IProjectsClient projectsClient;

    private IHandler<LicenseEvent> handler;

    private LicenseEvent emittedEvent;

    @Before
    public void setupDefaultUser() {
        Role publicRole = roleRepository.findOneByName(DefaultRole.PUBLIC.toString())
                                        .orElseThrow(() -> new AssertionError("Public role isn't setup in Role Database"));

        ProjectUserSetup userSetup = new ProjectUserSetup(projectUserRepository);
        userSetup.addUser(getDefaultUserEmail(),
                          publicRole,
                          Collections.emptyList(),
                          Collections.singletonList("group1"));
    }

    @Before
    public void mockProjectRetrieve() {
        when(projectsClient.retrieveProject(getDefaultTenant())).thenReturn(defaultTenantResponse());
    }

    private ResponseEntity<EntityModel<Project>> defaultTenantResponse() {
        return ResponseEntity.status(HttpStatus.OK.value())
                             .headers(new HttpHeaders())
                             .body(EntityModel.of(aProjectWithLicence()));
    }

    private Project aProjectWithLicence() {
        Project aProject = new Project();
        aProject.setName("A_PROJECT_WITH_LICENSE");
        aProject.setLicenseLink("link/to/license");
        return aProject;
    }

    @Test
    @Purpose("Check license agreement can be reset by an ADMIN")
    public void resetLicense() {
        performDefaultPut(LicenseController.PATH_LICENSE + LicenseController.PATH_RESET,
                          null,
                          customizer().expectStatusNoContent(),
                          "Error retrieving endpoints",
                          getDefaultTenant());
    }

    @Test
    @Ignore("Event is never retrieved")
    public void accept_license_send_notification() throws Exception {
        setupLicenseEventHandler();

        performDefaultPut(LicenseController.PATH_LICENSE,
                          null,
                          customizer().expectStatusOk(),
                          "Error retrieving endpoints",
                          getDefaultTenant());

        // FIXME the event is never retrieved.
        // However, the messaging queue and the event seem well configured
        assertThat(emittedEvent).isNotNull();
        assertThat(emittedEvent.getAction()).isEqualTo(LicenseAction.ACCEPT);
        assertThat(emittedEvent.getUser()).isEqualTo(getDefaultUserEmail());
        assertThat(emittedEvent.getLicenseLink()).isEmpty();
    }

    private void setupLicenseEventHandler() {
        handler = new IHandler<LicenseEvent>() {

            @Override
            public void handle(String tenant, LicenseEvent message) {
                emittedEvent = message;
            }
        };
        subscriber.subscribeTo(LicenseEvent.class, handler);
    }

}
