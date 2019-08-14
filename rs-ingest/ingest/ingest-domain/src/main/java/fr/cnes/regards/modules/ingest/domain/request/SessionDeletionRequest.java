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

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.modules.ingest.domain.IngestValidationMessages;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionSelectionMode;

/**
 * @author Marc SORDI
 */
@Entity
@Table(name = "t_deletion_request",
        indexes = { @Index(name = "idx_deletion_request_id", columnList = "request_id"),
                @Index(name = "idx_deletion_request_state", columnList = "state") },
        uniqueConstraints = { @UniqueConstraint(name = "uk_deletion_request_id", columnNames = { "request_id" }),
                @UniqueConstraint(name = "uk_deletion_request_by_session",
                        columnNames = { "session_owner", "session_name" }) })
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class SessionDeletionRequest extends AbstractRequest {

    @Id
    @SequenceGenerator(name = "deletionRequestSequence", initialValue = 1, sequenceName = "seq_deletion_request")
    @GeneratedValue(generator = "deletionRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * The SIP internal identifier (generated URN).
     */
    @Column(name = "sipId", length = SIPEntity.MAX_URN_SIZE, nullable = false)
    private String sipId;

    @NotBlank(message = IngestValidationMessages.MISSING_SESSION_OWNER)
    @Column(length = 128, name = "session_owner", nullable = false)
    private String sessionOwner;

    @NotBlank(message = IngestValidationMessages.MISSING_SESSION)
    @Column(length = 128, name = "session_name", nullable = false)
    private String session;

    @NotNull(message = IngestValidationMessages.MISSING_SESSION_DELETION_MODE)
    @Column(length = 20, name = "deletion_mode", nullable = false)
    private SessionDeletionMode deletionMode;

    @NotNull(message = IngestValidationMessages.MISSING_SESSION_DELETION_SELECTION_MODE)
    @Column(length = 20, name = "selection_mode", nullable = false)
    private SessionDeletionSelectionMode selectionMode;

    /**
     * URN of the SIP(s) to preserve or remove in the specified session (according to {@link #selectionMode})
     */
    @Column(columnDefinition = "jsonb")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private List<String> sipIds;

    /**
     * Provider id(s) of the SIP to preserve or remove in the specified session (according to {@link #selectionMode})
     */
    @Column(columnDefinition = "jsonb")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private List<String> providerIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

}
