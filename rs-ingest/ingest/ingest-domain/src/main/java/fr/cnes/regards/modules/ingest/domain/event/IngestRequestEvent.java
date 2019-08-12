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
package fr.cnes.regards.modules.ingest.domain.event;

import java.util.Set;

import org.springframework.lang.Nullable;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.ingest.domain.entity.request.IngestRequestState;

/**
 * Generic event for all service requests
 *
 * @author Marc SORDI
 *
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class IngestRequestEvent implements ISubscribable {

    /**
     * The request id
     */
    private String requestId;

    /**
     * The feature id
     */
    private String providerId;

    /**
     * The SIP URN
     */
    private String sipId;

    private IngestRequestState state;

    private IngestRequestType type;

    private Set<String> errors;

    public IngestRequestState getState() {
        return state;
    }

    public void setState(IngestRequestState state) {
        this.state = state;
    }

    public IngestRequestType getType() {
        return type;
    }

    public void setType(IngestRequestType type) {
        this.type = type;
    }

    public Set<String> getErrors() {
        return errors;
    }

    public void setErrors(Set<String> errors) {
        this.errors = errors;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getSipId() {
        return sipId;
    }

    public void setSipId(String sipId) {
        this.sipId = sipId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public static IngestRequestEvent build(String requestId, String providerId, @Nullable String sipId,
            IngestRequestState state, IngestRequestType type) {
        return build(requestId, providerId, sipId, state, type, null);
    }

    public static IngestRequestEvent build(String requestId, String providerId, @Nullable String sipId,
            IngestRequestState state, IngestRequestType type, Set<String> errors) {
        IngestRequestEvent event = new IngestRequestEvent();
        event.setProviderId(providerId);
        event.setSipId(sipId);
        event.setState(state);
        event.setType(type);
        event.setErrors(errors);
        return event;
    }

}
