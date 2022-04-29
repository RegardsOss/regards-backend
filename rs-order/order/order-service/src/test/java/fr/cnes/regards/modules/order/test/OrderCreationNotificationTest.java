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
package fr.cnes.regards.modules.order.test;

import feign.Request;
import feign.RetryableException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.service.IOrderJobService;
import fr.cnes.regards.modules.order.service.OrderCreationService;
import fr.cnes.regards.modules.order.service.OrderHelperService;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.templates.service.TemplateService;
import freemarker.template.TemplateException;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;

import java.time.OffsetDateTime;
import java.util.HashMap;

import static fr.cnes.regards.modules.order.test.MailClientMocks.aMailClient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OrderCreationNotificationTest {

    private Order givenOrder;

    private IEmailClient mailClient;

    private IOrderJobService jobOrderService;

    private OrderCreationService orderCreation;

    private Order givenOrder(OrderStatus orderStatus) {
        Order givenOrder = new Order();
        givenOrder.setId(1L);
        givenOrder.setOwner("USER");
        DatasetTask datasetTask = new DatasetTask();
        datasetTask.setDatasetLabel("DATASET");
        givenOrder.getDatasetTasks().add(datasetTask);
        givenOrder.setStatus(orderStatus);
        return givenOrder;
    }

    private OrderCreationService givenOrderCreationServiceUnderTest() {
        return new OrderCreationService(mockOrderRepo(),
                                        null,
                                        mockJobOrderService(),
                                        null,
                                        null,
                                        null,
                                        mailClient,
                                        mockOrderHelperService(),
                                        mockProjectClient(),
                                        mockSpringPublisher(),
                                        mockCurrentTenant(),
                                        null,
                                        mockTemplateService());
    }

    private IOrderRepository mockOrderRepo() {
        IOrderRepository orderRepo = mock(IOrderRepository.class);
        when(orderRepo.findCompleteById(any())).thenReturn(givenOrder);
        when(orderRepo.save(any())).thenReturn(givenOrder);
        return orderRepo;
    }

    private IOrderJobService mockJobOrderService() {
        jobOrderService = mock(IOrderJobService.class);
        when(jobOrderService.computePriority(any(), any())).thenReturn(1);
        return jobOrderService;
    }

    private OrderHelperService mockOrderHelperService() {
        OrderHelperService orderHelper = mock(OrderHelperService.class);
        when(orderHelper.computeOrderExpirationDate(any(), anyInt(), anyInt())).thenReturn(OffsetDateTime.now());
        return orderHelper;
    }

    private IProjectsClient mockProjectClient() {
        Project project = new Project();
        ResponseEntity<EntityModel<Project>> projectResponse = ResponseEntity.ok()
                                                                             .body(new EntityModel<Project>(project));
        IProjectsClient projectClient = mock(IProjectsClient.class);
        when(projectClient.retrieveProject(any())).thenReturn(projectResponse);
        return projectClient;
    }

    private ApplicationEventPublisher mockSpringPublisher() {
        return mock(ApplicationEventPublisher.class);
    }

    private IRuntimeTenantResolver mockCurrentTenant() {
        IRuntimeTenantResolver currentTenant = mock(IRuntimeTenantResolver.class);
        when(currentTenant.getTenant()).thenReturn("TENANT");
        return currentTenant;
    }

    private TemplateService mockTemplateService() {
        try {
            TemplateService templateService = mock(TemplateService.class);
            when(templateService.render(any(), any())).thenReturn("TEMPLATED");
            return templateService;
        } catch (TemplateException e) {
            throw new RuntimeException("Fail to initialize template service mock", e);
        }
    }

    @Test
    public void send_mail_if_order_is_successfully_created() throws Exception {
        givenOrder = givenOrder(OrderStatus.PENDING);
        mailClient = aMailClient().nominal();

        orderCreation = givenOrderCreationServiceUnderTest();
        orderCreation.completeOrderCreation(new Basket(), null, "ROLE", 0, null);

        // Mail is sent and jobs are queued
        verify(mailClient).sendEmail(any(), any(), any(), any());
        verify(jobOrderService).manageUserOrderStorageFilesJobInfos("USER");
    }

    @Test
    public void dont_send_mail_if_order_failed() throws Exception {
        givenOrder = givenOrder(OrderStatus.FAILED);
        mailClient = aMailClient().nominal();

        orderCreation = givenOrderCreationServiceUnderTest();
        orderCreation.completeOrderCreation(new Basket(), null, "ROLE", 0, null);

        verify(mailClient, times(0)).sendEmail(any(), any(), any(), any());
    }

    @Test
    public void a_mail_http_failure_does_not_stop_order_creation_process() throws Exception {
        givenOrder = givenOrder(OrderStatus.PENDING);
        mailClient = aMailClient().sendMailThrows(httpException());

        orderCreation = givenOrderCreationServiceUnderTest();
        orderCreation.completeOrderCreation(new Basket(), null, "ROLE", 0, null);

        // The mail exception is thrown
        // but it doesn't stop order creation process
        // => Jobs are queued (manageUserOrderStorageFilesJobInfos)
        verify(mailClient).sendEmail(any(), any(), any(), any());
        verify(jobOrderService).manageUserOrderStorageFilesJobInfos("USER");
    }

    @Test
    public void a_mail_timeout_failure_does_not_stop_order_creation_process() throws Exception {
        givenOrder = givenOrder(OrderStatus.PENDING);
        mailClient = aMailClient().sendMailThrows(mailTimeoutException());

        orderCreation = givenOrderCreationServiceUnderTest();

        // Mail timeout exception is raised by mail notification
        // It is not handled by order creation process
        // The exception isn't catched
        // and the order creation stops
        // The jobs aren't queued
        // => Bug CNES (ref 635)
        Basket basket = new Basket();
        Assertions.assertThatExceptionOfType(mailTimeoutException().getClass())
                  .isThrownBy(() -> orderCreation.completeOrderCreation(basket, null, "ROLE", 0, null));
    }

    @Test
    public void order_creation_fails_if_an_unexpected_mail_failure_occurred() throws Exception {
        givenOrder = givenOrder(OrderStatus.PENDING);
        mailClient = aMailClient().sendMailThrows(unexpectedException());

        orderCreation = givenOrderCreationServiceUnderTest();

        // Unexpected exception is raised by mail notification
        // It is not handled by order creation process
        // The exception isn't catched
        // and the order creation stops
        // The jobs aren't queued
        // => Bug CNES (ref 635)
        Basket basket = new Basket();
        Assertions.assertThatExceptionOfType(RuntimeException.class)
                  .isThrownBy(() -> orderCreation.completeOrderCreation(basket, null, "ROLE", 0, null));
    }

    private HttpServerErrorException httpException() {
        return new HttpServerErrorException(HttpStatus.ACCEPTED);
    }

    private RetryableException mailTimeoutException() {
        Request request = Request.create(Request.HttpMethod.GET, "test", new HashMap<>(), Request.Body.empty());
        return new RetryableException(1, "", Request.HttpMethod.POST, null, request);
    }

    private RuntimeException unexpectedException() {
        return new RuntimeException("mail unexpected failure");
    }

}

class MailClientMocks {

    private final IEmailClient mailClient;

    private MailClientMocks() {
        mailClient = mock(IEmailClient.class);
    }

    public static MailClientMocks aMailClient() {
        return new MailClientMocks();
    }

    public IEmailClient nominal() {
        return mailClient;
    }

    public IEmailClient sendMailThrows(Exception e) {
        when(mailClient.sendEmail(any(), any(), any(), any())).thenThrow(e);
        return mailClient;
    }
}
