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

import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
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
    @NotNull(message = "filters should be present")
    @Valid
    private final OrderRequestFilters filters;

    /**
     * Order identifier provided by the user. Is nullable in case of REST.
     */
    @Size(message = "provided correlationId must not exceed 100 characters.", max = 100)
    @Nullable
    private final String correlationId;

    /**
     * Ordering user login name
     */
    @Size(message = "user must not exceed 128 characters.", max = 128)
    @Nullable
    private String user;



    public OrderRequestDto(List<String> queries,
                           OrderRequestFilters filters,
                           @Nullable String correlationId,
                           @Nullable String user) {
        Assert.notEmpty(queries, "at least one query is mandatory!");
        Assert.notNull(filters, "filters are mandatory!");

        this.queries = queries;
        this.filters = filters;
        this.correlationId = correlationId;
        this.user = user;
    }

    public List<String> getQueries() {
        return queries;
    }

    @Nullable
    public String getCorrelationId() {
        return correlationId;
    }

    @Nullable
    public String getUser() {
        return user;
    }

    public void setUser(@Nullable String user) {
        this.user = user;
    }

    public OrderRequestFilters getFilters() {
        return filters;
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
               && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(queries, filters, correlationId, user);
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
               + '}';
    }
}
