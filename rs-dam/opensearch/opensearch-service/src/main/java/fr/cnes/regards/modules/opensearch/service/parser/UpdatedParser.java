/*
 * Copyright 2022-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.opensearch.service.parser;

import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import org.springframework.util.MultiValueMap;

/**
 * This {@link IParser} implementation handles the "updated" field of the OpenSearch request and returns an
 * {@link ICriterion} that excludes features having older lastUpdate than provided one.<br>
 *
 * @author Léo Mieulet
 */
public class UpdatedParser implements IParser {

    /**
     * A query param to filter features on the field lastUpdated, return all features having or after the provided date
     */
    public static final String UPDATED_PARAMETER = "updated";

    private static final String UPDATED_FIELD_NAME = "lastUpdate";

    @Override
    public ICriterion parse(MultiValueMap<String, String> parameters) {
        return parameters.containsKey(UPDATED_PARAMETER) ?
            ICriterion.ge(UPDATED_FIELD_NAME, OffsetDateTimeAdapter.parse(parameters.getFirst(UPDATED_PARAMETER))) :
            ICriterion.all();
    }
}
