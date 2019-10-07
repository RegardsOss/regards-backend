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
package fr.cnes.regards.modules.ingest.dto.request;

import fr.cnes.regards.modules.ingest.domain.request.InternalRequestStep;
import java.util.ArrayList;
import java.util.List;

import java.util.UUID;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.util.Assert;

import fr.cnes.regards.modules.ingest.domain.IngestValidationMessages;

/**
 * Session deletion request
 *
 * @author Marc SORDI
 */
public class OAISDeletionRequestDto {

    private String requestId;

    private InternalRequestStep state;

    @NotBlank(message = IngestValidationMessages.MISSING_SESSION_OWNER)
    private String sessionOwner;

    @NotBlank(message = IngestValidationMessages.MISSING_SESSION)
    private String session;

    @NotNull(message = IngestValidationMessages.MISSING_SESSION_DELETION_MODE)
    private SessionDeletionMode deletionMode;

    @NotNull(message = IngestValidationMessages.MISSING_SESSION_DELETION_SELECTION_MODE)
    private SessionDeletionSelectionMode selectionMode;

    /**
     * URN of the SIP(s) to preserve or remove in the specified session (according to {@link #selectionMode})
     */
    private List<String> sipIds;

    /**
     * Provider id(s) of the SIP to preserve or remove in the specified session (according to {@link #selectionMode})
     */
    private List<String> providerIds;

    /**
     * Build a new session deletion request
     */
    public static OAISDeletionRequestDto build(String sessionOwner, String session,
            SessionDeletionMode deletionMode, SessionDeletionSelectionMode selectionMode) {
        Assert.hasLength(sessionOwner, IngestValidationMessages.MISSING_SESSION_OWNER);
        Assert.hasLength(session, IngestValidationMessages.MISSING_SESSION);
        Assert.notNull(deletionMode, IngestValidationMessages.MISSING_SESSION_DELETION_MODE);
        Assert.notNull(selectionMode, IngestValidationMessages.MISSING_SESSION_DELETION_SELECTION_MODE);

        OAISDeletionRequestDto item = new OAISDeletionRequestDto();
        item.setRequestId(UUID.randomUUID().toString());
        item.setSessionOwner(sessionOwner);
        item.setSession(session);
        item.setDeletionMode(deletionMode);
        item.setSelectionMode(selectionMode);
        item.setSipIds(new ArrayList<>());
        item.setProviderIds(new ArrayList<>());
        return item;
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

    public SessionDeletionMode getDeletionMode() {
        return deletionMode;
    }

    public void setDeletionMode(SessionDeletionMode deletionMode) {
        this.deletionMode = deletionMode;
    }

    public SessionDeletionSelectionMode getSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(SessionDeletionSelectionMode selectionMode) {
        this.selectionMode = selectionMode;
    }

    public List<String> getSipIds() {
        return sipIds;
    }

    public void setSipIds(List<String> sipIds) {
        this.sipIds = sipIds;
    }

    public List<String> getProviderIds() {
        return providerIds;
    }

    public void setProviderIds(List<String> providerIds) {
        this.providerIds = providerIds;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public InternalRequestStep getState() {
        return state;
    }

    public void setState(InternalRequestStep state) {
        this.state = state;
    }
}
