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
package fr.cnes.regards.modules.delivery.service.submission.create;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.delivery.amqp.input.DeliveryRequestDtoEvent;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.domain.settings.DeliverySettings;
import fr.cnes.regards.modules.delivery.dto.input.DeliveryRequestDto;
import fr.cnes.regards.modules.delivery.service.settings.DeliverySettingService;
import fr.cnes.regards.modules.delivery.service.submission.DeliveryRequestService;
import fr.cnes.regards.modules.order.client.amqp.IAutoOrderRequestClient;
import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service to handle the receiving of {@link DeliveryRequestDtoEvent}s.
 *
 * @author Iliana Ghazali
 **/
@Service
public class DeliveryCreateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeliveryCreateService.class);

    private static final int DB_SLICE_SIZE = 100;

    private final IAutoOrderRequestClient orderClient;

    private final DeliverySettingService settingService;

    private final DeliveryRequestService deliveryRequestService;

    public DeliveryCreateService(IAutoOrderRequestClient orderClient,
                                 DeliverySettingService settingService,
                                 DeliveryRequestService deliveryRequestService) {
        this.orderClient = orderClient;
        this.settingService = settingService;
        this.deliveryRequestService = deliveryRequestService;
    }

    /**
     * Request the creation of orders from {@link DeliveryRequestDtoEvent}s. Then save {@link DeliveryRequest}s for
     * monitoring purposes.
     *
     * @param deliveryEvents containing metadata to create orders.
     * @return number of delivery requests created.
     */
    @MultitenantTransactional
    public int handleDeliveryRequestsCreation(List<? extends DeliveryRequestDto> deliveryEvents) {
        int nbRequestsCreated = 0;
        // split events received into manageable database slices
        List<? extends List<? extends DeliveryRequestDto>> deliveryRequestEventsSlices = Lists.partition(deliveryEvents,
                                                                                                         DB_SLICE_SIZE);
        for (List<? extends DeliveryRequestDto> deliveryEventPart : deliveryRequestEventsSlices) {
            int nbDeliveryEvents = deliveryEventPart.size();
            List<OrderRequestDto> orderRequestDtos = new ArrayList<>(nbDeliveryEvents);
            List<DeliveryRequest> deliveryRequests = new ArrayList<>(nbDeliveryEvents);
            // remove events with correlationIds already registered
            deliveryEventPart = filterUniqueCorrelationIds(deliveryEventPart);
            // if correlationIds are unique, create associated delivery requests and order events
            for (DeliveryRequestDto deliveryEvent : deliveryEventPart) {
                String correlationId = deliveryEvent.getCorrelationId();
                LOGGER.debug("---> Processing DeliveryResponseDtoEvent with correlationId \"{}\"", correlationId);

                String originRequestAppId = deliveryEvent.getOriginRequestAppId().orElse(null);
                Integer originRequestPriority = deliveryEvent.getOriginRequestPriority().orElse(null);

                // force order.correlationId with the same delivery request correlationId
                deliveryEvent.getOrder().setCorrelationId(correlationId);
                orderRequestDtos.add(deliveryEvent.getOrder());

                deliveryRequests.add(DeliveryRequest.buildGrantedDeliveryRequest(deliveryEvent,
                                                                                 settingService.getValue(
                                                                                     DeliverySettings.REQUEST_TTL_HOURS),
                                                                                 originRequestAppId,
                                                                                 originRequestPriority));
            }
            // save delivery requests
            nbRequestsCreated += deliveryRequestService.saveAllRequests(deliveryRequests).size();

            // create orders from events
            orderClient.publishOrderRequestEvents(orderRequestDtos,
                                                  settingService.getValue(DeliverySettings.DELIVERY_ORDER_SIZE_LIMIT_BYTES));
        }
        return nbRequestsCreated;
    }

    /**
     * Get DeliveryRequestDtoEvents with unique correlation ids, i.e., make sure delivery requests were not
     * already registered with the same correlation ids. In such cases, the duplicated events will not be processed.
     *
     * @param deliveryEventPart events received
     * @return events with not unique correlationIds
     */
    private List<? extends DeliveryRequestDto> filterUniqueCorrelationIds(List<? extends DeliveryRequestDto> deliveryEventPart) {
        List<DeliveryRequest> alreadyExistingRequests = deliveryRequestService.findDeliveryRequestByCorrelationIds(
            deliveryEventPart.stream().map(DeliveryRequestDto::getCorrelationId).toList());
        if (!alreadyExistingRequests.isEmpty()) {
            List<String> alreadyExistingCorrelationIds = alreadyExistingRequests.stream()
                                                                                .map(DeliveryRequest::getCorrelationId)
                                                                                .toList();
            LOGGER.warn("Delivery events with correlation ids \"{}\" already exist in the system, thus they "
                        + "will not be processed. You may send back the requests with other unique correlation ids.",
                        alreadyExistingCorrelationIds);

            return deliveryEventPart.stream()
                                    .filter(deliveryEvent -> !alreadyExistingCorrelationIds.contains(deliveryEvent.getCorrelationId()))
                                    .toList();
        }
        return deliveryEventPart;
    }
}
