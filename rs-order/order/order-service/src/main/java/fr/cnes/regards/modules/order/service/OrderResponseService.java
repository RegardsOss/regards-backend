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
package fr.cnes.regards.modules.order.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.ResponseEntityUtils;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.order.amqp.output.OrderResponseDtoEvent;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.dto.output.OrderRequestStatus;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;

/**
 * Service to send amqp notification when an order or a sub-order done
 *
 * @author Thomas GUILLOU
 **/
@Service
public class OrderResponseService {

    // FIXME see how to get that path without duplicating
    private static final String ZIP_DOWNLOAD_PATH = "user/orders/{orderId}/download";

    private final IPublisher publisher;

    private final IOrderRepository orderRepository;

    private final IProjectsClient projectClient;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    @Value("${prefix.path}")
    private String prefixPath;

    @Value("${spring.application.name}")
    private String applicationName;

    private final Cache<String, String> projectHostCache = Caffeine.newBuilder()
                                                                   .expireAfterWrite(Duration.ofMinutes(5))
                                                                   .build();

    public OrderResponseService(IPublisher publisher,
                                IOrderRepository orderRepository,
                                IProjectsClient projectClient,
                                IRuntimeTenantResolver runtimeTenantResolver) {
        this.publisher = publisher;
        this.orderRepository = orderRepository;
        this.projectClient = projectClient;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    /**
     * Compute status of order and send a DONE or FAILED amqp notification.
     */
    public void notifyOrderFinished(Order order) {
        OrderRequestStatus responseStatus = order.getStatus() == OrderStatus.DONE ?
            OrderRequestStatus.DONE :
            OrderRequestStatus.FAILED;
        String message;
        String downloadLink = null;
        switch (order.getStatus()) {
            case FAILED -> {
                message = String.format("Order of user %s is finished with errors. One or many files could not be "
                                        + "retrieved.", order.getOwner());
            }
            case DONE -> {
                downloadLink = computeDownloadLink(order.getId());
                message = String.format("Order of user %s is finished", order.getOwner());
            }
            case DONE_WITH_WARNING -> {
                downloadLink = computeDownloadLink(order.getId());
                message = String.format("Order of user %s is finished with some warnings.", order.getOwner());
            }
            default -> {
                throw new RsRuntimeException("Invalid status for order finished to notify");
            }
        }
        publisher.publish(new OrderResponseDtoEvent(responseStatus,
                                                    order.getId(),
                                                    order.getCorrelationId(),
                                                    message,
                                                    downloadLink));
    }

    public void notifySuborderDone(FilesTask filesTask) {
        Long orderId = filesTask.getOrderId();
        Order order = orderRepository.getById(orderId);
        notifySuborderDone(order.getCorrelationId(), order.getOwner(), orderId);
    }

    /**
     * Send a SUBORDER_DONE amqp notification.
     */
    public void notifySuborderDone(String correlationId, String owner, Long orderId) {
        OrderResponseDtoEvent orderResponseDtoEvent = new OrderResponseDtoEvent(OrderRequestStatus.SUBORDER_DONE,
                                                                                orderId,
                                                                                correlationId,
                                                                                "A sub-order of user "
                                                                                + owner
                                                                                + " is finished and ready to download",
                                                                                computeDownloadLink(orderId));
        publisher.publish(orderResponseDtoEvent);
    }

    public String computeDownloadLink(Long orderId) {
        return getProjectHost()
               + prefixPath
               + File.separator
               + applicationName
               + File.separator
               + ZIP_DOWNLOAD_PATH.replace("{orderId}", orderId.toString());
    }

    /**
     * Lazy loading of project host
     */
    public String getProjectHost() {
        return projectHostCache.get(runtimeTenantResolver.getTenant(), t -> {
            FeignSecurityManager.asSystem();
            Project project = ResponseEntityUtils.extractContentOrThrow(projectClient.retrieveProject(
                                                                            runtimeTenantResolver.getTenant()),
                                                                        () -> new RsRuntimeException(
                                                                            "An error occurred while retrieving project"));
            String host = project.getHost();
            FeignSecurityManager.reset();
            return host;
        });
    }
}
