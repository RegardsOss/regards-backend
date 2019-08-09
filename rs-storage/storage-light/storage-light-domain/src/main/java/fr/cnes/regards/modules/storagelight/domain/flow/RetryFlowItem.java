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
import fr.cnes.regards.modules.storagelight.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestEvent;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestType;

/**
 * Flow message to to retry error requests.<br/>
 * See {@link FileRequestEvent} for asynchronous responses when request is finished.<br/>
 * See {@link FileReferenceEvent} for asynchronous responses when a file handled.<br/>
 *
 * @author Sébastien Binda
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class RetryFlowItem implements ISubscribable {

    /**
     * Request business identifier to retry
     */
    private String requestId;

    /**
     * Owners to retry errors requests
     */
    private Collection<String> owners;

    /**
     * Request type to retry
     */
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

    /**
     * Build a storage retry request for the request business identifier provided
     * @param requestId
     * @return {@link RetryFlowItem}
     */
    public static RetryFlowItem buildStorageRetry(String requestId) {
        RetryFlowItem request = new RetryFlowItem();
        request.requestId = requestId;
        request.type = FileRequestType.STORAGE;
        return request;
    }

    /**
     * Build a storage retry request for all requests in error of provided owners.
     * @param owners
     * @return {@link RetryFlowItem}
     */
    public static RetryFlowItem buildStorageRetry(Collection<String> owners) {
        RetryFlowItem request = new RetryFlowItem();
        request.owners = owners;
        request.type = FileRequestType.STORAGE;
        return request;
    }

    /**
     * Build an availability retry request for the given request business identifier
     * @param requestId
     * @return {@link RetryFlowItem}
     */
    public static RetryFlowItem buildAvailabilityRetry(String requestId) {
        RetryFlowItem request = new RetryFlowItem();
        request.requestId = requestId;
        request.type = FileRequestType.AVAILABILITY;
        return request;
    }

}
