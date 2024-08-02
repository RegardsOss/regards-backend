/*

 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Useful information for SIP submission
 *
 * @author mnguyen0
 * @author Marc Sodi
 * @author Léo Mieulet
 */
public class IngestMetadataDto {

    private String sessionOwner;

    private String session;

    private String ingestChain;

    private VersioningMode versioningMode = VersioningMode.INC_VERSION;

    private List<StorageDto> storages;

    private OffsetDateTime submissionDate;

    private Set<String> categories;

    private Boolean replaceErrors = Boolean.FALSE;

    @Size(max = 128)
    private String model;

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

    public VersioningMode getVersioningMode() {
        return versioningMode;
    }

    public void setVersioningMode(VersioningMode versioningMode) {
        this.versioningMode = versioningMode;
    }

    public List<StorageDto> getStorages() {
        return storages;
    }

    public void setStorages(List<StorageDto> storages) {
        this.storages = storages;
    }

    public OffsetDateTime getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(OffsetDateTime submissionDate) {
        this.submissionDate = submissionDate;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    public Boolean getReplaceErrors() {
        return replaceErrors;
    }

    public void setReplaceErrors(Boolean replaceErrors) {
        this.replaceErrors = replaceErrors;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public IngestMetadataDto() {

    }

    public IngestMetadataDto(String sessionOwner,
                             String session,
                             @Nullable OffsetDateTime submissionDate,
                             String ingestChain,
                             Set<String> categories,
                             @Nullable VersioningMode versioningMode,
                             @Nullable String model,
                             StorageDto storages) {
        this(sessionOwner,
             session,
             submissionDate,
             ingestChain,
             categories,
             versioningMode,
             model,
             Arrays.asList(storages));
    }

    public IngestMetadataDto(String sessionOwner,
                             String session,
                             @Nullable OffsetDateTime submissionDate,
                             String ingestChain,
                             Set<String> categories,
                             @Nullable VersioningMode versioningMode,
                             @Nullable String model,
                             List<StorageDto> storages) {
        this.sessionOwner = sessionOwner;
        this.session = session;
        this.ingestChain = ingestChain;
        this.versioningMode = versioningMode;
        this.storages = storages;
        this.submissionDate = submissionDate;
        this.categories = categories;
        this.model = model;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IngestMetadataDto that = (IngestMetadataDto) o;
        return Objects.equals(sessionOwner, that.sessionOwner)
               && Objects.equals(session, that.session)
               && Objects.equals(ingestChain, that.ingestChain)
               && Objects.equals(versioningMode, that.versioningMode)
               && Objects.equals(storages, that.storages)
               && Objects.equals(submissionDate, that.submissionDate)
               && Objects.equals(categories, that.categories)
               && Objects.equals(replaceErrors, that.replaceErrors)
               && Objects.equals(model, that.model);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionOwner,
                            session,
                            ingestChain,
                            versioningMode,
                            storages,
                            submissionDate,
                            categories,
                            replaceErrors,
                            model);
    }

    @Override
    public String toString() {
        return "IngestMetadataDto{"
               + "sessionOwner='"
               + sessionOwner
               + '\''
               + ", session='"
               + session
               + '\''
               + ", ingestChain='"
               + ingestChain
               + '\''
               + ", versioningMode='"
               + versioningMode
               + '\''
               + ", storages="
               + storages
               + ", submissionDate="
               + submissionDate
               + ", categories="
               + categories
               + ", replaceErrors="
               + replaceErrors
               + ", model='"
               + model
               + '\''
               + '}';
    }
}
