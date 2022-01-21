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
package fr.cnes.regards.modules.feature.dto;

import java.time.OffsetDateTime;
import java.util.List;

import org.apache.commons.compress.utils.Lists;

import fr.cnes.regards.modules.feature.dto.event.out.RequestState;

/**
 *
 * Parameters to define a selection of feature requests
 *
 * @author Sébastien Binda
 *
 */
public class FeatureRequestsSelectionDTO {

    FeatureRequestSearchParameters filters = new FeatureRequestSearchParameters();

    List<Long> requestIds = Lists.newArrayList();

    SearchSelectionMode requestIdSelectionMode = SearchSelectionMode.INCLUDE;

    public static FeatureRequestsSelectionDTO build() {
        return new FeatureRequestsSelectionDTO();
    }

    public FeatureRequestsSelectionDTO withSource(String source) {
        this.filters.setSource(source);
        return this;
    }

    public FeatureRequestsSelectionDTO withSession(String session) {
        this.filters.setSession(session);
        return this;
    }

    public FeatureRequestsSelectionDTO withProviderId(String providerId) {
        this.filters.setProviderId(providerId);
        return this;
    }

    public FeatureRequestsSelectionDTO withStart(OffsetDateTime start) {
        this.filters.setFrom(start);
        return this;
    }

    public FeatureRequestsSelectionDTO withEnd(OffsetDateTime end) {
        this.filters.setTo(end);
        return this;
    }

    public FeatureRequestsSelectionDTO withState(RequestState state) {
        this.filters.setState(state);
        return this;
    }

    public FeatureRequestsSelectionDTO withStep(FeatureRequestStep step) {
        this.filters.withStep(step);
        return this;
    }

    public FeatureRequestsSelectionDTO withFilters(FeatureRequestSearchParameters filters) {
        this.filters = filters;
        return this;
    }

    public FeatureRequestsSelectionDTO withId(Long id) {
        this.requestIds.add(id);
        return this;
    }

    public FeatureRequestsSelectionDTO withSelectionMode(SearchSelectionMode mode) {
        this.requestIdSelectionMode = mode;
        return this;
    }

    public FeatureRequestSearchParameters getFilters() {
        return filters;
    }

    public void setFilters(FeatureRequestSearchParameters filters) {
        this.filters = filters;
    }

    public List<Long> getRequestIds() {
        return requestIds;
    }

    public void setRequestIds(List<Long> requestIds) {
        this.requestIds = requestIds;
    }

    public SearchSelectionMode getRequestIdSelectionMode() {
        return requestIdSelectionMode;
    }

    public void setRequestIdSelectionMode(SearchSelectionMode requestIdSelectionMode) {
        this.requestIdSelectionMode = requestIdSelectionMode;
    }

}
