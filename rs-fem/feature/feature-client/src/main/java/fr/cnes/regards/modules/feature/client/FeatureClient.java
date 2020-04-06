/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.client;

import java.util.List;

import org.apache.commons.compress.utils.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureMetadata;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureUpdateRequestEvent;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;

/**
 * BUS Message client for Fem
 *
 * @author Sébastien Binda
 *
 */
@Component
public class FeatureClient {

    @Autowired
    private IPublisher publisher;

    /**
     * Sends {@link FeatureUpdateRequestEvent} to fem manager to handle update of {@link Feature}s
     * @param features {@link Feature}s to patch
     * @param priorityLevel
     * @return update request identifiers
     */
    public List<String> updateFeatures(List<Feature> features, PriorityLevel priorityLevel) {
        List<String> requestIds = Lists.newArrayList();
        for (Feature feature : features) {
            FeatureUpdateRequestEvent event = FeatureUpdateRequestEvent.build(FeatureMetadata.build(priorityLevel),
                                                                              feature);
            publisher.publish(event);
            requestIds.add(event.getRequestId());
        }
        return requestIds;
    }

    /**
     * @param features
     * @param normal
     */
    public List<String> deleteFeatures(List<FeatureUniformResourceName> featureUrns, PriorityLevel priorityLevel) {
        List<String> requestIds = Lists.newArrayList();
        for (FeatureUniformResourceName urn : featureUrns) {
            FeatureDeletionRequestEvent event = FeatureDeletionRequestEvent.build(urn, priorityLevel);
            publisher.publish(event);
            requestIds.add(event.getRequestId());
        }
        return requestIds;
    }

}
