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
package fr.cnes.regards.modules.ltamanager.amqp.output;

import fr.cnes.regards.framework.amqp.event.*;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.ProductFileDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AMQP event class in order to communicate with LTA Clean worker (See {@link SubmissionRequestDto})
 *
 * @author Stephane Cortine
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class LtaCleanWorkerRequestDtoEvent extends SubmissionRequestDto
    implements ISubscribable, IMessagePropertiesAware {

    /**
     * Properties of event
     * Prevent GSON converter from serializing this field
     */
    @GsonIgnore
    private MessageProperties messageProperties;

    public LtaCleanWorkerRequestDtoEvent(String correlationId,
                                         String id,
                                         String datatype,
                                         @Nullable IGeometry geometry,
                                         List<ProductFileDto> files,
                                         @Nullable List<String> tags,
                                         @Nullable String originUrn,
                                         @Nullable Map<String, Object> properties,
                                         @Nullable String storePath,
                                         @Nullable String session,
                                         boolean replaceMode) {
        super(correlationId,
              id,
              datatype,
              geometry,
              files,
              tags,
              originUrn,
              properties,
              storePath,
              session,
              replaceMode);
        this.messageProperties = new MessageProperties();
    }

    @Override
    public void setMessageProperties(MessageProperties messageProperties) {
        this.messageProperties = messageProperties;
    }

    @Override
    public MessageProperties getMessageProperties() {
        return messageProperties;
    }

    /**
     * Add a header in the event
     *
     * @param key   the key of header
     * @param value the value of header
     */
    public void addHeader(String key, @Nullable String value) {
        Assert.notNull(key, "key is mandatory for header ! Make sure other constraints are satisfied.");
        if (messageProperties == null) {
            return;
        }
        messageProperties.setHeader(key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        LtaCleanWorkerRequestDtoEvent that = (LtaCleanWorkerRequestDtoEvent) o;
        return messageProperties.equals(that.messageProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), messageProperties);
    }
}
