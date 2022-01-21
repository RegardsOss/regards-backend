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


package fr.cnes.regards.modules.ingest.dto.request.event;

import java.util.Set;

import org.springframework.lang.Nullable;

import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.modules.ingest.dto.request.RequestState;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeEnum;

import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;

/**
 *
 * @author Iliana Ghazali
 */

public class RequestEvent {

    /**
     * The requestType
     */
    private RequestTypeEnum type;

    /**
     * The request id
     */
    private Long requestId;

    /**
     * Owner of the request
     */
    private String requestOwner;

    /**
     * State of the request
     */
    private RequestState state;

    /**
     * Request errors
     */
    private Set<String> errors;

    /**
     * Specific to AIPs
     *
     * {@link AIPEntity#getAipId()}}
     * {@link AIPEntity#getAipIdUrn()}
     */
    private String aipId;

    private OaisUniformResourceName urn;


    public static RequestEvent build(RequestTypeEnum type, String requestId, String requestOwner, @Nullable String aipId,
            @Nullable OaisUniformResourceName urn, RequestState state, Set<String> errors) {
        RequestEvent event = new RequestEvent();
        event.setType(type);
        //event.setRequestId(requestId);
        event.setAipId(aipId);
        event.setUrn(urn);
        event.setState(state);
        event.setErrors(errors);
        event.setRequestOwner(requestOwner);
        return event;
    }

    public RequestTypeEnum getType() {
        return type;
    }

    public void setType(RequestTypeEnum type) {
        this.type = type;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getRequestOwner() {
        return requestOwner;
    }

    public void setRequestOwner(String requestOwner) {
        this.requestOwner = requestOwner;
    }

    public RequestState getState() {
        return state;
    }

    public void setState(RequestState state) {
        this.state = state;
    }

    public Set<String> getErrors() {
        return errors;
    }

    public void setErrors(Set<String> errors) {
        this.errors = errors;
    }

    public String getAipId() {
        return aipId;
    }

    public void setAipId(String aipId) {
        this.aipId = aipId;
    }

    public OaisUniformResourceName getUrn() {
        return urn;
    }

    public void setUrn(OaisUniformResourceName urn) {
        this.urn = urn;
    }
}
