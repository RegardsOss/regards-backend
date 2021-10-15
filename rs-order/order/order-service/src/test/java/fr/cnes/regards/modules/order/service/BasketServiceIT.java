/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.domain.basket.DataTypeSelection;
import fr.cnes.regards.modules.order.domain.exception.EmptyBasketException;
import fr.cnes.regards.modules.order.domain.exception.EmptySelectionException;
import fr.cnes.regards.modules.order.domain.exception.TooManyItemsSelectedInBasketException;
import fr.cnes.regards.modules.order.domain.process.ProcessDatasetDescription;
import fr.cnes.regards.modules.order.service.processing.AbstractOrderProcessingServiceIT;
import fr.cnes.regards.modules.order.test.SearchClientMock;
import static fr.cnes.regards.modules.order.test.SearchClientMock.DS1_IP_ID;
import static fr.cnes.regards.modules.order.test.SearchClientMock.DS2_IP_ID;
import static fr.cnes.regards.modules.order.test.SearchClientMock.DS3_IP_ID;
import fr.cnes.regards.modules.order.test.ServiceConfiguration;
import fr.cnes.regards.modules.processing.forecast.MultiplierResultSizeForecast;
import fr.cnes.regards.modules.processing.order.Cardinality;
import fr.cnes.regards.modules.processing.order.OrderProcessInfo;
import fr.cnes.regards.modules.processing.order.OrderProcessInfoMapper;
import fr.cnes.regards.modules.processing.order.Scope;
import fr.cnes.regards.modules.processing.order.SizeLimit;
import io.vavr.collection.List;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ServiceConfiguration.class)
@ActiveProfiles("test")
public class BasketServiceIT extends AbstractOrderProcessingServiceIT {

    @Autowired
    private IOrderMaintenanceService orderMaintenanceService;

    private static final String USER_EMAIL = "test@test.fr";

    @Before
    public void setUp() {
        Mockito.when(authResolver.getUser()).thenReturn(USER_EMAIL);
        Role role = new Role();
        role.setName(DefaultRole.REGISTERED_USER.name());
        ProjectUser projectUser = new ProjectUser();
        projectUser.setRole(role);
        Mockito.when(projectUsersClient.retrieveProjectUserByEmail(Mockito.anyString())).thenReturn(new ResponseEntity<>(new EntityModel<>(projectUser), HttpStatus.OK));
    }

    private BasketSelectionRequest createBasketSelectionRequest(String datasetUrn, String query) {
        BasketSelectionRequest request = new BasketSelectionRequest();
        request.setEngineType("engine");
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("q", query);
        request.setSearchParameters(parameters);
        request.setDatasetUrn(datasetUrn);
        return request;
    }

