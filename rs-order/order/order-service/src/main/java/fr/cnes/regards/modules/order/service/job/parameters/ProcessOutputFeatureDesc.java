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
package fr.cnes.regards.modules.order.service.job.parameters;

import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;

/**
 * This class allows to keep the necessary information about EntityFeatures when creating
 * correlations IDs for data files sent to processing. This way, input data files keep the
 * information of their parent feature, and this information is propagated back in process
 * output files, which refer to their corresponding input correlation ID.
 *
 * @author Guillaume Andrieu
 */
@lombok.Value
public class ProcessOutputFeatureDesc {
    String label;
    String ipId;

    @Override
    public String toString() {
        return String.format("{\"label\":\"%s\", \"ipId\":\"%s\"}", label, ipId);
    }

    public static ProcessOutputFeatureDesc from(EntityFeature feature) {
        return new ProcessOutputFeatureDesc(feature.getLabel(), feature.getId().toString());
    }
}
