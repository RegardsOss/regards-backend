/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;

import org.apache.commons.compress.utils.Lists;

/**
 *
 * @author Sébastien Binda
 *
 */
public class FeatureRequestsSelectionDTO {

    FeatureRequestSearchParameters filters;

    List<String> excludedUrns = Lists.newArrayList();

    public FeatureRequestSearchParameters getFilters() {
        return filters;
    }

    public void setFilters(FeatureRequestSearchParameters filters) {
        this.filters = filters;
    }

    public List<String> getExcludedUrns() {
        return excludedUrns;
    }

    public void setExcludedUrns(List<String> excludedUrns) {
        this.excludedUrns = excludedUrns;
    }

}