    /**
     * BECAUSE OF OffsetDateTime.now() used by BasketService, THIS TEST CLASS MUST DEFINE ONLY ONE TEST
     */
    @Test
    @Requirement("REGARDS_DSL_STO_CMD_100")
    public void test() throws EmptyBasketException, EmptySelectionException, EntityInvalidException, TooManyItemsSelectedInBasketException {
        Basket basket = basketService.findOrCreate(USER_EMAIL);

        Assert.assertNotNull(basketService.find(USER_EMAIL));

        // Add a selection on DS1 => 2 documents, 2 RAWDATA files + 6 QUICKLOOKS 2 x 3 of each size, 1 Mb each RAW
        // file, 500 b QUICKLOOK SD, 1 kb MD, 500 kb HD

        basketService.addSelection(basket.getId(), createBasketSelectionRequest(DS1_IP_ID.toString(), ""));

        basket = basketService.load(basket.getId());
        Assert.assertEquals(1, basket.getDatasetSelections().size());
        BasketDatasetSelection dsSelection = basket.getDatasetSelections().first();
        Assert.assertEquals(DS1_IP_ID.toString(), dsSelection.getDatasetIpid());
        Assert.assertEquals(8L, DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> dsSelection.getFileTypeCount(ft.name())).sum());
        Assert.assertEquals(2, dsSelection.getObjectsCount());
        Assert.assertEquals(3_003_000L, DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> dsSelection.getFileTypeSize(ft.name())).sum());

        // Add a selection on DS2 and DS3 with an opensearch request
        basketService.addSelection(basket.getId(), createBasketSelectionRequest(null, SearchClientMock.QUERY_DS2_DS3));
        basket = basketService.load(basket.getId());
        Assert.assertEquals(3, basket.getDatasetSelections().size());
        for (BasketDatasetSelection dsSel : basket.getDatasetSelections()) {
            // No change on DS1
            if (dsSel.getDatasetIpid().equals(DS1_IP_ID.toString())) {
                Assert.assertEquals(8, DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> dsSel.getFileTypeCount(ft.name())).sum());
                Assert.assertEquals(2, dsSel.getObjectsCount());
                Assert.assertEquals(3_003_000L,
                        DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> dsSel.getFileTypeSize(ft.name())).sum());
            } else if (dsSel.getDatasetIpid().equals(DS2_IP_ID.toString())) {
                Assert.assertEquals(8, DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> dsSel.getFileTypeCount(ft.name())).sum());
                Assert.assertEquals(2, dsSel.getObjectsCount());
                Assert.assertEquals(2_020_202L,
                        DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> dsSel.getFileTypeSize(ft.name())).sum());
            } else if (dsSel.getDatasetIpid().equals(DS3_IP_ID.toString())) {
                Assert.assertEquals(4, DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> dsSel.getFileTypeCount(ft.name())).sum());
                Assert.assertEquals(1, dsSel.getObjectsCount());
                Assert.assertEquals(1_010_101L,
                        DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> dsSel.getFileTypeSize(ft.name())).sum());
            } else {
                Assert.fail("Unknown Dataset !!!");
            }
        }

        // Add a selection on all DS (DS1, 2, 3) : for DS1, same results as previous must be returned
        basketService.addSelection(basket.getId(), createBasketSelectionRequest(null, ""));

        basket = basketService.load(basket.getId());
        Assert.assertEquals(3, basket.getDatasetSelections().size());
        // Computations on dataset selections must not have been changed (concerns same files as previous)
        for (BasketDatasetSelection dsSel : basket.getDatasetSelections()) {
            if (dsSel.getDatasetIpid().equals(DS1_IP_ID.toString())) {
                Assert.assertEquals(8, DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> dsSel.getFileTypeCount(ft.name())).sum());
                Assert.assertEquals(2, dsSel.getObjectsCount());
                Assert.assertEquals(3_003_000L,
                        DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> dsSel.getFileTypeSize(ft.name())).sum());
                // Must have 2 itemsSelections
                Assert.assertEquals(2, dsSel.getItemsSelections().size());
                // And both must have same values as dataset selection (only date changed and opensearch request)
                for (BasketDatedItemsSelection itemsSel : dsSel.getItemsSelections()) {
                    Assert.assertEquals(DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> dsSel.getFileTypeCount(ft.name())).sum(), DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> itemsSel.getFileTypeCount(ft.name())).sum());
                    Assert.assertEquals(DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> dsSel.getFileTypeSize(ft.name())).sum(), DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> itemsSel.getFileTypeSize(ft.name())).sum());
                }
            } else if (dsSel.getDatasetIpid().equals(DS2_IP_ID.toString())) {
                Assert.assertEquals(8, DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> dsSel.getFileTypeCount(ft.name())).sum());
                Assert.assertEquals(2, dsSel.getObjectsCount());
                Assert.assertEquals(2_020_202L,
                        DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> dsSel.getFileTypeSize(ft.name())).sum());
                // Must have 2 itemsSelections
                Assert.assertEquals(2, dsSel.getItemsSelections().size());
                // And both must have same values as dataset selection (only date changed and opensearch request)
                for (BasketDatedItemsSelection itemsSel : dsSel.getItemsSelections()) {
                    Assert.assertEquals(DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> dsSel.getFileTypeCount(ft.name())).sum(), DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> itemsSel.getFileTypeCount(ft.name())).sum());
                    Assert.assertEquals(DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> dsSel.getFileTypeSize(ft.name())).sum(), DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> itemsSel.getFileTypeSize(ft.name())).sum());
                }
            } else if (dsSel.getDatasetIpid().equals(DS3_IP_ID.toString())) {
                Assert.assertEquals(4, DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> dsSel.getFileTypeCount(ft.name())).sum());
                Assert.assertEquals(1, dsSel.getObjectsCount());
                Assert.assertEquals(1_010_101L,
                        DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> dsSel.getFileTypeSize(ft.name())).sum());
                // Must have 2 itemsSelections
                Assert.assertEquals(2, dsSel.getItemsSelections().size());
                // And both must have same values as dataset selection (only date changed and opensearch request)
                for (BasketDatedItemsSelection itemsSel : dsSel.getItemsSelections()) {
                    Assert.assertEquals(DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> dsSel.getFileTypeCount(ft.name())).sum(), DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> itemsSel.getFileTypeCount(ft.name())).sum());
                    Assert.assertEquals(DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> dsSel.getFileTypeSize(ft.name())).sum(), DataTypeSelection.ALL.getFileTypes().stream().mapToLong(ft -> itemsSel.getFileTypeSize(ft.name())).sum());
                }
            } else {
                Assert.fail("Unknown Dataset !!!");
            }
        }

        orderService.createOrder(basket, "perdu", "http://perdu.com", 240);

        // manage periodic email notifications
       orderMaintenanceService.sendPeriodicNotifications();
    }

    @Test
    @Purpose("Test if the selection is in error when too many features are added")
    public void addOversizedSelectionTestForFeatures() throws EmptyBasketException, EmptySelectionException {
       OrderProcessInfo orderProcessInfo =  new OrderProcessInfo(
                Scope.FEATURE,
                Cardinality.ONE_PER_INPUT_FILE,
                List.of(DataType.RAWDATA),
                new SizeLimit(SizeLimit.Type.FEATURES, 2L),
                new MultiplierResultSizeForecast(1d), Boolean.TRUE);
        testSelectionOverProcessSizeLimit(orderProcessInfo, true);
    }

    @Test
    @Purpose("Test if the selection is in error when too many files are added")
    public void addOversizedSelectionTestForFiles() throws EmptyBasketException, EmptySelectionException {
        OrderProcessInfo orderProcessInfo =  new OrderProcessInfo(
                Scope.FEATURE,
                Cardinality.ONE_PER_INPUT_FILE,
                List.of(DataType.RAWDATA),
                new SizeLimit(SizeLimit.Type.FILES, 16L),
                new MultiplierResultSizeForecast(1d), Boolean.TRUE);
        testSelectionOverProcessSizeLimit(orderProcessInfo, true);
    }

    @Test
    @Purpose("Test if the selection is in error when file sizes exceed the process limit")
    public void addOversizedSelectionTestForFileSizes() throws EmptyBasketException, EmptySelectionException {
        OrderProcessInfo orderProcessInfo =  new OrderProcessInfo(
                Scope.FEATURE,
                Cardinality.ONE_PER_INPUT_FILE,
                List.of(DataType.RAWDATA),
                new SizeLimit(SizeLimit.Type.BYTES, 4040404L),
                new MultiplierResultSizeForecast(1d), Boolean.TRUE);
        testSelectionOverProcessSizeLimit(orderProcessInfo, true);
    }


    @Test
    @Purpose("Test if the selection is working when there is no limit")
    public void addOversizedSelectionTestWithNoLimit() throws EmptyBasketException, EmptySelectionException {
        OrderProcessInfo orderProcessInfo =  new OrderProcessInfo(
                Scope.FEATURE,
                Cardinality.ONE_PER_INPUT_FILE,
                List.of(DataType.RAWDATA),
                new SizeLimit(SizeLimit.Type.NO_LIMIT, 0L),
                new MultiplierResultSizeForecast(1d), Boolean.TRUE);
        testSelectionOverProcessSizeLimit(orderProcessInfo, false);
    }


    private void testSelectionOverProcessSizeLimit(OrderProcessInfo orderProcessInfo, boolean expectedException) throws EmptyBasketException,
            EmptySelectionException {
        UUID processBusinessId = UUID.randomUUID();
        setUpProcessingClient(processBusinessId, new OrderProcessInfoMapper(), orderProcessInfo);
        // Create a basket add multiple selections to the basket
        Basket basket = basketService.findOrCreate(USER_EMAIL);
        Long basketId = basket.getId();
        Assert.assertNotNull(basketService.find(USER_EMAIL));
        try {
            basketService.addSelection(basketId, createBasketSelectionRequest(DS2_IP_ID.toString(), ""));
            basket = basketService.load(basketId);

            // Attach a processing to the basket
            basketService.attachProcessing(basket,
                    basket.getDatasetSelections().stream().findFirst().get().getId(),
                    new ProcessDatasetDescription(processBusinessId, null));
        } catch (TooManyItemsSelectedInBasketException e) {
            LOGGER.error(e.getMessage(), e);
            Assert.fail("No error is expected at this point.");
        }

        // Add a selection to the basket and check an exception is thrown in case there is a limit defined by
        // the process (if the number of items to add is higher than the limit)
        try {
            basketService.addSelection(basketId, createBasketSelectionRequest(null, SearchClientMock.QUERY_DS2_DS3));
            if (expectedException) {
                Assert.fail(String.format("Expected %s exception to occur",
                        TooManyItemsSelectedInBasketException.class.getName()));
            } else {
                basket = basketService.load(basketId);
                // assert the selection was correctly added
                Assert.assertEquals("The selection was not correctly added",  2,  basket.getDatasetSelections().size());
            }
         } catch(TooManyItemsSelectedInBasketException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
