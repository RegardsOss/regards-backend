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
package fr.cnes.regards.modules.ingest.dto.request;

import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * @author Léo Mieulet
 */
public class RequestDto {

    private Long id;

    private Set<String> errors;

    private OffsetDateTime creationDate;

    private OffsetDateTime remoteStepDeadline;

    private String sessionOwner;

    private String session;

    private String providerId;

    private String dtype;

    private InternalRequestState state;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<String> getErrors() {
        return errors;
    }

    public void setErrors(Set<String> errors) {
        this.errors = errors;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(OffsetDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public OffsetDateTime getRemoteStepDeadline() {
        return remoteStepDeadline;
    }

    public void setRemoteStepDeadline(OffsetDateTime remoteStepDeadline) {
        this.remoteStepDeadline = remoteStepDeadline;
    }

    public String getSessionOwner() {
        return sessionOwner;
    }

    public void setSessionOwner(String sessionOwner) {
        this.sessionOwner = sessionOwner;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getDtype() {
        return dtype;
    }

    public void setDtype(String dtype) {
        this.dtype = dtype;
    }

    public InternalRequestState getState() {
        return state;
    }

    public void setState(InternalRequestState state) {
        this.state = state;
    }
}
