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
package fr.cnes.regards.modules.ingest.domain.sip;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.springframework.util.Assert;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.modules.ingest.domain.IngestValidationMessages;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;

/**
 * Extra information useful for SIP submission.<br/>
 * The processing chain name is required and is linked to an existing processing chain.<br/>
 * The sessionOwner and session allows to make consistent group of SIP.
 *
 * @author Marc Sordi
 * @author Léo Mieulet
 *
 */
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
@Embeddable
public class IngestMetadata {

    @NotBlank(message = IngestValidationMessages.MISSING_SESSION_OWNER)
    @Column(length = 128, name = "session_owner", nullable = false)
    private String sessionOwner;

    @NotBlank(message = IngestValidationMessages.MISSING_SESSION)
    @Column(length = 128, name = "session_name", nullable = false)
    private String session;

    /**
     * {@link fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain} name
     */
    @NotBlank(message = IngestValidationMessages.MISSING_INGEST_CHAIN)
    @Column(length = 100, name = "ingest_chain", nullable = false)
    private String ingestChain;

    @Valid
    @NotNull(message = IngestValidationMessages.MISSING_STORAGE_METADATA)
    @Column(columnDefinition = "jsonb")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE,
            value = "fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata") })
    private Set<StorageMetadata> storages;

    @NotNull(message = IngestValidationMessages.MISSING_VERSIONING_MODE)
    @Column(name = "versioning_mode")
    @Enumerated(EnumType.STRING)
    private VersioningMode versioningMode = VersioningMode.INC_VERSION;

    @Column(columnDefinition = "jsonb", nullable = false)
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private Set<String> categories;

    public VersioningMode getVersioningMode() {
        return versioningMode;
    }

    public void setVersioningMode(VersioningMode versioningMode) {
        this.versioningMode = versioningMode;
    }

    public String getIngestChain() {
        return ingestChain;
    }

    public void setIngestChain(String ingestChain) {
        this.ingestChain = ingestChain;
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

    public Set<StorageMetadata> getStorages() {
        return storages;
    }

    public void setStorages(Set<StorageMetadata> storages) {
        this.storages = storages;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    /**
     * Build ingest metadata with default versioning mode: {@link VersioningMode#INC_VERSION}
     * @param sessionOwner Owner of the session
     * @param session session
     * @param categories category list
     * @param ingestChain ingest processing chain name
     * @param storages storage metadata
     */
    public static IngestMetadata build(String sessionOwner, String session, String ingestChain, Set<String> categories,
            StorageMetadata... storages) {
        return build(sessionOwner, session, ingestChain, categories, VersioningMode.INC_VERSION, storages);
    }

    /**
     * Build ingest metadata
     * @param sessionOwner Owner of the session
     * @param session session
     * @param categories category list
     * @param ingestChain ingest processing chain name
     * @param versioningMode versioning mode
     * @param storages storage metadata
     */
    public static IngestMetadata build(String sessionOwner, String session, String ingestChain, Set<String> categories,
            VersioningMode versioningMode, StorageMetadata... storages) {
        Assert.hasLength(ingestChain, IngestValidationMessages.MISSING_INGEST_CHAIN);
        Assert.hasLength(sessionOwner, IngestValidationMessages.MISSING_SESSION_OWNER);
        Assert.hasLength(session, IngestValidationMessages.MISSING_SESSION);
        Assert.notEmpty(storages, IngestValidationMessages.MISSING_STORAGE_METADATA);
        Assert.notNull(versioningMode, IngestValidationMessages.MISSING_VERSIONING_MODE);
        IngestMetadata m = new IngestMetadata();
        m.setIngestChain(ingestChain);
        m.setSessionOwner(sessionOwner);
        m.setSession(session);
        m.setCategories(categories);
        m.setStorages(Sets.newHashSet(storages));
        m.setVersioningMode(versioningMode);
        return m;
    }
}
