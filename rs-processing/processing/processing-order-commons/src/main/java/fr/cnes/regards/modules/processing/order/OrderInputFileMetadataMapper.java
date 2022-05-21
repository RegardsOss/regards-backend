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

package fr.cnes.regards.modules.processing.order;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;

import static fr.cnes.regards.modules.processing.order.Constants.*;

/**
 * This class is a mapper for {@link OrderInputFileMetadata}.
 *
 * @author gandrieu
 */
public class OrderInputFileMetadataMapper extends AbstractMapper<OrderInputFileMetadata> {

    @Override
    public Map<String, String> toMap(OrderInputFileMetadata params) {
        return HashMap.of(INTERNAL,
                          params.getInternal().toString(),
                          FEATURE_ID,
                          params.getFeatureId().toString(),
                          STORED_PATH,
                          params.getStoredPath());
    }

    @Override
    public Option<OrderInputFileMetadata> fromMap(Map<String, String> map) {
        return parseBoolean(map, INTERNAL).flatMap(internal -> parseUrn(map,
                                                                        FEATURE_ID).map(urn -> new OrderInputFileMetadata(
            internal,
            urn,
            parseString(map, STORED_PATH).getOrNull())));
    }

}
