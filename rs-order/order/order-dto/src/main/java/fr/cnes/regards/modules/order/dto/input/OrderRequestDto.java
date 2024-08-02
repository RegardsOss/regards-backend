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
package fr.cnes.regards.modules.order.dto.input;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.util.Assert;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Contains all necessary metadata to make an order
 *
 * @author Iliana Ghazali
 **/
public class OrderRequestDto {

    /**
     * Lucene queries to select products to order
     */
    @NotEmpty(message = "list of queries should not be empty")
    private final List<String> queries;

    /**
     * see {@link OrderRequestFilters}
     */
    @Valid
    @Nullable
    private final OrderRequestFilters filters;

    /**
     * Order identifier provided by the user. Is nullable in case of REST.
     */
    @Size(message = "provided correlationId must not exceed 255 characters.", max = 255)
    @Nullable
    private String correlationId;

    /**
     * Ordering user login name.
     * Note : Mandatory in AMQP API but null with REST interface.
     */
    @Size(message = "user must not exceed 128 characters.", max = 128)
    @Nullable
    private String user;

    /**
     * Maximum basket size limit.
     * Always null with REST requests.
     */
    @Nullable
    private Long sizeLimitInBytes;

    public OrderRequestDto(List<String> queries,
                           @Nullable OrderRequestFilters filters,
                           @Nullable String correlationId,
                           @Nullable String user,
                           @Nullable Long sizeLimitInBytes) {
        Assert.notEmpty(queries, "at least one query is mandatory!");

        this.queries = queries;
        this.filters = filters;
        this.correlationId = correlationId;
        this.user = user;
        this.sizeLimitInBytes = sizeLimitInBytes;
    }

    public List<String> getQueries() {
        return queries;
    }

    @Nullable
    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(@Nullable String correlationId) {
        this.correlationId = correlationId;
    }

    @Nullable
    public String getUser() {
        return user;
    }

    public void setUser(@Nullable String user) {
        this.user = user;
    }

    @Nullable
    public OrderRequestFilters getFilters() {
        return filters;
    }

    public void setSizeLimitInBytes(@Nullable Long sizeLimitInBytes) {
        this.sizeLimitInBytes = sizeLimitInBytes;
    }

    @Nullable
    public Long getSizeLimitInBytes() {
        return sizeLimitInBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderRequestDto that = (OrderRequestDto) o;
        return queries.equals(that.queries)
               && filters.equals(that.filters)
               && Objects.equals(correlationId,
                                 that.correlationId)
               && Objects.equals(user, that.user)
               && Objects.equals(sizeLimitInBytes, that.sizeLimitInBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queries, filters, correlationId, user, sizeLimitInBytes);
    }

    @Override
    public String toString() {
        return "OrderRequestDto{"
               + "queries="
               + queries
               + ", filters="
               + filters
               + ", correlationId='"
               + correlationId
               + '\''
               + ", user='"
               + user
               + '\''
               + ", sizeLimitInBytes="
               + sizeLimitInBytes
               + '}';
    }
}
