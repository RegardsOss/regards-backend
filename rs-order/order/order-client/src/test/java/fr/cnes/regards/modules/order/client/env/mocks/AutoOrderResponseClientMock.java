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
package fr.cnes.regards.modules.order.client.env.mocks;

import fr.cnes.regards.modules.order.amqp.output.OrderResponseDtoEvent;
import fr.cnes.regards.modules.order.client.amqp.IAutoOrderResponseClient;
import fr.cnes.regards.modules.order.dto.output.OrderRequestStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.cnes.regards.modules.order.dto.output.OrderRequestStatus.*;

/**
 * @author Iliana Ghazali
 **/
public class AutoOrderResponseClientMock implements IAutoOrderResponseClient {


    private final Map<OrderRequestStatus, List<OrderResponseDtoEvent>> responsesByStatus;

    public AutoOrderResponseClientMock() {
        this.responsesByStatus = new HashMap<>();
    }

    @Override
    public void onOrderDenied(List<OrderResponseDtoEvent> stored) {
        addEventsToMap(DENIED, stored);

    }

    @Override
    public void onOrderGranted(List<OrderResponseDtoEvent> groupedEvents) {
        addEventsToMap(GRANTED, groupedEvents);
    }

    @Override
    public void onSubOrderDone(List<OrderResponseDtoEvent> groupedEvents) {
        addEventsToMap(SUBORDER_DONE, groupedEvents);
    }

    @Override
    public void onOrderDone(List<OrderResponseDtoEvent> groupedEvents) {
        addEventsToMap(DONE, groupedEvents);
    }

    @Override
    public void onOrderFailed(List<OrderResponseDtoEvent> groupedEvents) {
        addEventsToMap(FAILED, groupedEvents);
    }


    private void addEventsToMap(OrderRequestStatus status, List<OrderResponseDtoEvent> stored) {
        responsesByStatus.computeIfAbsent(status, v -> new ArrayList<>()).addAll(stored);
    }

    public Map<OrderRequestStatus, Integer> countEventsByStatus() {
        Map<OrderRequestStatus, Integer> countMap = new HashMap<>();
        responsesByStatus.forEach((key, list) -> countMap.put(key, list.size()));
        return countMap;
    }
    public void reset() {
        responsesByStatus.clear();
    }

    public Map<OrderRequestStatus, List<OrderResponseDtoEvent>> getResponsesByStatus() {
        return responsesByStatus;
    }
}
