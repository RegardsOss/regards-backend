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
package fr.cnes.regards.modules.order.service.commons;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.order.dao.*;
import fr.cnes.regards.modules.order.dto.dto.OrderStatus;
import fr.cnes.regards.modules.order.service.*;
import fr.cnes.regards.modules.order.service.settings.OrderSettingsService;
import fr.cnes.regards.modules.order.test.ServiceConfiguration;
import fr.cnes.regards.modules.order.test.StorageClientMock;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;

/**
 * @author Thomas GUILLOU
 **/
@ActiveProfiles(value = { "default", "test", "testAmqp" }, inheritProfiles = false)
@ContextConfiguration(classes = ServiceConfiguration.class)
public abstract class AbstractOrderServiceIT extends AbstractMultitenantServiceIT {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractOrderServiceIT.class);

    protected static final String URL = "http://frontend.com";

    protected static final UniformResourceName OBJECT_IP_ID = UniformResourceName.fromString(
        "URN:AIP:DATA:ORDER:00000000-0000-0002-0000-000000000002:V1");

    @Autowired
    protected IOrderService orderService;

    @Autowired
    protected IOrderDownloadService orderDownloadService;

    @Autowired
    protected IOrderRepository orderRepository;

    @Autowired
    protected IOrderDataFileRepository orderDataFileRepository;

    @Autowired
    protected IFilesTasksRepository filesTasksRepository;

    @Autowired
    protected IOrderDataFileService dataFileService;

    @Autowired
    protected IBasketRepository basketRepository;

    @Autowired
    protected BasketService basketService;

    @Autowired
    protected IDatasetTaskRepository datasetTaskRepository;

    @Autowired
    protected IJobInfoRepository jobInfoRepository;

    @Autowired
    protected IAuthenticationResolver authenticationResolver;

    @Autowired
    protected IProjectsClient projectsClient;

    @Autowired
    protected IRuntimeTenantResolver tenantResolver;

    @Autowired
    protected StorageClientMock storageClientMock;

    @MockBean
    protected IProjectUsersClient projectUsersClient;

    @Autowired
    protected OrderSettingsService orderSettingsService;

    @Autowired
    protected OrderHelperService orderHelperService;

    @Autowired
    protected ITenantResolver tenantsResolver;

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    public void clean() {
        filesTasksRepository.deleteAll();
        jobInfoRepository.deleteAll();
        orderDataFileRepository.deleteAll();
        datasetTaskRepository.deleteAll();
        orderRepository.deleteAll();
        basketRepository.deleteAll();
    }

    @Before
    public void init() {
        Mockito.clearInvocations(publisher);
        tenantResolver.forceTenant(getDefaultTenant());
        storageClientMock.setWaitMode(false);
        clean();
        Mockito.when(authenticationResolver.getRole()).thenAnswer(i -> {
            LOGGER.info("Asking for role");
            return DefaultRole.REGISTERED_USER.toString();
        });

        Project project = new Project();
        project.setHost("regardsHost");
        Mockito.when(projectsClient.retrieveProject(Mockito.anyString()))
               .thenReturn(new ResponseEntity<>(EntityModel.of(project), HttpStatus.OK));

        Role role = new Role();
        role.setName(DefaultRole.REGISTERED_USER.name());
        ProjectUser projectUser = new ProjectUser();
        projectUser.setRole(role);
        Mockito.when(projectUsersClient.isAdmin(any())).thenReturn(ResponseEntity.ok(false));
        Mockito.when(projectUsersClient.retrieveProjectUserByEmail(Mockito.anyString()))
               .thenReturn(new ResponseEntity<>(EntityModel.of(projectUser), HttpStatus.OK));
        simulateApplicationReadyEvent();
        simulateApplicationStartedEvent();
    }

    protected void waitForStatus(Long orderId, OrderStatus status) {
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return orderService.loadComplete(orderId).getStatus().equals(status);
        });
    }

    protected void waitForPausedStatus(Long orderId) {
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return orderService.isPaused(orderId);
        });
    }

    protected void waitForWaitingForUser(Long orderId) {
        Awaitility.await().atMost(60, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(getDefaultTenant());
            return orderService.loadComplete(orderId).isWaitingForUser();
        });
    }
}
