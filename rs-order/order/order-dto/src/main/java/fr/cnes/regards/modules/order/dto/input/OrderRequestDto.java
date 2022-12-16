/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.hibernate.validator.constraints.Length;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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
    private List<String> queries;

    /**
     * Order identifier
     */
    @NotBlank(message = "correlationId must be present")
    @Length(message = "correlationId must not exceed 100 characters.", max = 100)
    private String correlationId;

    /**
     * Ordering user login name
     */
    @NotBlank(message = "user should be present")
    private String user;

    /**
     * see {@link OrderRequestFilters}
     */
    @NotNull(message = "filters should be present")
    @Valid
    private OrderRequestFilters filters;

    public OrderRequestDto(List<String> queries, String correlationId, String user, OrderRequestFilters filters) {
        this.queries = queries;
        this.correlationId = correlationId;
        this.user = user;
        this.filters = filters;
    }

    public List<String> getQueries() {
        return queries;
    }

    public void setQueries(List<String> queries) {
        this.queries = queries;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public OrderRequestFilters getFilters() {
        return filters;
    }

    public void setFilters(OrderRequestFilters filters) {
        this.filters = filters;
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
        return correlationId.equals(that.correlationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(correlationId);
    }

    @Override
    public String toString() {
        return "OrderRequestDto{"
               + "queries="
               + queries
               + ", correlationId='"
               + correlationId
               + '\''
               + ", user='"
               + user
               + '\''
               + ", filters="
               + filters
               + '}';
    }
}
