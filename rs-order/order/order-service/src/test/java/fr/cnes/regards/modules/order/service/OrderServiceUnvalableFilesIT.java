/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.service;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.test.ServiceConfigurationWithFilesNotAvailable;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ServiceConfigurationWithFilesNotAvailable.class)
@ActiveProfiles("test")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@DirtiesContext
public class OrderServiceUnvalableFilesIT {

    @Autowired
    private IOrderService orderService;

    @Autowired
    private IOrderRepository orderRepos;

    @Autowired
    private IOrderDataFileService orderDataFileService;

    @Autowired
    private IOrderJobService orderJobService;

    @Autowired
    private IOrderDataFileRepository dataFileRepos;

    @Autowired
    private IBasketRepository basketRepos;

    @Autowired
    private IJobInfoRepository jobInfoRepos;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IProjectsClient projectsClient;

    @Autowired
    private IJobService jobService;

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    public static final UniformResourceName DS1_IP_ID = UniformResourceName
            .build(OAISIdentifier.AIP, EntityType.DATASET, "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DS2_IP_ID = UniformResourceName
            .build(OAISIdentifier.AIP, EntityType.DATASET, "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DO1_IP_ID = UniformResourceName.build(OAISIdentifier.AIP, EntityType.DATA,
                                                                                  "ORDER", UUID.randomUUID(), 1);

    public static final UniformResourceName DO2_IP_ID = UniformResourceName.build(OAISIdentifier.AIP, EntityType.DATA,
                                                                                  "ORDER", UUID.randomUUID(), 1);

    @Before
    public void init() {
        clean();

        eventPublisher.publishEvent(new ApplicationStartedEvent(Mockito.mock(SpringApplication.class), null, null));

        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.REGISTERED_USER.toString());
        Project project = new Project();
        project.setHost("regardsHost");
        Mockito.when(projectsClient.retrieveProject(Mockito.anyString()))
                .thenReturn(new ResponseEntity<>(new EntityModel<>(project), HttpStatus.OK));
    }

    public void clean() {
        basketRepos.deleteAll();
        orderRepos.deleteAll();
        dataFileRepos.deleteAll();

        jobInfoRepos.deleteAll();
    }

    private BasketSelectionRequest createBasketSelectionRequest(String query) {
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setEngineType("engine");
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("q", query);
        request.setSearchParameters(parameters);
        return request;
    }

    private BasketDatedItemsSelection createDatasetItemSelection(long filesSize, long filesCount, int objectsCount,
            String query) {

        BasketDatedItemsSelection item = new BasketDatedItemsSelection();
        item.setFileTypeCount(DataType.RAWDATA.name()+"_ref", 0L);
        item.setFileTypeSize(DataType.RAWDATA.name()+"_ref", 0L);
        item.setFileTypeCount(DataType.RAWDATA.name()+"_!ref", filesCount);
        item.setFileTypeSize(DataType.RAWDATA.name()+"_!ref", filesSize);
        item.setFileTypeCount(DataType.RAWDATA.name(), filesCount);
        item.setFileTypeSize(DataType.RAWDATA.name(), filesSize);
        item.setObjectsCount(objectsCount);
        item.setDate(OffsetDateTime.now());
        item.setSelectionRequest(createBasketSelectionRequest(query));
        return item;
    }

    @Test
    public void testBucketsJobswithUnavalableFiles() throws IOException, InterruptedException, EntityInvalidException {
        String user = "tulavu@qui.fr";
        Basket basket = new Basket(user);
        BasketDatasetSelection dsSelection = new BasketDatasetSelection();
        dsSelection.setDatasetIpid(DS1_IP_ID.toString());
        dsSelection.setDatasetLabel("DS");
        dsSelection.setObjectsCount(3);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name()+"_ref", 0L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name()+"_ref", 0L);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name()+"_!ref", 12L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name()+"_!ref", 3_000_171L);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name(), 12L);
        dsSelection.setFileTypeSize(DataType.RAWDATA.name(), 3_000_171L);
        dsSelection.addItemsSelection(createDatasetItemSelection(3_000_171L, 12, 3, "ALL"));
        basket.addDatasetSelection(dsSelection);
        basketRepos.save(basket);

        Order order = orderService.createOrder(basket, "perdu","http://perdu.com");
        Thread.sleep(10_000);
        List<JobInfo> jobInfos = jobInfoRepo.findAllByStatusStatus(JobStatus.QUEUED);
        Assert.assertEquals(2, jobInfos.size());

        List<OrderDataFile> files = dataFileRepos.findAllAvailables(order.getId());
        Assert.assertEquals(0, files.size());

        jobInfos.forEach(j -> {
            try {
                JobInfo ji = jobInfoRepo.findCompleteById(j.getId());
                jobService.runJob(ji, "ORDER").get();
                tenantResolver.forceTenant("ORDER");
            } catch (InterruptedException | ExecutionException e) {
                tenantResolver.forceTenant("ORDER");
                Assert.fail(e.getMessage());
            }
        });

        // Some files are in error
        files = dataFileRepos.findAllAvailables(order.getId());
        int firstAvailables = files.size();

        // Download all available files
        files.forEach(f -> f.setState(FileState.DOWNLOADED));
        orderDataFileService.save(files);
        // Act as true downloads
        orderJobService.manageUserOrderStorageFilesJobInfos(user);
        // Re-wait a while to permit execution of last jobInfo
        Thread.sleep(10_000);

        files = dataFileRepos.findAllAvailables(order.getId());
        order = orderService.loadSimple(order.getId());
        // Error file count on order should be the same as total files - available files
        Assert.assertEquals(12 - files.size() - firstAvailables, order.getFilesInErrorCount());
        // But order should be at 100 % ever
        Assert.assertEquals(100, order.getPercentCompleted());
        Assert.assertEquals(OrderStatus.FAILED, order.getStatus());
    }
}
