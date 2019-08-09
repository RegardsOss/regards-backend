/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.domain.flow;

import java.util.Collection;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestType;

/**
 * @author sbinda
 *
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class RetryFlowItem implements ISubscribable {

    private String requestId;

    private Collection<String> owners;

    private FileRequestType type;

    public String getRequestId() {
        return requestId;
    }

    public FileRequestType getType() {
        return type;
    }

    public Collection<String> getOwners() {
        return owners;
    }

    public static RetryFlowItem buildStorageRetry(String requestId) {
        RetryFlowItem request = new RetryFlowItem();
        request.requestId = requestId;
        request.type = FileRequestType.STORAGE;
        return request;
    }

    public static RetryFlowItem buildStorageRetry(Collection<String> owners) {
        RetryFlowItem request = new RetryFlowItem();
        request.owners = owners;
        request.type = FileRequestType.STORAGE;
        return request;
    }

    public static RetryFlowItem buildAvailabilityRetry(String requestId) {
        RetryFlowItem request = new RetryFlowItem();
        request.requestId = requestId;
        request.type = FileRequestType.AVAILABILITY;
        return request;
    }

}
