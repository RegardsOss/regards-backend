/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.dto.flow;

import javax.validation.constraints.NotBlank;

import org.springframework.util.Assert;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.ingest.domain.IngestValidationMessages;
import fr.cnes.regards.modules.ingest.domain.dto.event.IngestRequestEvent;

/**
 * Data flow item to delete SIP using event driven mechanism.
 *
 * @author Marc SORDI
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class DeletionRequestFlowItem extends AbstractRequestFlowItem implements ISubscribable {

    /**
     * The SIP internal identifier (generated URN).
     */
    @NotBlank(message = IngestValidationMessages.MISSING_SIPID)
    private String sipId;

    public String getSipId() {
        return sipId;
    }

    public void setSipId(String sipId) {
        this.sipId = sipId;
    }

    /**
     * Build a new deletion flow item with a custom request id.
     * You may generate your request id using {@link #generateRequestId()} or pass your own (max 36 alphanumerical characters)<br/>
     * A {@link IngestRequestEvent} including this request id will be sent to monitor the progress of the request.
     */
    public static DeletionRequestFlowItem build(String requestId, String sipId) {
        Assert.notNull(requestId, IngestValidationMessages.MISSING_REQUEST_ID);
        Assert.notNull(sipId, IngestValidationMessages.MISSING_SIPID);
        DeletionRequestFlowItem item = new DeletionRequestFlowItem();
        item.setRequestId(requestId);
        item.setSipId(sipId);
        return item;
    }

    /**
     * Build a new deletion flow item with a generated unique request id.
     * A {@link IngestRequestEvent} including this request id will be sent to monitor the progress of the request.
     */
    public static DeletionRequestFlowItem build(String sipId) {
        return build(generateRequestId(), sipId);
    }
}
