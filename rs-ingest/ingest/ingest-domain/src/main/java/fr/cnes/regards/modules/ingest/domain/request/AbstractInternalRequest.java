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
package fr.cnes.regards.modules.ingest.domain.request;


import fr.cnes.regards.modules.ingest.domain.IngestValidationMessages;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Common request properties for requests published outside of this microservice
 * @author Léo Mieulet
 */
@MappedSuperclass
public abstract class AbstractInternalRequest extends AbstractRequest {

    @NotBlank(message = IngestValidationMessages.MISSING_SESSION_OWNER)
    @Column(length = 128, name = "session_owner", nullable = false)
    private String sessionOwner;

    @NotBlank(message = IngestValidationMessages.MISSING_SESSION)
    @Column(length = 128, name = "session_name", nullable = false)
    private String session;

    @NotNull(message = "Request state is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 50, nullable = false)
    private InternalRequestStep state;

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

    public InternalRequestStep getState() {
        return state;
    }

    public void setState(InternalRequestStep state) {
        this.state = state;
    }
}
