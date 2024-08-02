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
package fr.cnes.regards.modules.order.rest;

import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.order.amqp.output.OrderResponseDtoEvent;
import fr.cnes.regards.modules.order.dto.input.DataTypeLight;
import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;
import fr.cnes.regards.modules.order.dto.input.OrderRequestFilters;
import fr.cnes.regards.modules.order.dto.output.OrderRequestStatus;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Set;

/**
 * Test to verify automatic order creations and executions.
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "regards.tenant=order1",
                                   "spring.jpa.properties.hibernate.default_schema=auto_order_it" })
public class AutoOrderControllerIT extends AbstractOrderControllerIT {

    @Test
    @Purpose("Tests the successful creation of an order by bypassing user interactions.")
    public void testNominalOrderAutoCreation() throws Exception {
        // GIVEN
        // Before: clear basket for next order and mock search client results
        initForNextOrder();
        // Note: this is not a complete version of a DocFilesSummary as it is not required
        Mockito.when(searchClient.computeDatasetsSummary(Mockito.any())).thenAnswer(invocationOnMock -> {
            DocFilesSummary summary = new DocFilesSummary();
            summary.addFilesCount(2);
            return ResponseEntity.ok(summary);
        });
        // Create order request
        OrderRequestDto orderRequestDto = new OrderRequestDto(List.of("q:\"\""),
                                                              new OrderRequestFilters(Set.of(DataTypeLight.RAWDATA),
                                                                                      null),
                                                              null,
                                                              null,
                                                              null);
        // WHEN
        RequestBuilderCustomizer customizer = customizer().expectStatusOk();
        ResultActions actualResponse = performDefaultPost(OrderController.AUTO_ORDER_PATH,
                                                          orderRequestDto,
                                                          customizer,
                                                          "order was not created from orderRequestDto!");

        // THEN
        // expect amqp response event
        OrderResponseDtoEvent expectedResponseEvent = new OrderResponseDtoEvent(OrderRequestStatus.GRANTED,
                                                                                orderRepository.findAll(PageRequest.of(0,
                                                                                                                       1,
                                                                                                                       Sort.by(
                                                                                                                           "id")))
                                                                                               .getContent()
                                                                                               .get(0)
                                                                                               .getId(),
                                                                                "something-random",
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                null,
                                                                                null);

        // expect rest response
        actualResponse.andExpect(MockMvcResultMatchers.jsonPath("$.content").exists())
                      .andExpect(MockMvcResultMatchers.jsonPath("$.content.status",
                                                                Matchers.equalTo(expectedResponseEvent.getStatus()
                                                                                                      .toString())))
                      .andExpect(MockMvcResultMatchers.jsonPath("$.content.orderId",
                                                                Matchers.comparesEqualTo(expectedResponseEvent.getOrderId()),
                                                                Long.class))
                      .andExpect(MockMvcResultMatchers.jsonPath("$.content.message").doesNotExist());
    }

    @Test
    @Purpose("Tests that an order is not automatically created when an error occurred during the process.")
    public void testErrorOrderAutoCreation() throws Exception {
        // GIVEN
        // Create order request
        OrderRequestDto orderRequestDto = new OrderRequestDto(List.of("q:\"\""),
                                                              new OrderRequestFilters(Set.of(DataTypeLight.QUICKLOOK),
                                                                                      null),
                                                              null,
                                                              null,
                                                              null);
        // mock result of search with 0 files returned
        Mockito.when(searchClient.computeDatasetsSummary(Mockito.any())).thenAnswer(invocationOnMock -> {
            DocFilesSummary summary = new DocFilesSummary();
            summary.addFilesCount(0);
            return ResponseEntity.ok(summary);
        });

        // WHEN
        // expected status is 400 because searchClient.computeDatasetsSummary contains 0 files. This will result in a
        // EmptySelectionException.
        RequestBuilderCustomizer customizer = customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        ResultActions actualResponse = performDefaultPost(OrderController.AUTO_ORDER_PATH,
                                                          orderRequestDto,
                                                          customizer,
                                                          "order was not created from orderRequestDto!");

        // THEN
        // empty basket should be created
        Assert.assertNotNull(basketRepository.findByOwner(getDefaultUserEmail()));

        // expect rest response
        actualResponse.andExpect(MockMvcResultMatchers.jsonPath("$.content").exists())
                      .andExpect(MockMvcResultMatchers.jsonPath("$.content.status",
                                                                Matchers.equalTo(OrderRequestStatus.FAILED.toString())))
                      .andExpect(MockMvcResultMatchers.jsonPath("$.content.createdOrderId").doesNotExist())
                      .andExpect(MockMvcResultMatchers.jsonPath("$.content.message").exists());
    }

    @Test
    @Purpose("Tests that an order is not automatically created when an error occurred during the process.")
    public void testInvalidRequestOrderAutoCreation() throws Exception {
        // GIVEN
        // Create order request
        OrderRequestDto orderRequestDto = new OrderRequestDto(List.of("q:\"\""),
                                                              new OrderRequestFilters(Set.of(DataTypeLight.QUICKLOOK),
                                                                                      null),
                                                              null,
                                                              null,
                                                              null);
        // mock result of search with 0 files returned
        Mockito.when(searchClient.computeDatasetsSummary(Mockito.any()))
               .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // WHEN
        // expected status is 400 because searchClient.computeDatasetsSummary contains 0 files. This will result in a
        // EmptySelectionException.
        RequestBuilderCustomizer customizer = customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        ResultActions actualResponse = performDefaultPost(OrderController.AUTO_ORDER_PATH,
                                                          orderRequestDto,
                                                          customizer,
                                                          "order was not created from orderRequestDto!");

        // THEN
        // empty basket should be created
        Assert.assertNotNull(basketRepository.findByOwner(getDefaultUserEmail()));

        // expect rest response
        actualResponse.andExpect(MockMvcResultMatchers.jsonPath("$.content").exists())
                      .andExpect(MockMvcResultMatchers.jsonPath("$.content.status",
                                                                Matchers.equalTo(OrderRequestStatus.FAILED.toString())))
                      .andExpect(MockMvcResultMatchers.jsonPath("$.content.createdOrderId").doesNotExist())
                      .andExpect(MockMvcResultMatchers.jsonPath("$.content.message").exists());
    }

}
