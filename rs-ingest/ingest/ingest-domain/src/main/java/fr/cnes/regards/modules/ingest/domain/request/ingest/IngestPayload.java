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
package fr.cnes.regards.modules.ingest.domain.request.ingest;

import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.dto.request.RequestState;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;

import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Léo Mieulet
 */
public class IngestPayload {

    private Set<IngestRequestError> RequestErrors = new HashSet<>();

    private IngestMetadata metadata;

    private String requestId;

    @NotNull(message = "Ingest request state is required")
    private RequestState state;

    /**
     * All internal request steps including local and remote ones
     */
    @NotNull(message = "Ingest request step is required")
    private IngestRequestStep step;

    private SIP sip;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public RequestState getState() {
        return state;
    }

    public void setState(RequestState state) {
        this.state = state;
    }

    public IngestMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(IngestMetadata metadata) {
        this.metadata = metadata;
    }

    public SIP getSip() {
        return sip;
    }

    public void setSip(SIP sip) {
        this.sip = sip;
    }

    /**
     * @param step local step
     */
    public void setStep(IngestRequestStep step) {
        if (step.isRemote() && step.withTimeout()) {
            throw new IllegalArgumentException("Remote step needs a timeout, use dedicated setter!");
        }
        this.step = step;
    }

    /**
     * @param step              remote step
     * @param remoteStepTimeout timeout in minute
     */
    public void setStep(IngestRequestStep step, long remoteStepTimeout) {
        if (!step.isRemote() && !step.withTimeout()) {
            throw new IllegalArgumentException("Local step don't need timeout, use dedicated setter!");
        }
        this.step = step;
    }

    public IngestRequestStep getStep() {
        return step;
    }

    public Set<IngestRequestError> getRequestErrors() {
        return RequestErrors;
    }

}
