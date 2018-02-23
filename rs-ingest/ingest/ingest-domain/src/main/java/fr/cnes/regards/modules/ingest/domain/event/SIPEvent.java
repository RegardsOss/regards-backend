/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.event;

import java.time.ZoneOffset;
import java.util.List;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

/**
 * AMQP Event to inform system for all {@link SIPEntity} state modification
 * @author Sébastien Binda
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class SIPEvent implements ISubscribable {

    private SIPState state;

    private String ipId;

    private String sipId;

    private String owner;

    private String ingestDate;

    private List<String> processingErrors;

    @SuppressWarnings("unused")
    private SIPEvent() {
        // Used for (de)serialization
    }

    public SIPEvent(SIPEntity sip) {
        // Data provider
        owner = sip.getOwner();
        // Ingest date
        ingestDate = sip.getIngestDate().atZoneSameInstant(ZoneOffset.UTC)
                .format(OffsetDateTimeAdapter.ISO_DATE_TIME_UTC);
        // SIP provider ID
        sipId = sip.getSipId();
        // SIP system ID (with version)
        ipId = sip.getIpId();
        // SIP state
        state = sip.getState();
        // SIP errors
        processingErrors = sip.getProcessingErrors();
    }

    public SIPState getState() {
        return state;
    }

    public String getIpId() {
        return ipId;
    }

    public String getSipId() {
        return sipId;
    }

    public void setSipId(String sipId) {
        this.sipId = sipId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getIngestDate() {
        return ingestDate;
    }

    public void setIngestDate(String ingestDate) {
        this.ingestDate = ingestDate;
    }

    public List<String> getProcessingErrors() {
        return processingErrors;
    }

    public void setProcessingErrors(List<String> processingErrors) {
        this.processingErrors = processingErrors;
    }

}
