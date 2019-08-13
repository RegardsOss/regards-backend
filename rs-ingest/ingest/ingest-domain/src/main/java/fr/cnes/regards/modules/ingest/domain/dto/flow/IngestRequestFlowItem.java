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
package fr.cnes.regards.modules.ingest.domain.dto.flow;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.util.Assert;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.ingest.domain.IngestValidationMessages;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.dto.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.domain.dto.event.IngestRequestEvent;

/**
 * Data flow item to ingest SIP using event driven mechanism.
 *
 * @author Marc SORDI
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class IngestRequestFlowItem extends AbstractRequestFlowItem implements ISubscribable {

    @Valid
    @NotNull(message = IngestValidationMessages.MISSING_METADATA)
    private IngestMetadataDto metadata;

    @Valid
    @NotNull(message = IngestValidationMessages.MISSING_SIP)
    private SIP sip;

    public IngestMetadataDto getMetadata() {
        return metadata;
    }

    public SIP getSip() {
        return sip;
    }

    public void setMetadata(IngestMetadataDto metadata) {
        this.metadata = metadata;
    }

    public void setSip(SIP sip) {
        this.sip = sip;
    }

    /**
     * Build a new SIP flow item with a custom request id.
     * You may generate your request id using {@link #generateRequestId()} or pass your own (max 36 alphanumerical characters)<br/>
     * An {@link IngestRequestEvent} including this request id will be sent to monitor the progress of the request.
     */
    public static IngestRequestFlowItem build(String requestId, IngestMetadataDto metadata, SIP sip) {
        Assert.notNull(requestId, IngestValidationMessages.MISSING_REQUEST_ID);
        Assert.notNull(metadata, IngestValidationMessages.MISSING_METADATA);
        Assert.notNull(sip, IngestValidationMessages.MISSING_SIP);
        IngestRequestFlowItem item = new IngestRequestFlowItem();
        item.setRequestId(requestId);
        item.setMetadata(metadata);
        item.setSip(sip);
        return item;
    }

    /**
     * Build a new SIP flow item with a generated unique request id.<br/>
     * An {@link IngestRequestEvent} including this request id will be sent to monitor the progress of the request.
     */
    public static IngestRequestFlowItem build(IngestMetadataDto metadata, SIP sip) {
        return build(generateRequestId(), metadata, sip);
    }
}
