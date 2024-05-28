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
package fr.cnes.regards.modules.delivery.amqp.output;

import fr.cnes.regards.framework.amqp.event.*;
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.modules.delivery.amqp.input.DeliveryRequestDtoEvent;
import fr.cnes.regards.modules.delivery.dto.input.DeliveryRequestDto;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryErrorType;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryResponseDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.MessageProperties;

import jakarta.annotation.Nullable;

/**
 * An amqp message for the delivery response event {@link DeliveryResponseDto} after a delivery request event
 * {@link DeliveryRequestDtoEvent}.
 *
 * @author Iliana Ghazali
 **/
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class DeliveryResponseDtoEvent extends DeliveryResponseDto implements ISubscribable, IMessagePropertiesAware {

    /**
     * Properties of event
     * Prevent GSON converter from serializing this field
     */
    @GsonIgnore
    protected MessageProperties messageProperties;

    public DeliveryResponseDtoEvent(String correlationId,
                                    DeliveryRequestStatus status,
                                    @Nullable DeliveryErrorType errorType,
                                    @Nullable String message,
                                    @Nullable String url,
                                    @Nullable String md5,
                                    String originRequestAppId,
                                    Integer originRequestPriority) {
        super(correlationId, status, errorType, message, url, md5, originRequestAppId, originRequestPriority);
        this.messageProperties = new MessageProperties();

        if (!StringUtils.isBlank(originRequestAppId)) {
            this.messageProperties.setAppId(originRequestAppId);
        }
        if (originRequestPriority != null) {
            this.messageProperties.setPriority(originRequestPriority);
        }
    }

    @Override
    public void setMessageProperties(MessageProperties messageProperties) {
        this.messageProperties = messageProperties;
    }

    @Override
    public MessageProperties getMessageProperties() {
        if (messageProperties == null) {
            messageProperties = new MessageProperties();
        }
        return messageProperties;
    }

    public static DeliveryResponseDtoEvent buildGrantedDeliveryResponseEvent(DeliveryRequestDto deliveryRequest,
                                                                             String originRequestAppId,
                                                                             Integer originRequestPriority) {
        return new DeliveryResponseDtoEvent(deliveryRequest.getCorrelationId(),
                                            DeliveryRequestStatus.GRANTED,
                                            null,
                                            null,
                                            null,
                                            null,
                                            originRequestAppId,
                                            originRequestPriority);
    }

    public static DeliveryResponseDtoEvent buildDeniedDeliveryResponseEvent(DeliveryRequestDto deliveryRequest,
                                                                            String originRequestAppId,
                                                                            Integer originRequestPriority,
                                                                            DeliveryErrorType deliveryErrorType,
                                                                            String errorsFormatted) {
        return new DeliveryResponseDtoEvent(deliveryRequest.getCorrelationId(),
                                            DeliveryRequestStatus.DENIED,
                                            deliveryErrorType,
                                            errorsFormatted,
                                            null,
                                            null,
                                            originRequestAppId,
                                            originRequestPriority);
    }

}
