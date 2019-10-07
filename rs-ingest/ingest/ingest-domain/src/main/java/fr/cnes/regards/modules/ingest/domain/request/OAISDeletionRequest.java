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
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionSelectionMode;
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
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

/**
 * Macro request that keeps info about a "massive" suppression of OAIS entities
 * @author Marc SORDI
 */
@Entity
@Table(name = "t_deletion_request",
        indexes = { @Index(name = "idx_deletion_request_search", columnList = "session_owner,session_name,state") } )
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
public class OAISDeletionRequest extends AbstractInternalRequest {

    @Id
    @SequenceGenerator(name = "deletionRequestSequence", initialValue = 1, sequenceName = "seq_deletion_request")
    @GeneratedValue(generator = "deletionRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @NotNull(message = IngestValidationMessages.MISSING_SESSION_DELETION_MODE)
    @Column(name = "deletion_mode", nullable = false)
    private SessionDeletionMode deletionMode;

    @NotNull(message = IngestValidationMessages.MISSING_SESSION_DELETION_SELECTION_MODE)
    @Column(name = "selection_mode", nullable = false)
    private SessionDeletionSelectionMode selectionMode;

    /**
     * URN of the SIP(s) to preserve or remove in the specified session (according to {@link #selectionMode})
     */
    @Column(columnDefinition = "jsonb")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private Set<String> sipIds;

    /**
     * Provider id(s) of the SIP to preserve or remove in the specified session (according to {@link #selectionMode})
     */
    @Column(columnDefinition = "jsonb")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private Set<String> providerIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Set<String> getSipIds() {
        return sipIds;
    }

    public void setSipIds(Set<String> sipIds) {
        this.sipIds = sipIds;
    }

    public Set<String> getProviderIds() {
        return providerIds;
    }

    public void setProviderIds(Set<String> providerIds) {
        this.providerIds = providerIds;
    }

}
