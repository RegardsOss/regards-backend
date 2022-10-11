/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.dto.sip;

import fr.cnes.regards.modules.ingest.domain.IngestValidationMessages;
import fr.cnes.regards.modules.ingest.domain.sip.VersioningMode;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import org.springframework.util.Assert;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Extra information useful for SIP submission.<br/>
 * The ingest chain name is required and is linked to an existing one.<br/>
 * The sessionOwner and session allows to make consistent group of SIP.
 *
 * @author Marc Sordi
 * @author Léo Mieulet
 */
public class IngestMetadataDto {

    @NotBlank(message = IngestValidationMessages.MISSING_SESSION_OWNER)
    @Size(max = 128)
    private String sessionOwner;

    @NotBlank(message = IngestValidationMessages.MISSING_SESSION)
    @Size(max = 128)
    private String session;

    /**
     * {@link fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain} name
     */
    @NotBlank(message = IngestValidationMessages.MISSING_INGEST_CHAIN)
    @Size(max = 100)
    private String ingestChain;

    @Valid
    @NotNull(message = IngestValidationMessages.MISSING_STORAGE_METADATA)
    private List<StorageMetadata> storages;

    @Valid
    @NotNull(message = IngestValidationMessages.MISSING_CATEGORIES)
    private Set<String> categories;

    @NotNull(message = IngestValidationMessages.MISSING_VERSIONING_MODE)
    private VersioningMode versioningMode = VersioningMode.INC_VERSION;

    @Size(max = 128)
    private String model;

    /**
     * Build ingest metadata
     *
     * @param sessionOwner Owner of the session
     * @param session      session
     * @param ingestChain  ingest processing chain name
     * @param model        the model to be used for DescriptiveInformation validation
     * @param storages     storage metadata
     */
    public static IngestMetadataDto build(String sessionOwner,
                                          String session,
                                          String ingestChain,
                                          Set<String> categories,
                                          String model,
                                          StorageMetadata... storages) {
        return IngestMetadataDto.build(sessionOwner,
                                       session,
                                       ingestChain,
                                       categories,
                                       null,
                                       model,
                                       Arrays.asList(storages));
    }

    /**
     * Build ingest metadata
     *
     * @param sessionOwner   Owner of the session
     * @param session        session
     * @param ingestChain    ingest processing chain name
     * @param versioningMode
     * @param model          the model to be used for DescriptiveInformation validation
     * @param storages       storage metadata
     */
    public static IngestMetadataDto build(String sessionOwner,
                                          String session,
                                          String ingestChain,
                                          Set<String> categories,
                                          VersioningMode versioningMode,
                                          String model,
                                          List<StorageMetadata> storages) {
        Assert.hasLength(ingestChain, IngestValidationMessages.MISSING_INGEST_CHAIN);
        Assert.hasLength(sessionOwner, IngestValidationMessages.MISSING_SESSION_OWNER);
        Assert.hasLength(session, IngestValidationMessages.MISSING_SESSION);
        Assert.notNull(storages, IngestValidationMessages.MISSING_STORAGE_METADATA);
        IngestMetadataDto m = new IngestMetadataDto();
        m.setIngestChain(ingestChain);
        m.setSessionOwner(sessionOwner);
        m.setSession(session);
        m.setCategories(categories);
        m.setModel(model);
        m.setStorages(storages);
        m.setVersioningMode(versioningMode == null ? VersioningMode.INC_VERSION : versioningMode);
        return m;
    }

    public VersioningMode getVersioningMode() {
        return versioningMode;
    }

    public void setVersioningMode(VersioningMode versioningMode) {
        this.versioningMode = versioningMode;
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

    public String getIngestChain() {
        return ingestChain;
    }

    public void setIngestChain(String ingestChain) {
        this.ingestChain = ingestChain;
    }

    public List<StorageMetadata> getStorages() {
        return storages;
    }

    public void setStorages(List<StorageMetadata> storages) {
        this.storages = storages;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
