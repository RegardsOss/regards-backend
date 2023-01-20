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
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.LtaWorkerRequestDto;
import fr.cnes.regards.modules.workermanager.amqp.events.EventHeadersHelper;
import org.springframework.amqp.core.MessageProperties;

import java.nio.file.Path;
import java.util.Objects;

/**
 * See {@link LtaWorkerRequestDto}
 *
 * @author Iliana Ghazali
 **/
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class LtaWorkerRequestDtoEvent extends LtaWorkerRequestDto implements ISubscribable, IMessagePropertiesAware {

    // Prevent GSON converter from serializing this field
    @GsonIgnore
    private MessageProperties messageProperties;

    public LtaWorkerRequestDtoEvent(String storage,
                                    Path dataTypeStorePath,
                                    String model,
                                    SubmissionRequestDto product,
                                    boolean replace) {
        super(storage, dataTypeStorePath, model, product, replace);
        this.messageProperties = new MessageProperties();
    }

    @Override
    public MessageProperties getMessageProperties() {
        return messageProperties;
    }

    @Override
    public void setMessageProperties(MessageProperties messageProperties) {
        this.messageProperties = messageProperties;
    }

    public void setWorkerHeaders(String contentType, String tenant, String correlationId, String owner, String session) {
        setHeader(EventHeadersHelper.CONTENT_TYPE_HEADER, contentType);
        setHeader(EventHeadersHelper.TENANT_HEADER, tenant);
        setHeader(EventHeadersHelper.REQUEST_ID_HEADER, correlationId);
        setHeader(EventHeadersHelper.OWNER_HEADER, owner);
        setHeader(EventHeadersHelper.SESSION_HEADER, session);
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
        LtaWorkerRequestDtoEvent that = (LtaWorkerRequestDtoEvent) o;
        return messageProperties.equals(that.messageProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), messageProperties);
    }

}
