/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Stephane Cortine
 */
public class RecipientsSearchFeatureSimpleEntityParameters {

    /**
     * Search parameters for the entity {@link FeatureSimpleEntity}
     */
    @Schema(description = "Search parameters for the entity FeatureSimpleEntity")
    private SearchFeatureSimpleEntityParameters searchParameters;

    /**
     * List of recipients(business identifiers of plugin configurations
     * {@link fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration}) for the direct
     * notification
     */
    @Schema(description = "List of recipient(business identifiers) for direct notification")
    private Set<String> recipientIds = new HashSet<>();

    public Set<String> getRecipientIds() {
        return recipientIds;
    }

    public SearchFeatureSimpleEntityParameters getSearchParameters() {
        return searchParameters;
    }

    public void setSearchParameters(SearchFeatureSimpleEntityParameters searchParameters) {
        this.searchParameters = searchParameters;
    }

    public void setRecipientIds(Set<String> recipientIds) {
        this.recipientIds = recipientIds;
    }

}
