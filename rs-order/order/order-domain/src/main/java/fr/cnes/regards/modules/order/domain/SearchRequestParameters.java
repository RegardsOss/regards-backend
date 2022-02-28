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
package fr.cnes.regards.modules.order.domain;

import javax.validation.Valid;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;

import fr.cnes.regards.framework.jpa.restriction.DatesRangeRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.framework.jpa.utils.AbstractSearchParameters;

/**
 * Allows complex research on {@link Order}
 *
 * @author Théo Lasserre
 */
public class SearchRequestParameters implements AbstractSearchParameters<Order> {

    private String owner;

    @Valid
    private ValuesRestriction<OrderStatus> statuses;

    @Valid
    private DatesRangeRestriction creationDate = new DatesRangeRestriction();

    public static SearchRequestParameters build() {
        return new SearchRequestParameters()
                .withStatusesExcluded();
    }

    private Boolean waitingForUser;

    public SearchRequestParameters withWaitingForUser(Boolean waitingForUser) {
        this.waitingForUser = waitingForUser;
        return this;
    }

    public Boolean getWaitingForUser() { return waitingForUser; }

    public SearchRequestParameters withOwner(String owner) {
        this.owner = owner;
        return this;
    }

    public DatesRangeRestriction getCreationDate() {
        return creationDate;
    }

    public SearchRequestParameters withCreationDateBefore(OffsetDateTime before) {
        this.creationDate = DatesRangeRestriction.buildBefore(before);
        return this;
    }

    public SearchRequestParameters withCreationDateAfter(OffsetDateTime after) {
        this.creationDate = DatesRangeRestriction.buildAfter(after);
        return this;
    }

    public SearchRequestParameters withCreateDateBeforeAndAfter(OffsetDateTime before, OffsetDateTime after) {
        this.creationDate = DatesRangeRestriction.buildBeforeAndAfter(before, after);
        return this;
    }

    public ValuesRestriction<OrderStatus> getStatuses() { return this.statuses; }

    public SearchRequestParameters withStatusesIncluded(OrderStatus ...status) {
        this.statuses = new ValuesRestriction<OrderStatus>().withInclude(Arrays.asList(status));
        return this;
    }

    public SearchRequestParameters withStatusesIncluded(Collection<OrderStatus> status) {
        this.statuses = new ValuesRestriction<OrderStatus>().withInclude(status);
        return this;
    }

    public SearchRequestParameters withStatusesExcluded(OrderStatus ...status) {
        this.statuses = new ValuesRestriction<OrderStatus>().withExclude(Arrays.asList(status));
        return this;
    }

    public SearchRequestParameters withStatusesExcluded(Collection<OrderStatus> status) {
        this.statuses = new ValuesRestriction<OrderStatus>().withExclude(status);
        return this;
    }

    public String getOwner() {
        return owner;
    }
}
