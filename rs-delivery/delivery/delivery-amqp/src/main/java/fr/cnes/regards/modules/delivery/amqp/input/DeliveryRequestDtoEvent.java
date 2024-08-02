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
package fr.cnes.regards.modules.delivery.amqp.input;

import fr.cnes.regards.framework.amqp.event.*;
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.modules.delivery.dto.input.DeliveryRequestDto;
import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.MessageProperties;

import java.util.Objects;
import java.util.Optional;

/**
 * An amqp message for the delivery request event {@link DeliveryRequestDto}.
 *
 * @author Iliana Ghazali
 **/
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class DeliveryRequestDtoEvent extends DeliveryRequestDto implements ISubscribable, IMessagePropertiesAware {

    /**
     * Properties of event
     * Prevent GSON converter from serializing this field
     */
    @GsonIgnore
    protected MessageProperties messageProperties;

    public DeliveryRequestDtoEvent(String correlationId, String targetDelivery, OrderRequestDto order) {
        super(correlationId, targetDelivery, order);
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

    @Override
    public Optional<String> getOriginRequestAppId() {
        String appId = this.getMessageProperties().getAppId();
        if (StringUtils.isBlank(appId)) {
            appId = this.getOrder().getUser();
        }
        return Optional.ofNullable(appId);
    }

    @Override
    public void setOriginRequestAppId(String originRequestAppId) {
        this.getMessageProperties().setAppId(originRequestAppId);
    }

    @Override
    public Optional<Integer> getOriginRequestPriority() {
        Integer priority = 1;
        if (this.getMessageProperties().getPriority() != null
            && this.getMessageProperties().getPriority() != MessageProperties.DEFAULT_PRIORITY) {
            priority = this.getMessageProperties().getPriority();
        }
        return Optional.of(priority);
    }

    @Override
    public void setOriginRequestPriority(Integer originRequestPriority) {
        this.getMessageProperties().setPriority(originRequestPriority);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        DeliveryRequestDtoEvent that = (DeliveryRequestDtoEvent) o;
        return Objects.equals(messageProperties, that.messageProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), messageProperties);
    }
}
