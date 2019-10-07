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
package fr.cnes.regards.modules.ingest.domain.request;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.modules.ingest.domain.IngestValidationMessages;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.request.RequestState;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

/**
 * Storing info that a request have been sent to storage to remove files
 * To track answer about request status
 * @author Léo Mieulet
 */
@Entity
@Table(name = "t_deletion_storage_request",
        indexes = {@Index(name = "idx_deletion_storage_remote_step_group_id", columnList = "remote_step_group_id")},
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_deletion_storage_remote_step_group_id", columnNames = {"remote_step_group_id"})
        })
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
public class StorageDeletionRequest extends AbstractInternalRequest {

    @Id
    @SequenceGenerator(name = "storageDeletionRequestSequence", initialValue = 1, sequenceName = "seq_storage_deletion_request")
    @GeneratedValue(generator = "storageDeletionRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @NotBlank(message = IngestValidationMessages.MISSING_SESSION)
    @Column(length = 128, name = "sip_id", nullable = false)
    private String sipId;

    @NotNull(message = IngestValidationMessages.MISSING_SESSION_DELETION_MODE)
    @Column(length = 20, name = "deletion_mode", nullable = false)
    private SessionDeletionMode deletionMode;

    /**
     * Store a request id sent by the current request to propagate changes (on storage)
     */
    @Column(name = "remote_step_group_id", length = 36, nullable = false)
    private String remoteStepGroupId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSipId() {
        return sipId;
    }

    public void setSipId(String sipId) {
        this.sipId = sipId;
    }

    public SessionDeletionMode getDeletionMode() {
        return deletionMode;
    }

    public void setDeletionMode(SessionDeletionMode deletionMode) {
        this.deletionMode = deletionMode;
    }

    public String getRemoteStepGroupId() {
        return remoteStepGroupId;
    }

    public void setRemoteStepGroupId(String remoteStepGroupId) {
        this.remoteStepGroupId = remoteStepGroupId;
    }

    public static StorageDeletionRequest build(String requestId, SIPEntity sipEntity, SessionDeletionMode deletionMode) {
        StorageDeletionRequest sdr = new StorageDeletionRequest();
        sdr.setState(InternalRequestStep.RUNNING);
        sdr.setRemoteStepGroupId(requestId);
        sdr.setSipId(sipEntity.getSipId());
        sdr.setSessionOwner(sipEntity.getIngestMetadata().getSessionOwner());
        sdr.setSession(sipEntity.getIngestMetadata().getSession());
        sdr.setDeletionMode(deletionMode);
        return sdr;
    }
}
