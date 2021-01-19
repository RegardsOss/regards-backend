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
package fr.cnes.regards.modules.featureprovider.domain;

import java.time.OffsetDateTime;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.google.gson.JsonObject;

import fr.cnes.regards.framework.amqp.event.AbstractRequestEvent;
import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.feature.dto.FeatureCreationSessionMetadata;

/**
 * Request extraction for new feature creation from a location
 *
 * @author Kevin Marchois
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class FeatureExtractionRequestEvent extends AbstractRequestEvent implements ISubscribable {

    @Valid
    @NotNull(message = "Request metadata is required")
    private FeatureCreationSessionMetadata metadata;

    @NotBlank(message = "Extraction factory identified by a plugin business identifier is required")
    private String factory;

    /**
     * Free parameters that target factory must understand
     */
    @NotNull(message = "Extraction parameters must not be empty")
    private JsonObject parameters;

    public static FeatureExtractionRequestEvent build(String requestOwner, FeatureCreationSessionMetadata metadata,
            JsonObject parameters, String factory) {
        return build(requestOwner, metadata, parameters, OffsetDateTime.now().minusSeconds(1), factory);
    }

    public static FeatureExtractionRequestEvent build(String requestOwner, FeatureCreationSessionMetadata metadata,
            JsonObject parameters, OffsetDateTime requestDate, String factory) {
        FeatureExtractionRequestEvent event = new FeatureExtractionRequestEvent();
        event.setParameters(parameters);
        event.setRequestId(generateRequestId());
        event.setMetadata(metadata);
        event.setRequestDate(requestDate);
        event.setFactory(factory);
        event.setRequestOwner(requestOwner);
        return event;
    }

    public String getFactory() {
        return factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }

    public FeatureCreationSessionMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(FeatureCreationSessionMetadata metadata) {
        this.metadata = metadata;
    }

    public JsonObject getParameters() {
        return parameters;
    }

    public void setParameters(JsonObject parameters) {
        this.parameters = parameters;
    }

}
