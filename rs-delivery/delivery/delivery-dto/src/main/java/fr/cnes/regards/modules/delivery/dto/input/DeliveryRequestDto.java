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
package fr.cnes.regards.modules.delivery.dto.input;

import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.util.Assert;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Objects;
import java.util.Optional;

/**
 * Dto to create a delivery request. It is used to create automatically an order and receive results in the
 * specified target storage.
 *
 * @author Iliana Ghazali
 **/
public class DeliveryRequestDto {

    /**
     * Identifier provided by the user. Used to monitor the request.
     */
    @Size(message = "provided correlation identifier must not exceed 255 characters.", max = 255)
    @NotBlank(message = "correlation identifier is required to track this delivery.")
    private final String correlationId;

    /**
     * Bucket destination name to deliver orders.
     */
    @NotBlank(message = "targetDelivery is mandatory")
    private final String targetDelivery;

    /**
     * Metadata about the order to create.
     * Order request correlation identifier must be the same as the delivery request.
     */
    @Valid
    private OrderRequestDto order;

    @GsonIgnore
    @Schema(description = "Origin request app_id in amqp message (header property of amqp message)")
    private String originRequestAppId;

    @GsonIgnore
    @Schema(description = "Origin request priority in amqp message (header property of amqp message")
    private Integer originRequestPriority;

    public DeliveryRequestDto(String correlationId, String targetDelivery, OrderRequestDto order) {
        Assert.notNull(correlationId, "correlationId is mandatory !");
        Assert.notNull(targetDelivery, "targetDelivery is mandatory !");
        Assert.notNull(order, "order is mandatory !");
        Assert.notNull(order.getUser(), "order user is mandatory !");

        this.correlationId = correlationId;
        this.targetDelivery = targetDelivery;
        // force order.correlationId with the same delivery request correlationId
        order.setCorrelationId(correlationId);
        this.order = order;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getTargetDelivery() {
        return targetDelivery;
    }

    public OrderRequestDto getOrder() {
        return order;
    }

    public Optional<String> getOriginRequestAppId() {
        return Optional.ofNullable(originRequestAppId);
    }

    public void setOriginRequestAppId(String originRequestAppId) {
        this.originRequestAppId = originRequestAppId;
    }

    public Optional<Integer> getOriginRequestPriority() {
        return Optional.ofNullable(originRequestPriority);
    }

    public void setOriginRequestPriority(Integer originRequestPriority) {
        this.originRequestPriority = originRequestPriority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeliveryRequestDto that = (DeliveryRequestDto) o;
        return Objects.equals(correlationId, that.correlationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(correlationId);
    }

    @Override
    public String toString() {
        return "DeliveryRequestDto{"
               + "correlationId='"
               + correlationId
               + '\''
               + ", targetDelivery='"
               + targetDelivery
               + '\''
               + ", order="
               + order
               + ", originRequestAppId='"
               + originRequestAppId
               + '\''
               + ", originRequestPriority="
               + originRequestPriority
               + '}';
    }
}
