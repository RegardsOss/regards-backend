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
package fr.cnes.regards.modules.delivery.domain.input;

import fr.cnes.regards.modules.delivery.dto.input.DeliveryRequestDto;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryErrorType;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import org.springframework.util.Assert;

import jakarta.annotation.Nullable;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * A delivery request contains information about an order previously requested.
 *
 * @author Iliana Ghazali
 **/
@Entity
@Table(name = "t_delivery_request")
public class DeliveryRequest {

    @Id
    @SequenceGenerator(name = "deliveryRequestSequence", initialValue = 1, sequenceName = "seq_delivery_request")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "deliveryRequestSequence")
    private Long id;

    @Column(name = "correlation_id", nullable = false, updatable = false, unique = true)
    @NotBlank(message = "correlationId is required to track this request.")
    private String correlationId;

    @Column(name = "user_name", length = 128, nullable = false, updatable = false)
    @NotBlank(message = "userName is required")
    private String userName;

    @Embedded
    @Valid
    private DeliveryStatus deliveryStatus;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "total_sub_orders")
    private Integer totalSubOrders;

    @Column(name = "origin_request_app_id", nullable = false, updatable = false)
    private String originRequestAppId;

    @Column(name = "origin_request_priority", updatable = false)
    private Integer originRequestPriority;

    // attribute to handle concurrency with optiminic lock
    @Version
    private int version;

    public DeliveryRequest() {
        // no-args constructor for jpa
    }

    public DeliveryRequest(String correlationId,
                           String userName,
                           DeliveryStatus deliveryStatus,
                           @Nullable Long orderId,
                           @Nullable Integer totalSubOrders,
                           String originRequestAppId,
                           Integer originRequestPriority) {
        Assert.notNull(correlationId, "correlationId is mandatory !");
        Assert.notNull(userName, "userName is mandatory !");
        Assert.notNull(deliveryStatus, "deliveryStatus is mandatory !");
        Assert.notNull(originRequestAppId, "originAppId is mandatory !");

        this.correlationId = correlationId;
        this.userName = userName;
        this.deliveryStatus = deliveryStatus;
        this.originRequestAppId = originRequestAppId;
        this.orderId = orderId;
        this.totalSubOrders = totalSubOrders;
        this.originRequestPriority = originRequestPriority;
    }

    public static DeliveryRequest buildGrantedDeliveryRequest(DeliveryRequestDto deliveryDto,
                                                              int requestTtlExpiresHours,
                                                              String originRequestAppId,
                                                              Integer originRequestPriority) {
        OffsetDateTime now = OffsetDateTime.now();
        return new DeliveryRequest(deliveryDto.getCorrelationId(),
                                   deliveryDto.getOrder().getUser(),
                                   new DeliveryStatus(now,
                                                      now,
                                                      requestTtlExpiresHours,
                                                      DeliveryRequestStatus.GRANTED,
                                                      null,
                                                      null),
                                   null,
                                   null,
                                   originRequestAppId,
                                   originRequestPriority);
    }

    public void update(Long orderId,
                       Integer totalSubOrders,
                       DeliveryRequestStatus status,
                       @Nullable DeliveryErrorType errorType,
                       @Nullable String errorCause) {
        this.orderId = orderId;
        this.totalSubOrders = totalSubOrders;
        this.deliveryStatus.setStatus(status);
        this.deliveryStatus.setStatusDate(OffsetDateTime.now());
        this.deliveryStatus.setError(errorType, errorCause);
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getUserName() {
        return userName;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public OffsetDateTime getCreationDate() {
        return getDeliveryStatus().getCreationDate();
    }

    public OffsetDateTime getExpiryDate() {
        return getDeliveryStatus().getExpiryDate();
    }

    public OffsetDateTime getStatusDate() {
        return getDeliveryStatus().getStatusDate();
    }

    public DeliveryRequestStatus getStatus() {
        return getDeliveryStatus().getStatus();
    }

    @Nullable
    public String getErrorCause() {
        return getDeliveryStatus().getErrorCause();
    }

    @Nullable
    public DeliveryErrorType getErrorType() {
        return getDeliveryStatus().getErrorType();
    }

    @Nullable
    public Long getOrderId() {
        return orderId;
    }

    @Nullable
    public Integer getTotalSubOrders() {
        return totalSubOrders;
    }

    public String getOriginRequestAppId() {
        return originRequestAppId;
    }

    public Integer getOriginRequestPriority() {
        return originRequestPriority;
    }

    public Long getId() {
        return id;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeliveryRequest that = (DeliveryRequest) o;
        return correlationId.equals(that.correlationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(correlationId);
    }

    @Override
    public String toString() {
        return "DeliveryRequest{"
               + "id="
               + id
               + ", correlationId='"
               + correlationId
               + '\''
               + ", userName='"
               + userName
               + '\''
               + ", deliveryStatus="
               + deliveryStatus
               + ", orderId="
               + orderId
               + ", totalSubOrders="
               + totalSubOrders
               + ", originRequestAppId='"
               + originRequestAppId
               + '\''
               + ", originRequestPriority="
               + originRequestPriority
               + ", version="
               + version
               + '}';
    }
}
