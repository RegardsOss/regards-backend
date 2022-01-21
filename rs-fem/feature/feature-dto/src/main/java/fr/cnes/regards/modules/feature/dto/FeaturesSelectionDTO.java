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

/**
 *
 * Parameters to define a selection of {@link FeatureEntityDto}
 *
 * @author Sébastien Binda
 *
 */
public class FeaturesSelectionDTO {

    /**
     * Search parameters
     */
    FeaturesSearchParameters filters = new FeaturesSearchParameters();

    /**
     * List of ids to include/exclude according to {@link FeaturesSelectionDTO#featureIdsSelectionMode}
     */
    List<Long> featureIds = Lists.newArrayList();

    /**
     * Feature ids selection mode
     */
    SearchSelectionMode featureIdsSelectionMode = SearchSelectionMode.INCLUDE;

    public static FeaturesSelectionDTO build() {
        return new FeaturesSelectionDTO();
    }

    public FeaturesSelectionDTO withSource(String source) {
        this.filters.setSource(source);
        return this;
    }

    public FeaturesSelectionDTO withSession(String session) {
        this.filters.setSession(session);
        return this;
    }

    public FeaturesSelectionDTO withProviderId(String providerId) {
        this.filters.setProviderId(providerId);
        return this;
    }

    public FeaturesSelectionDTO withFrom(OffsetDateTime from) {
        this.filters.setFrom(from);
        return this;
    }

    public FeaturesSelectionDTO withTo(OffsetDateTime to) {
        this.filters.setTo(to);
        return this;
    }

    public FeaturesSelectionDTO withId(Long id) {
        this.featureIds.add(id);
        return this;
    }

    public FeaturesSelectionDTO withSelectionMode(SearchSelectionMode mode) {
        this.featureIdsSelectionMode = mode;
        return this;
    }

    public FeaturesSearchParameters getFilters() {
        return filters;
    }

    public void setFilters(FeaturesSearchParameters filters) {
        this.filters = filters;
    }

    public SearchSelectionMode getFeatureIdsSelectionMode() {
        return featureIdsSelectionMode;
    }

    public void setFeatureIdsSelectionMode(SearchSelectionMode featureIdsSelectionMode) {
        this.featureIdsSelectionMode = featureIdsSelectionMode;
    }

    public List<Long> getFeatureIds() {
        return featureIds;
    }

    public void setFeatureIds(List<Long> featureIds) {
        this.featureIds = featureIds;
    }

    public FeaturesSelectionDTO withModel(String model) {
        this.filters.withModel(model);
        return this;
    }

    public FeaturesSelectionDTO withFilters(FeaturesSearchParameters filters) {
        this.setFilters(filters);
        return this;
    }

}
