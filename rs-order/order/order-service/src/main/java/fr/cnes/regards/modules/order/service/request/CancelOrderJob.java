/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.service.request;

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderStatus;
import fr.cnes.regards.modules.order.domain.dto.OrderStatusDto;
import fr.cnes.regards.modules.order.service.IOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This job is used to cancel a list of orders {@link Order}.
 *
 * @author Stephane Cortine
 */
public class CancelOrderJob extends AbstractJob<Void> {

    public static final String ORDERS_TO_CANCEL = "ORDERS_TO_CANCEL";

    private static final int NB_LOOP = 20;

    @Autowired
    private IAuthenticationResolver authentificationResolver;

    @Autowired
    private IOrderService orderService;

    @Value("${regards.order.cancel.loop.duration:5000}")
    private long loopDuration;

    private List<OrderStatusDto> orders;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        this.orders = getValue(parameters, ORDERS_TO_CANCEL, new TypeToken<List<OrderStatusDto>>() {

        }.getType());
    }

    @Override
    public void run() {
        logger.debug("[{}] Cancel order job starts. Handle {} orders.", jobInfoId, orders.size());
        long start = System.currentTimeMillis();

        List<Long> ordersInWaitingPause = new ArrayList<>();
        List<Long> ordersToDeleted = new ArrayList<>();

        if (orders != null) {
            //1. pause each order if its state is PENDING or RUNNING
            orders.forEach(order -> {
                try {
                    if (order.getStatus().isOneOfStatuses(OrderStatus.PENDING, OrderStatus.RUNNING)) {
                        orderService.pause(order.getId(), false);
                        ordersInWaitingPause.add(order.getId());
                    } else {
                        ordersToDeleted.add(order.getId());
                    }
                } catch (ModuleException e) {
                    manageOrderInError(order.getId(),
                                       String.format("Exception raised while pausing an order [%s] : " + "%s",
                                                     order.getId(),
                                                     e.getMessage()));
                }
            });
            //2. delete order with right state
            ordersToDeleted.forEach(orderId -> {
                try {
                    orderService.delete(orderId, false);
                    this.advanceCompletion();
                } catch (ModuleException e) {
                    manageOrderInError(orderId, e.getMessage());
                }
            });
            //3. ask if each order is in pause state before delete it
            ordersInWaitingPause.forEach(orderId -> {
                try {
                    int loop = 0;
                    while (!orderService.isPaused(orderId) && (loop < NB_LOOP)) {
                        Thread.sleep(loopDuration);
                        loop++;
                    }
                    if (loop == NB_LOOP) {
                        throw new ModuleException("Timeout during canceling of order with identifier ["
                                                  + orderId
                                                  + "]");
                    }
                    orderService.delete(orderId, false);
                    this.advanceCompletion();
                } catch (ModuleException | InterruptedException e) {
                    manageOrderInError(orderId, e.getMessage());
                }
            });
        }

        logger.debug("[{}] Cancel order job ended in {}ms.", jobInfoId, System.currentTimeMillis() - start);
    }

    private void manageOrderInError(Long orderId, String message) {
        logger.error(message);
        orderService.updateErrorWithMessageIfNecessary(orderId, message);
    }

    @Override
    public int getCompletionCount() {
        return orders.size();
    }
}
