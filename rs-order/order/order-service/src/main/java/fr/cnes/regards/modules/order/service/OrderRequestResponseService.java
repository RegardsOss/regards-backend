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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.order.amqp.output.OrderRequestResponseDtoEvent;
import fr.cnes.regards.modules.order.dao.IOrderRepository;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.dto.output.OrderRequestStatus;
import org.springframework.stereotype.Service;

/**
 * Service to send amqp notification when an order or a sub-order is done
 *
 * @author Thomas GUILLOU
 **/
@Service
public class OrderRequestResponseService {

    private final IPublisher publisher;

    private final IOrderRepository orderRepository;

    public OrderRequestResponseService(IPublisher publisher, IOrderRepository orderRepository) {
        this.publisher = publisher;
        this.orderRepository = orderRepository;
    }

    /**
     * Compute status of order and send a DONE or FAILED amqp notification.
     */
    public void notifyOrderFinished(Order order) {
        OrderRequestStatus responseStatus = order.getStatus() == OrderStatus.DONE ?
            OrderRequestStatus.DONE :
            OrderRequestStatus.FAILED;
        OrderRequestResponseDtoEvent orderRequestResponseDtoEvent = new OrderRequestResponseDtoEvent(order.getCorrelationId(),
                                                                                                     responseStatus,
                                                                                                     "Order of user "
                                                                                                     + order.getOwner()
                                                                                                     + " is finished");
        publisher.publish(orderRequestResponseDtoEvent);
    }

    public void notifySuborderDone(FilesTask filesTask) {
        Order order = orderRepository.getById(filesTask.getOrderId());
        notifySuborderDone(order.getCorrelationId(), order.getOwner());
    }

    /**
     * Send a SUBORDER_DONE amqp notification.
     */
    public void notifySuborderDone(String correlationId, String owner) {
        OrderRequestResponseDtoEvent orderRequestResponseDtoEvent = new OrderRequestResponseDtoEvent(correlationId,
                                                                                                     OrderRequestStatus.SUBORDER_DONE,
                                                                                                     "A sub-order of user "
                                                                                                     + owner
                                                                                                     + " is finished and ready to download");
        publisher.publish(orderRequestResponseDtoEvent);
    }
}
