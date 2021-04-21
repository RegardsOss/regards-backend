/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.client;

import java.util.Set;
import java.util.UUID;

import org.springframework.lang.Nullable;

/**
 * @author Marc SORDI
 */
public class RequestInfo {

    /**
     * Request unique identifier
     */
    private String requestId;

    /**
     * Provider identifier (i.e. feature id)
     */
    private String providerId;

    /**
     * Unique Resource Name : internal REGARDS versioned unique identifier
     */
    private String sipId;

    /**
     * List of errors
     */
    private Set<String> errors;

    public static RequestInfo build(String providerId, @Nullable String urn, @Nullable Set<String> errors) {
        return build(UUID.randomUUID().toString(), providerId, urn, errors);
    }

    public static RequestInfo build(String requestId, String providerId, @Nullable String urn,
            @Nullable Set<String> errors) {
        RequestInfo ri = new RequestInfo();
        ri.setRequestId(requestId);
        ri.setProviderId(providerId);
        ri.setSipId(urn);
        ri.setErrors(errors);
        return ri;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
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

    public void setSipId(String urn) {
        this.sipId = urn;
    }

    public Set<String> getErrors() {
        return errors;
    }

    public void setErrors(Set<String> errors) {
        this.errors = errors;
    }

}
