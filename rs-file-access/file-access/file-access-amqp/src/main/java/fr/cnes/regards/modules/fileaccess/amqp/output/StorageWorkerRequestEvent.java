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
package fr.cnes.regards.modules.fileaccess.amqp.output;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.IMessagePropertiesAware;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.modules.fileaccess.dto.AbstractStoragePluginConfigurationDto;
import fr.cnes.regards.modules.fileaccess.dto.output.worker.StorageWorkerRequestDto;
import org.springframework.amqp.core.MessageProperties;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Event will be sent to the worker manager for the creation of a storage request.
 *
 * @author Thibaud Michaudel
 **/
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class StorageWorkerRequestEvent extends StorageWorkerRequestDto
    implements ISubscribable, IMessagePropertiesAware {

    public static final String CONTENT_TYPE_HEADER = "content-type";

    public static final String REQUEST_ID_HEADER = "requestId";

    public static final String TENANT_HEADER = "tenant";

    public static final String OWNER_HEADER = "owner";

    public static final String SESSION_HEADER = "session";

    @GsonIgnore
    @NotNull(message = "Message properties is required")
    protected MessageProperties messageProperties;

    public StorageWorkerRequestEvent(String checksum,
                                     String algorithm,
                                     String url,
                                     String destination,
                                     boolean computeImageSize,
                                     boolean activateSmallFilePackaging,
                                     AbstractStoragePluginConfigurationDto parameters) {
        super(checksum, algorithm, url, destination, computeImageSize, activateSmallFilePackaging, parameters);
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
        StorageWorkerRequestEvent that = (StorageWorkerRequestEvent) o;
        return Objects.equals(messageProperties, that.messageProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), messageProperties);
    }

    @Override
    public String toString() {
        return "StorageWorkerRequestEvent{" + "messageProperties=" + messageProperties + "} " + super.toString();
    }
}
