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
package fr.cnes.regards.modules.feature.dto.event.in;

import fr.cnes.regards.framework.amqp.event.*;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureMetadata;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * An amqp message to request a {@link Feature} update.
 *
 * @author Marc SORDI
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class FeatureUpdateRequestEvent extends AbstractRequestEvent implements ISubscribable {

    /**
     * Patch feature : only contains changes
     */
    @Valid
    @NotNull(message = "Feature is required")
    private Feature feature;

    @Valid
    @NotNull(message = "Metadata are required")
    private FeatureMetadata metadata;

    public Feature getFeature() {
        return feature;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }

    public FeatureMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(FeatureMetadata metadata) {
        this.metadata = metadata;
    }

    public static FeatureUpdateRequestEvent build(String requestOwner, FeatureMetadata metadata, Feature feature) {
        return build(requestOwner, metadata, feature, OffsetDateTime.now());
    }

    public static FeatureUpdateRequestEvent build(String requestOwner,
                                                  FeatureMetadata metadata,
                                                  Feature feature,
                                                  OffsetDateTime requestDate) {
        FeatureUpdateRequestEvent event = new FeatureUpdateRequestEvent();
        event.setFeature(feature);
        event.setRequestId(generateRequestId());
        event.setRequestDate(requestDate);
        event.setMetadata(metadata);
        event.setRequestOwner(requestOwner);
        return event;
    }
}
