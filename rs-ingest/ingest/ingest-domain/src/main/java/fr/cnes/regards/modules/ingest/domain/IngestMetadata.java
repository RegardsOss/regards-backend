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
package fr.cnes.regards.modules.ingest.domain;

import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.modules.ingest.domain.aip.StorageMetadata;

/**
 * Extra information useful for SIP submission.<br/>
 * The processing chain name is required and is linked to an existing processing chain.<br/>
 * The sessionSource and sessionName allows to make consistent group of SIP.
 *
 * @author Marc Sordi
 * @author Léo Mieulet
 *
 */
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
@Embeddable
public class IngestMetadata {

    private static final String MISSING_INGEST_CHAIN_ERROR = "Ingest processing chain name is required";

    private static final String MISSING_CLIENT_ID_ERROR = "Identifier of the client who operates the SIP submission is required";

    private static final String MISSING_CLIENT_SESSION_ERROR = "Client session is required";

    private static final String MISSING_STORAGE_METADATA_ERROR = "Storage metadata is required";

    @NotBlank(message = MISSING_CLIENT_ID_ERROR)
    @Column(length = 128, name = "client_id", nullable = false)
    private String clientId;

    @NotBlank(message = MISSING_CLIENT_SESSION_ERROR)
    @Column(length = 128, name = "client_session", nullable = false)
    private String clientSession;

    /**
     * {@link fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain} name
     */
    @NotBlank(message = MISSING_INGEST_CHAIN_ERROR)
    @Column(length = 100, name = "ingest_chain", nullable = false)
    private String ingestChain;

    @Valid
    @NotNull(message = MISSING_STORAGE_METADATA_ERROR)
    @Column(columnDefinition = "jsonb")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE,
            value = "fr.cnes.regards.modules.ingest.domain.StorageMetadata") })
    private List<StorageMetadata> storages;

    public String getIngestChain() {
        return ingestChain;
    }

    public void setIngestChain(String ingestChain) {
        this.ingestChain = ingestChain;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSession() {
        return clientSession;
    }

    public void setClientSession(String clientSession) {
        this.clientSession = clientSession;
    }

    public List<StorageMetadata> getStorages() {
        return storages;
    }

    public void setStorages(List<StorageMetadata> storages) {
        this.storages = storages;
    }

    /**
     * Build ingest metadata
     * @param clientId client identifier
     * @param clientSession client session
     * @param ingestChain ingest processing chain name
     * @param storages storage metadata
     */
    public static IngestMetadata build(String clientId, String clientSession, String ingestChain,
            StorageMetadata... storages) {
        Assert.hasLength(ingestChain, MISSING_INGEST_CHAIN_ERROR);
        Assert.hasLength(clientId, MISSING_CLIENT_ID_ERROR);
        Assert.hasLength(clientSession, MISSING_CLIENT_SESSION_ERROR);
        Assert.notEmpty(storages, MISSING_STORAGE_METADATA_ERROR);
        IngestMetadata m = new IngestMetadata();
        m.setIngestChain(ingestChain);
        m.setClientId(clientId);
        m.setClientSession(clientSession);
        m.setStorages(Arrays.asList(storages));
        return m;
    }
}
