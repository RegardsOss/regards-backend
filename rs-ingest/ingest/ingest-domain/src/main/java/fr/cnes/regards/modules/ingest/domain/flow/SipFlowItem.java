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
package fr.cnes.regards.modules.ingest.domain.flow;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.util.Assert;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.ingest.domain.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.SIP;

/**
 * Data flow item to ingest SIP using event driven mechanism.
 *
 * @author Marc SORDI
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class SipFlowItem implements ISubscribable {

    private static final String MISSING_METADATA_ERROR = "Ingest metadata is required";

    private static final String MISSING_SIP_ERROR = "SIP is required";

    private static final String MISSING_OWNER_ERROR = "Owner is required";

    @Valid
    @NotNull(message = MISSING_METADATA_ERROR)
    private IngestMetadata metadata;

    @NotNull(message = MISSING_SIP_ERROR)
    private SIP sip;

    @NotBlank(message = MISSING_OWNER_ERROR)
    private String owner;

    public IngestMetadata getMetadata() {
        return metadata;
    }

    public SIP getSip() {
        return sip;
    }

    public void setMetadata(IngestMetadata metadata) {
        this.metadata = metadata;
    }

    public void setSip(SIP sip) {
        this.sip = sip;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public static SipFlowItem build(IngestMetadata metadata, SIP sip, String owner) {
        Assert.notNull(metadata, MISSING_METADATA_ERROR);
        Assert.notNull(sip, MISSING_SIP_ERROR);
        Assert.hasText(owner, MISSING_OWNER_ERROR);
        SipFlowItem item = new SipFlowItem();
        item.setMetadata(metadata);
        item.setSip(sip);
        item.setOwner(owner);
        return item;
    }
}
