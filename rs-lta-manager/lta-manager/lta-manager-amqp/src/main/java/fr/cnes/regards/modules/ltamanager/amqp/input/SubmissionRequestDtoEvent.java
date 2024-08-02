/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.ProductFileDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.MessageProperties;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A submission request event {@link SubmissionRequestDto}
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

    public SubmissionRequestDtoEvent(String correlationId,
                                     String productId,
                                     String datatype,
                                     List<ProductFileDto> files) {
        super(correlationId, productId, datatype, files);
    }

    @Override
    public MessageProperties getMessageProperties() {
        if (messageProperties == null) {
            messageProperties = new MessageProperties();
        }
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
    public Optional<String> getOriginRequestAppId() {
        String appId = this.getMessageProperties().getAppId();
        if (StringUtils.isBlank(appId)) {
            appId = this.getOwner();
        }
        return Optional.ofNullable(appId);
    }

    @Override
    public void setOriginRequestAppId(String originRequestAppId) {
        this.getMessageProperties().setAppId(originRequestAppId);
    }

    @Override
    public Optional<Integer> getOriginRequestPriority() {
        Integer priority = 1;
        if (this.getMessageProperties().getPriority() != null
            && this.getMessageProperties().getPriority() != MessageProperties.DEFAULT_PRIORITY) {
            priority = this.getMessageProperties().getPriority();
        }
        return Optional.of(priority);
    }

    @Override
    public void setOriginRequestPriority(Integer originRequestPriority) {
        this.getMessageProperties().setPriority(originRequestPriority);
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
