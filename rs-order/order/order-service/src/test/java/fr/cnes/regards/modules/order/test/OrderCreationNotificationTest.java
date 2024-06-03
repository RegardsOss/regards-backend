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
import feign.RequestTemplate;
import feign.RetryableException;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.dto.dto.OrderStatus;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.order.service.IOrderJobService;
import fr.cnes.regards.modules.order.service.OrderCreationService;
import fr.cnes.regards.modules.order.service.OrderHelperService;
import fr.cnes.regards.modules.order.service.OrderResponseService;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.templates.service.TemplateService;
import freemarker.template.TemplateException;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;

import static fr.cnes.regards.modules.order.service.OrderService.DEFAULT_CORRELATION_ID_FORMAT;
import static fr.cnes.regards.modules.order.test.FakeExceptions.*;
import static fr.cnes.regards.modules.order.test.MailClientMocks.aMailClient;
import static fr.cnes.regards.modules.order.test.TemplateServiceMocks.aTemplateService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OrderCreationNotificationTest {

    private Order givenOrder;

    private IEmailClient emailClient;

    private IOrderJobService jobOrderService;

    private OrderCreationService orderCreation;

    private TemplateService templateService;

    public OrderCreationNotificationTest() {
        emailClient = aMailClient().nominal();
        templateService = aTemplateService().nominal();
    }

    private Order givenOrder(OrderStatus orderStatus) {
        Order givenOrder = new Order();
        givenOrder.setId(1L);
        givenOrder.setOwner("USER");
        givenOrder.setCreationDate(OffsetDateTime.now());
        givenOrder.setCorrelationId(String.format(DEFAULT_CORRELATION_ID_FORMAT,
                                                  OffsetDateTimeAdapter.format(givenOrder.getCreationDate())));
        DatasetTask datasetTask = new DatasetTask();
        datasetTask.setDatasetLabel("DATASET");
        givenOrder.getDatasetTasks().add(datasetTask);
        givenOrder.setStatus(orderStatus);
        givenOrder.setFrontendUrl("https://fakeUrl.fr");
        return givenOrder;
    }

    private OrderCreationService givenOrderCreationServiceUnderTest() {
        return new OrderCreationService(mockOrderRepo(),
                                        null,
                                        mockJobOrderService(),
                                        null,
                                        null,
                                        null,
                                        emailClient,
                                        mockOrderHelperService(),
                                        mockProjectClient(),
                                        mockSpringPublisher(),
                                        mockCurrentTenant(),
                                        null,
                                        templateService,
                                        mockOrderRequestResponseService(),
                                        null,
                                        null,
                                        null,
                                        null);
    }

    private OrderResponseService mockOrderRequestResponseService() {
        return Mockito.mock(OrderResponseService.class);
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
        when(orderHelper.computeOrderExpirationDate(anyInt(), anyInt())).thenReturn(OffsetDateTime.now());
        return orderHelper;
    }

    private IProjectsClient mockProjectClient() {
        Project project = new Project();
        ResponseEntity<EntityModel<Project>> projectResponse = ResponseEntity.ok().body(EntityModel.of(project));
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

    @Test
    public void send_mail_if_order_is_successfully_created() throws Exception {
        givenOrder = givenOrder(OrderStatus.PENDING);

        orderCreation = givenOrderCreationServiceUnderTest();
        orderCreation.completeOrderCreation(new Basket(), null, "ROLE", 0, null);

        // Mail is sent and jobs are queued
        verify(emailClient).sendEmail(any(), any(), any(), any());
        verify(jobOrderService).manageUserOrderStorageFilesJobInfos("USER");
    }

    @Test
    public void dont_send_mail_if_order_failed() throws Exception {
        givenOrder = givenOrder(OrderStatus.FAILED);

        orderCreation = givenOrderCreationServiceUnderTest();
        orderCreation.completeOrderCreation(new Basket(), null, "ROLE", 0, null);

        verify(emailClient, times(0)).sendEmail(any(), any(), any(), any());
    }

    @Test
    public void a_mail_http_failure_does_not_stop_order_creation_process() throws Exception {
        givenOrder = givenOrder(OrderStatus.PENDING);
        emailClient = aMailClient().sendMailRaises(aHttpException());

        orderCreation = givenOrderCreationServiceUnderTest();
        orderCreation.completeOrderCreation(new Basket(), null, "ROLE", 0, null);

        // The mail exception is raised but handled
        // but it doesn't stop order creation process
        // => Jobs are queued (manageUserOrderStorageFilesJobInfos)
        verify(emailClient).sendEmail(any(), any(), any(), any());
        verify(jobOrderService).manageUserOrderStorageFilesJobInfos("USER");
    }

    @Test
    public void a_mail_timeout_failure_does_not_stop_order_creation_process() throws Exception {
        givenOrder = givenOrder(OrderStatus.PENDING);
        emailClient = aMailClient().sendMailRaises(aMailTimeoutException());

        orderCreation = givenOrderCreationServiceUnderTest();
        orderCreation.completeOrderCreation(new Basket(), null, "ROLE", 0, null);

        // The mail exception is raised but handled
        // but it doesn't stop order creation process
        // => Jobs are queued (manageUserOrderStorageFilesJobInfos)
        verify(emailClient).sendEmail(any(), any(), any(), any());
        verify(jobOrderService).manageUserOrderStorageFilesJobInfos("USER");
    }

    @Test
    public void an_unexpected_mail_failure_does_not_stop_order_creation() throws Exception {
        givenOrder = givenOrder(OrderStatus.PENDING);
        emailClient = aMailClient().sendMailRaises(anUnexpectedException());

        orderCreation = givenOrderCreationServiceUnderTest();
        orderCreation.completeOrderCreation(new Basket(), null, "ROLE", 0, null);

        verify(emailClient).sendEmail(any(), any(), any(), any());
        verify(jobOrderService).manageUserOrderStorageFilesJobInfos("USER");
    }

    @Test
    public void a_mail_template_failure_does_not_stop_order_creation_process() throws Exception {
        givenOrder = givenOrder(OrderStatus.PENDING);
        templateService = aTemplateService().renderRaises(aTemplateException());

        orderCreation = givenOrderCreationServiceUnderTest();
        orderCreation.completeOrderCreation(new Basket(), null, "ROLE", 0, null);

        verify(templateService).render(any(), any());
        verify(jobOrderService).manageUserOrderStorageFilesJobInfos("USER");
    }

    @Test
    public void an_io_error_on_mail_templating_does_not_stop_order_creation_process() throws Exception {
        givenOrder = givenOrder(OrderStatus.PENDING);
        templateService = aTemplateService().renderRaises(anExceptionWhenIOErrorOnTemplating());

        orderCreation = givenOrderCreationServiceUnderTest();
        orderCreation.completeOrderCreation(new Basket(), null, "ROLE", 0, null);

        verify(templateService).render(any(), any());
        verify(jobOrderService).manageUserOrderStorageFilesJobInfos("USER");
    }

    @Test
    public void an_unexpected_error_on_templating_does_not_stop_order_creation_process() throws Exception {
        givenOrder = givenOrder(OrderStatus.PENDING);
        templateService = aTemplateService().renderRaises(anUnexpectedException());

        orderCreation = givenOrderCreationServiceUnderTest();
        orderCreation.completeOrderCreation(new Basket(), null, "ROLE", 0, null);

        verify(templateService).render(any(), any());
        verify(jobOrderService).manageUserOrderStorageFilesJobInfos("USER");
    }
}

class FakeExceptions {

    public static HttpServerErrorException aHttpException() {
        return new HttpServerErrorException(HttpStatus.ACCEPTED);
    }

    public static RetryableException aMailTimeoutException() {
        Request request = Request.create(Request.HttpMethod.GET,
                                         "test",
                                         new HashMap<>(),
                                         Request.Body.empty(),
                                         new RequestTemplate());
        return new RetryableException(1, "", Request.HttpMethod.POST, null, request);
    }

    public static TemplateException aTemplateException() {
        return mock(TemplateException.class);
    }

    public static RsRuntimeException anExceptionWhenIOErrorOnTemplating() {
        return new RsRuntimeException(new IOException());
    }

    public static RuntimeException anUnexpectedException() {
        return new RuntimeException("mail unexpected failure");
    }
}

class TemplateServiceMocks {

    private final TemplateService templateService;

    private TemplateServiceMocks() {
        templateService = mock(TemplateService.class);
    }

    public static TemplateServiceMocks aTemplateService() {
        return new TemplateServiceMocks();
    }

    public TemplateService nominal() {
        try {
            when(templateService.render(any(), any())).thenReturn("TEMPLATED");
            return templateService;
        } catch (TemplateException e) {
            throw new RuntimeException("Fail to mock template service.");
        }
    }

    public TemplateService renderRaises(Exception e) {
        try {
            when(templateService.render(any(), any())).thenThrow(e);
            return templateService;
        } catch (TemplateException e1) {
            throw new RuntimeException("Fail to mock template service.");
        }
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

    public IEmailClient sendMailRaises(Exception e) {
        when(mailClient.sendEmail(any(), any(), any(), any())).thenThrow(e);
        return mailClient;
    }
}

