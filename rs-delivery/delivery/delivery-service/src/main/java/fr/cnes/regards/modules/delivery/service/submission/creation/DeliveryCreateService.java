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
package fr.cnes.regards.modules.delivery.service.submission.creation;

import fr.cnes.regards.modules.delivery.amqp.input.DeliveryRequestDtoEvent;
import fr.cnes.regards.modules.delivery.amqp.output.DeliveryResponseDtoEvent;
import fr.cnes.regards.modules.delivery.dao.IDeliveryRequestRepository;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.domain.settings.DeliverySettings;
import fr.cnes.regards.modules.delivery.dto.input.DeliveryRequestDto;
import fr.cnes.regards.modules.delivery.service.settings.DeliverySettingService;
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

    private final IAutoOrderRequestClient orderClient;

    private final DeliverySettingService settingService;

    private final IDeliveryRequestRepository deliveryRequestRepository;

    public DeliveryCreateService(IAutoOrderRequestClient orderClient,
                                 DeliverySettingService settingService,
                                 IDeliveryRequestRepository deliveryRequestRepository) {
        this.orderClient = orderClient;
        this.settingService = settingService;
        this.deliveryRequestRepository = deliveryRequestRepository;
    }

    /**
     * Request the creation of orders from {@link DeliveryRequestDtoEvent}s. Then save {@link DeliveryRequest}s for
     * monitoring purposes.
     *
     * @param deliveryEvents containing metadata to create orders.
     * @return status responses.
     */
    public List<DeliveryResponseDtoEvent> handleDeliveryRequestsCreation(List<? extends DeliveryRequestDto> deliveryEvents) {
        int nbDeliveryEvents = deliveryEvents.size();
        List<OrderRequestDto> orderRequestDtos = new ArrayList<>(nbDeliveryEvents);
        List<DeliveryRequest> deliveryRequests = new ArrayList<>(nbDeliveryEvents);
        List<DeliveryResponseDtoEvent> responses = new ArrayList<>(nbDeliveryEvents);

        for (DeliveryRequestDto deliveryEvent : deliveryEvents) {
            LOGGER.debug("---> Processing DeliveryResponseDtoEvent with correlationId \"{}\"",
                         deliveryEvent.getCorrelationId());

            String originRequestAppId = deliveryEvent.getOriginRequestAppId().orElse(null);
            Integer originRequestPriority = deliveryEvent.getOriginRequestPriority().orElse(null);

            orderRequestDtos.add(deliveryEvent.getOrder());

            deliveryRequests.add(DeliveryRequest.buildGrantedDeliveryRequest(deliveryEvent,
                                                                             settingService.getValue(DeliverySettings.REQUEST_TTL_HOURS),
                                                                             originRequestAppId,
                                                                             originRequestPriority));
            responses.add(DeliveryResponseDtoEvent.buildGrantedDeliveryResponseEvent(deliveryEvent,
                                                                                     originRequestAppId,
                                                                                     originRequestPriority));
        }
        // create orders from events
        orderClient.createOrderFromRequests(orderRequestDtos);
        // save delivery requests
        deliveryRequestRepository.saveAll(deliveryRequests);

        return responses;
    }
}
