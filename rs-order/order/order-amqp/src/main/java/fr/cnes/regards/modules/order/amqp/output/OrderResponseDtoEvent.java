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
package fr.cnes.regards.modules.order.amqp.output;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.modules.order.dto.OrderErrorType;
import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;
import fr.cnes.regards.modules.order.dto.output.OrderRequestStatus;
import fr.cnes.regards.modules.order.dto.output.OrderResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.validation.Errors;

import java.util.Optional;

/**
 * AMQP message of order or sub-order response for the AMQP message of order request (See
 * {@link fr.cnes.regards.modules.order.dto.output.OrderResponseDto})
 *
 * @author Iliana Ghazali
 **/
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class OrderResponseDtoEvent extends OrderResponseDto implements ISubscribable {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderResponseDtoEvent.class);

    public OrderResponseDtoEvent(OrderRequestStatus status,
                                 @Nullable Long orderId,
                                 String correlationId,
                                 @Nullable String message,
                                 @Nullable String downloadLink,
                                 @Nullable OrderErrorType errorType,
                                 @Nullable Integer errors,
                                 @Nullable Integer totalSubOrders,
                                 @Nullable Long subOrderId) {
        super(status, orderId, correlationId, message, downloadLink, errorType, errors, totalSubOrders, subOrderId);
    }

    public OrderResponseDtoEvent(OrderResponseDto orderResponse) {
        super(orderResponse.getStatus(),
              orderResponse.getOrderId(),
              orderResponse.getCorrelationId(),
              orderResponse.getMessage(),
              orderResponse.getDownloadLink(),
              orderResponse.getErrorType(),
              orderResponse.getErrors(),
              orderResponse.getTotalSubOrders(),
              orderResponse.getSubOrderId());
    }

    public static OrderResponseDtoEvent buildDeniedResponse(OrderRequestDto orderRequest,
                                                            Errors errors,
                                                            OrderErrorType errorType) {
        String errorsFormatted = ErrorTranslator.getErrorsAsString(errors);
        LOGGER.error("""
                         Errors were detected while validating OrderRequestDtoEvent with correlation id [{}].
                         The request is therefore DENIED and will not be processed.
                         Refer to the OrderResponseDtoEvent response for more information.
                         List of errors detected:
                         {}""", orderRequest.getCorrelationId(), errorsFormatted);

        return new OrderResponseDtoEvent(OrderRequestStatus.DENIED,
                                         null,
                                         orderRequest.getCorrelationId(),
                                         errorsFormatted,
                                         null,
                                         errorType,
                                         errors.getAllErrors().size(),
                                         null,
                                         null);
    }

    @Override
    public Optional<String> getMessageCorrelationId() {
        return Optional.ofNullable(this.getCorrelationId());
    }
}
