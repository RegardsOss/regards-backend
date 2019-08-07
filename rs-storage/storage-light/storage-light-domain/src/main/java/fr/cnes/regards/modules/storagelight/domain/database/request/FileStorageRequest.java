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
package fr.cnes.regards.modules.storagelight.domain.database.request;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.springframework.util.Assert;

import com.google.common.collect.Lists;

import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;

/**
 * @author Sébastien Binda
 *
 */
@Entity
@Table(name = "t_file_storage_request",
        indexes = { @Index(name = "idx_file_storage_request", columnList = "storage, checksum") },
        uniqueConstraints = { @UniqueConstraint(name = "t_file_storage_request_checksum_storage",
                columnNames = { "checksum", "storage" }) })
public class FileStorageRequest {

    /**
     * Internal database unique identifier
     */
    @Id
    @SequenceGenerator(name = "fileStorageRequestSequence", initialValue = 1, sequenceName = "seq_file_storage_request")
    @GeneratedValue(generator = "fileStorageRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "owner")
    @ElementCollection
    @CollectionTable(name = "ta_file_storage_request_owners", joinColumns = @JoinColumn(name = "file_ref_id",
            foreignKey = @ForeignKey(name = "fk_ta_file_storage_request_owners_t_file_storage_request")))
    private final List<String> owners = Lists.newArrayList();

    @Column(name = "origin_url", length = FileLocation.URL_MAX_LENGTH)
    private String originUrl;

    @Column(name = "storage_subdirectory", length = FileLocation.URL_MAX_LENGTH)
    private String storageSubDirectory;

    @Column(name = "storage", length = FileLocation.STORAGE_MAX_LENGTH)
    private String storage;

    @Embedded
    private FileReferenceMetaInfo metaInfo;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FileRequestStatus status = FileRequestStatus.TODO;

    @Column(name = "error_cause", length = 512)
    private String errorCause;

    public FileStorageRequest() {
        super();
    }

    public FileStorageRequest(String owner, FileReferenceMetaInfo metaInfos, URL originUrl, String storage,
            Optional<String> storageSubDirectory) {
        super();
        Assert.notNull(owner, "File storage request need a owner !");
        Assert.notNull(originUrl, "File storage request need an origin location !");
        Assert.notNull(storage, "File storage request need a destination location !");
        Assert.notNull(metaInfos, "File storage request need file meta information !");
        Assert.notNull(metaInfos.getChecksum(), "File storage request need file checkusm !");

        this.owners.add(owner);
        this.originUrl = originUrl.toString();
        this.storage = storage;
        this.storageSubDirectory = storageSubDirectory.orElse(null);
        this.metaInfo = metaInfos;
    }

    public FileStorageRequest(Collection<String> owners, FileReferenceMetaInfo metaInfos, URL originUrl, String storage,
            Optional<String> storageSubDirectory) {
        super();
        Assert.notNull(owners, "File storage request need a owner !");
        Assert.isTrue(!owners.isEmpty(), "File storage request need a owner !");
        Assert.notNull(originUrl, "File storage request need an origin location !");
        Assert.notNull(storage, "File storage request need a destination location !");
        Assert.notNull(metaInfos, "File storage request need file meta information !");
        Assert.notNull(metaInfos.getChecksum(), "File storage request need file checkusm !");

        this.owners.addAll(owners);
        this.originUrl = originUrl.toString();
        this.storage = storage;
        this.storageSubDirectory = storageSubDirectory.orElse(null);
        this.metaInfo = metaInfos;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<String> getOwners() {
        return owners;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public void setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
    }

    public FileReferenceMetaInfo getMetaInfo() {
        return metaInfo;
    }

    public void setMetaInfo(FileReferenceMetaInfo metaInfos) {
        this.metaInfo = metaInfos;
    }

    public FileRequestStatus getStatus() {
        return status;
    }

    public void setStatus(FileRequestStatus status) {
        this.status = status;
    }

    public String getErrorCause() {
        return errorCause;
    }

    public void setErrorCause(String errorCause) {
        this.errorCause = errorCause;
    }

    public String getStorageSubDirectory() {
        return storageSubDirectory;
    }

    public void setStorageSubDirectory(String storageSubDirectory) {
        this.storageSubDirectory = storageSubDirectory;
    }

    /**
     * @return the storage
     */
    public String getStorage() {
        return storage;
    }

    /**
     * @param storage the storage to set
     */
    public void setStorage(String storage) {
        this.storage = storage;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FileStorageRequest other = (FileStorageRequest) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

}
