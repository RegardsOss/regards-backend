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
package fr.cnes.regards.modules.ltamanager.amqp.input;

import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.event.*;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.ProductFileDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import org.springframework.amqp.core.MessageProperties;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * See {@link SubmissionRequestDto}
 *
 * @author Iliana Ghazali
 **/
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class SubmissionRequestDtoEvent extends SubmissionRequestDto implements ISubscribable, IMessagePropertiesAware {

    /**
     * Properties of event
     * Prevent GSON converter from serializing this field
     */
    @GsonIgnore
    private MessageProperties messageProperties;

    public SubmissionRequestDtoEvent(String correlationId, String id, String datatype, List<ProductFileDto> files) {
        super(correlationId, id, datatype, files);
        this.messageProperties = new MessageProperties();
    }

    public SubmissionRequestDtoEvent(String correlationId,
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
        this(correlationId, id, datatype, files);
        this.setGeometry(geometry);
        this.setTags(tags);
        this.setOriginUrn(originUrn);
        this.setProperties(properties);
        this.setStorePath(storePath);
        this.setSession(session);
        this.setReplaceMode(replaceMode);

    }

    @Override
    public MessageProperties getMessageProperties() {
        return messageProperties;
    }

    @Override
    public void setMessageProperties(MessageProperties messageProperties) {
        this.messageProperties = messageProperties;
    }

    @Override
    public String getOwner() {
        return this.getMessageProperties().getHeader(AmqpConstants.REGARDS_REQUEST_OWNER_HEADER);
    }

    @Override
    public void setOwner(String owner) {
        setHeader(AmqpConstants.REGARDS_REQUEST_OWNER_HEADER, owner);
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
        SubmissionRequestDtoEvent that = (SubmissionRequestDtoEvent) o;
        return messageProperties.equals(that.messageProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), messageProperties);
    }
}
