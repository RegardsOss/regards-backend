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
package fr.cnes.regards.modules.ingest.domain.request.deletion;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.modules.ingest.domain.IngestValidationMessages;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestStep;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.NotBlank;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

/**
 * Storing info that a request have been sent to storage to remove files
 * To track answer about request status
 * @author Léo Mieulet
 */
@Entity(name = RequestTypeConstant.STORAGE_DELETION_VALUE)
public class StorageDeletionRequest extends AbstractRequest {

    /**
     * request configuration
     */
    @Column(columnDefinition = "jsonb", name = "payload")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private StorageDeletionPayload config;

    @NotBlank(message = IngestValidationMessages.MISSING_SESSION)
    @Column(length = 128, name = "sip_id", nullable = false)
    private String sipId;

    public String getSipId() {
        return sipId;
    }

    public void setSipId(String sipId) {
        this.sipId = sipId;
    }

    public StorageDeletionPayload getConfig() {
        return config;
    }

    public void setConfig(StorageDeletionPayload config) {
        this.config = config;
    }


    public SessionDeletionMode getDeletionMode() {
        return config.getDeletionMode();
    }

    public void setDeletionMode(SessionDeletionMode deletionMode) {
        config.setDeletionMode(deletionMode);
    }

    public static StorageDeletionRequest build(String requestId, SIPEntity sipEntity, SessionDeletionMode deletionMode) {
        StorageDeletionRequest sdr = new StorageDeletionRequest();
        sdr.setState(InternalRequestStep.RUNNING);
        sdr.setRemoteStepGroupIds(Lists.newArrayList(requestId));
        sdr.setSipId(sipEntity.getSipId());
        sdr.setSessionOwner(sipEntity.getIngestMetadata().getSessionOwner());
        sdr.setSession(sipEntity.getIngestMetadata().getSession());
        sdr.setProviderId(sipEntity.getProviderId());
        sdr.setConfig(StorageDeletionPayload.build(deletionMode));
        sdr.setCreationDate(OffsetDateTime.now());
        return sdr;
    }
}
