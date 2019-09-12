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
import fr.cnes.regards.modules.ingest.domain.IngestValidationMessages;
import fr.cnes.regards.modules.ingest.dto.request.RequestState;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
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
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

/**
 * Entity storing info that a request have been sent to storage to remove files
 * To track answer about request status
 * @author Léo Mieulet
 */
@Entity
@Table(name = "t_deletion_storage_request",
        indexes = {@Index(name = "idx_deletion_storage_request_id", columnList = "request_id"),
                @Index(name = "idx_deletion_storage_request_state", columnList = "state")},
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_deletion_storage_request_id", columnNames = {"request_id"})
        })
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
public class StorageDeletionRequest extends AbstractRequest {

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

    public static StorageDeletionRequest build(String requestId, String sipId, SessionDeletionMode deletionMode) {
        StorageDeletionRequest sdr = new StorageDeletionRequest();
        sdr.setRequestId(requestId);
        sdr.setSipId(sipId);
        sdr.setDeletionMode(deletionMode);
        sdr.setState(RequestState.GRANTED);
        return sdr;
    }
}
