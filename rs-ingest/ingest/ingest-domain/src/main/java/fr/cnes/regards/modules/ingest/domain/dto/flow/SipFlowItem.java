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

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.util.Assert;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.ingest.domain.IngestValidationMessages;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.dto.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.domain.event.IngestRequestEvent;

/**
 * Data flow item to ingest SIP using event driven mechanism.
 *
 * @author Marc SORDI
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class SipFlowItem implements ISubscribable {

    @NotBlank(message = IngestValidationMessages.MISSING_REQUEST_ID_ERROR)
    private String requestId;

    @Valid
    @NotNull(message = IngestValidationMessages.MISSING_METADATA_ERROR)
    private IngestMetadataDto metadata;

    @Valid
    @NotNull(message = IngestValidationMessages.MISSING_SIP_ERROR)
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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * Build a new SIP flow item and generates a unique request id.<br/>
     * An {@link IngestRequestEvent} including this request id will be sent to monitor the progress of the request.
     */
    public static SipFlowItem build(IngestMetadataDto metadata, SIP sip) {
        Assert.notNull(metadata, IngestValidationMessages.MISSING_METADATA_ERROR);
        Assert.notNull(sip, IngestValidationMessages.MISSING_SIP_ERROR);
        SipFlowItem item = new SipFlowItem();
        item.setRequestId(UUID.randomUUID().toString());
        item.setMetadata(metadata);
        item.setSip(sip);
        return item;
    }
}
