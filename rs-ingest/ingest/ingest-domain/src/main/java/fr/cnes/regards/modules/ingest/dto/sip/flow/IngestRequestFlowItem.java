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
package fr.cnes.regards.modules.ingest.dto.sip.flow;

import fr.cnes.regards.framework.amqp.event.*;
import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.modules.ingest.domain.IngestValidationMessages;
import fr.cnes.regards.modules.ingest.dto.request.event.IngestRequestEvent;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.util.Assert;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Data flow item to ingest SIP using event driven mechanism.
 *
 * @author Marc SORDI
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class IngestRequestFlowItem extends AbstractRequestFlowItem implements ISubscribable, IMessagePropertiesAware {

    // Prevent GSON converter from serializing this field
    @GsonIgnore
    protected MessageProperties messageProperties;

    @Valid
    @NotNull(message = IngestValidationMessages.MISSING_METADATA)
    private IngestMetadataDto metadata;

    @Valid
    @NotNull(message = IngestValidationMessages.MISSING_SIP)
    private SIPDto sip;

    public IngestMetadataDto getMetadata() {
        return metadata;
    }

    public SIPDto getSip() {
        return sip;
    }

    public void setMetadata(IngestMetadataDto metadata) {
        this.metadata = metadata;
    }

    public void setSip(SIPDto sip) {
        this.sip = sip;
    }

    /**
     * Build a new SIP flow item with a custom request id.
     * You may generate your request id using {@link #generateRequestId()} or pass your own (max 36 alphanumerical characters)<br/>
     * An {@link IngestRequestEvent} including this request id will be sent to monitor the progress of the request.
     */
    public static IngestRequestFlowItem build(String requestId, IngestMetadataDto metadata, SIPDto sip) {
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
    public static IngestRequestFlowItem build(IngestMetadataDto metadata, SIPDto sip) {
        return build(generateRequestId(), metadata, sip);
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
}
