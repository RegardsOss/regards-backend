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

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.util.Assert;

/**
 * Extra information for SIP submission.<br/>
 *
 * The ingest processing chain name is required and is linked to an existing processing chain.<br/>
 * The session identifier allows to make consistent group of SIP.
 *
 * @author Marc Sordi
 *
 */
public class IngestMetadata {

    private static final String INGEST_CHAIN_REQUIRED = "Ingest processing chain name is required";

    private static final String SESSION_CHAIN_REQUIRED = "Session is required";

    private static final String STORAGE_CHAIN_REQUIRED = "Storage metadata is required";

    /**
     * {@link fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain} name
     */
    @NotBlank(message = INGEST_CHAIN_REQUIRED)
    private String ingestChain;

    @NotBlank(message = SESSION_CHAIN_REQUIRED)
    private String session;

    @NotNull(message = STORAGE_CHAIN_REQUIRED)
    private StorageMetadata storages;

    public static IngestMetadata build(String ingestChain, String session, StorageMetadata storages) {
        Assert.hasLength(ingestChain, INGEST_CHAIN_REQUIRED);
        Assert.hasLength(session, SESSION_CHAIN_REQUIRED);
        Assert.notNull(storages, STORAGE_CHAIN_REQUIRED);
        IngestMetadata m = new IngestMetadata();
        m.setIngestChain(ingestChain);
        m.setSession(session);
        m.setStorages(storages);
        return m;
    }

    public String getIngestChain() {
        return ingestChain;
    }

    public void setIngestChain(String ingestChain) {
        this.ingestChain = ingestChain;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public StorageMetadata getStorages() {
        return storages;
    }

    public void setStorages(StorageMetadata storages) {
        this.storages = storages;
    }
}
