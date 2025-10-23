/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import fr.cnes.regards.modules.order.dto.dto.BasketSelectionRequest;
import fr.cnes.regards.modules.order.dto.dto.OrderStatus;
import fr.cnes.regards.modules.order.service.IOrderCreationService;
import fr.cnes.regards.modules.order.service.IOrderService;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.search.client.IComplexSearchClient;
import fr.cnes.regards.modules.search.domain.plugin.legacy.FacettedPagedModel;
import fr.cnes.regards.modules.search.dto.ComplexSearchRequest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static fr.cnes.regards.modules.order.service.OrderService.DEFAULT_CORRELATION_ID_FORMAT;

/**
 * @author Sébastien Binda
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = PerfServiceConfiguration.class)
@ActiveProfiles("test")
@Ignore("Run for performance tests")
public class OrderPerformanceIT extends AbstractMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderPerformanceIT.class);

    @Autowired
    private IBasketRepository basketRepos;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private IOrderCreationService orderCreationService;

    @Autowired
    private IProjectsClient projectsClient;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IOrderRepository orderRepo;

    @Autowired
    private IComplexSearchClient searchClient;

    private final static int MAX_PAGE = 5;

    @Before
    public void init() {
        basketRepos.deleteAll();

        simulateApplicationReadyEvent();
        simulateApplicationStartedEvent();

        Mockito.when(authResolver.getRole()).thenReturn(DefaultRole.REGISTERED_USER.toString());
        Project project = new Project();
        project.setHost("regardsHost");
        Mockito.when(projectsClient.retrieveProject(Mockito.anyString()))
               .thenReturn(new ResponseEntity<>(EntityModel.of(project), HttpStatus.OK));

        Mockito.when(searchClient.searchDataObjects(Mockito.any())).thenAnswer(invocation -> {
            ComplexSearchRequest r = invocation.getArgument(0);
            int page = r.getPage();
            if (page < MAX_PAGE) {
                try {
                    LOGGER.info("Getting page " + page + "....");
                    List<EntityModel<EntityFeature>> list = new ArrayList<>();
                    int nbDataPerPage = 0;
                    do {
                        String id = "data_" + page + "_" + nbDataPerPage;
                        EntityFeature feature = new DataObjectFeature(UniformResourceName.build(id,
                                                                                                EntityType.DATA,
                                                                                                getDefaultTenant(),
                                                                                                UUID.randomUUID(),
                                                                                                1), id, id);
                        Multimap<DataType, DataFile> fileMultimap = ArrayListMultimap.create();
                        DataFile dataFile = new DataFile();
                        dataFile.setOnline(false);
                        dataFile.setUri(new URI("file:///test/" + id).toString());
                        dataFile.setFilename(id);
                        dataFile.setFilesize(10L);
                        dataFile.setReference(false);
                        dataFile.setChecksum(UUID.randomUUID().toString());
                        dataFile.setDigestAlgorithm("MD5");
                        dataFile.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
                        dataFile.setDataType(DataType.RAWDATA);
                        fileMultimap.put(DataType.RAWDATA, dataFile);

                        feature.setFiles(fileMultimap);
                        list.add(EntityModel.of(feature));
                        nbDataPerPage++;
                    } while (nbDataPerPage < r.getSize());
                    LOGGER.info("Getting page " + page + " done !");
                    return ResponseEntity.ok(new FacettedPagedModel<>(Sets.newHashSet(),
                                                                      list,
                                                                      new PagedModel.PageMetadata(list.size(),
                                                                                                  0,
                                                                                                  list.size())));
                } catch (URISyntaxException e) {
                    throw new RsRuntimeException(e);
                }
            }
            LOGGER.info("Getting page " + page + " done !");
            return ResponseEntity.ok(new FacettedPagedModel<>(Sets.newHashSet(),
                                                              Collections.emptyList(),
                                                              new PagedModel.PageMetadata(0, 0, 0)));
        });
    }

    @Test
    public void test() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Used Memory before : " + (usedMemoryBefore / 1024) + "Ko");

        Basket basket = new Basket();
        basket.setOwner("test");
        BasketDatasetSelection dsSelection = new BasketDatasetSelection();
        dsSelection.setDatasetLabel("DS1");
        dsSelection.setDatasetIpid("DS_1");
        dsSelection.setFileTypeSize(DataType.RAWDATA.name(), 1_000_000L);
        dsSelection.setFileTypeCount(DataType.RAWDATA.name(), 1L);
        dsSelection.setObjectsCount(1);
        dsSelection.addItemsSelection(createDatasetItemSelection(1_000_001L, 1, 1, "someone:something"));
        basket.addDatasetSelection(dsSelection);
        basket = basketRepos.save(basket);

        Order order = new Order();
        order.setCreationDate(OffsetDateTime.now());
        order.setOwner(basket.getOwner());
        order.setFrontendUrl("plop");
        order.setStatus(OrderStatus.PENDING);
        order.setCorrelationId(String.format(DEFAULT_CORRELATION_ID_FORMAT, UUID.randomUUID()));
        // expiration date is set during asyncCompleteOrderCreation execution
        // To generate orderId
        order = orderRepo.save(order);

        orderCreationService.completeOrderCreation(basket,
                                                   order.getId(),
                                                   DefaultRole.REGISTERED_USER.toString(),
                                                   240,
                                                   getDefaultTenant());

        // working code here
        long usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Memory increased: " + ((usedMemoryAfter - usedMemoryBefore) / 1024) + "Ko");
    }

    private BasketDatedItemsSelection createDatasetItemSelection(long filesSize,
                                                                 long filesCount,
                                                                 int objectsCount,
                                                                 String query) {

        BasketDatedItemsSelection item = new BasketDatedItemsSelection();
        item.setFileTypeSize(DataType.RAWDATA.name(), filesSize);
        item.setFileTypeCount(DataType.RAWDATA.name(), filesCount);
        item.setObjectsCount(objectsCount);
        item.setDate(OffsetDateTime.now());
        item.setSelectionRequest(createBasketSelectionRequest(query));
        return item;
    }

    private BasketSelectionRequest createBasketSelectionRequest(String query) {
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setEngineType("engine");
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("q", query);
        request.setSearchParameters(parameters);
        return request;
    }

}
