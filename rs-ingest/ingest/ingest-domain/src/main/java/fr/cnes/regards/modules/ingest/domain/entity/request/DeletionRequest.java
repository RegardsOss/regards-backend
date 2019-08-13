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
package fr.cnes.regards.modules.ingest.domain.entity.request;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.springframework.lang.Nullable;

import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;

/**
 * @author Marc SORDI
 */
@Entity
@Table(name = "t_deletion_request",
        indexes = { @Index(name = "idx_deletion_request_id", columnList = "request_id"),
                @Index(name = "idx_deletion_request_state", columnList = "state") },
        uniqueConstraints = { @UniqueConstraint(name = "uk_deletion_request_id", columnNames = { "request_id" }) })
public class DeletionRequest extends AbstractRequest {

    @Id
    @SequenceGenerator(name = "deletionRequestSequence", initialValue = 1, sequenceName = "seq_deletion_request")
    @GeneratedValue(generator = "deletionRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * The SIP internal identifier (generated URN).
     */
    @Column(name = "sipId", length = SIPEntity.MAX_URN_SIZE, nullable = false)
    private String sipId;

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

    public static DeletionRequest build(String requestId, RequestState state, String sipId) {
        return build(requestId, state, sipId, null);
    }

    public static DeletionRequest build(String requestId, RequestState state, String sipId,
            @Nullable Set<String> errors) {
        DeletionRequest request = new DeletionRequest();
        request.setRequestId(requestId);
        request.setSipId(sipId);
        request.setErrors(errors);
        return request;
    }

}
