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
import javax.validation.constraints.Size;

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
public class IngestRequestFlowItem implements ISubscribable {

    @NotBlank(message = IngestValidationMessages.MISSING_REQUEST_ID_ERROR)
    @Size(max = 36)
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
     * Build a new SIP flow item with a custom request id.
     * You may generate your request id using {@link #generateRequestId()} or pass your own (max 36 alphanumerical characters)<br/>
     * An {@link IngestRequestEvent} including this request id will be sent to monitor the progress of the request.
     */
    public static IngestRequestFlowItem build(String requestId, IngestMetadataDto metadata, SIP sip) {
        Assert.notNull(requestId, IngestValidationMessages.MISSING_REQUEST_ID_ERROR);
        Assert.notNull(metadata, IngestValidationMessages.MISSING_METADATA_ERROR);
        Assert.notNull(sip, IngestValidationMessages.MISSING_SIP_ERROR);
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

    /**
     * Generate a request ID
     */
    public static String generateRequestId() {
        return UUID.randomUUID().toString();
    }
}
