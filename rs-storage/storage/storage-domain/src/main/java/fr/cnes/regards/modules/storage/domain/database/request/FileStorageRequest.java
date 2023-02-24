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
package fr.cnes.regards.modules.storage.domain.database.request;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.dto.request.FileStorageRequestDTO;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Database definition of the table containing the requests to store files.
 *
 * @author Sébastien Binda
 */
@Entity
@Table(name = "t_file_storage_request",
       indexes = { @Index(name = "idx_file_storage_request", columnList = "storage, checksum"),
                   @Index(name = "idx_file_storage_request_cs", columnList = "checksum"),
                   @Index(name = "idx_file_storage_request_storage", columnList = "storage") })
public class FileStorageRequest {

    public static final String FILE_STORAGE_REQUEST_NEED_A_OWNER = "File storage request need a owner !";

    /**
     * Internal database unique identifier
     */
    @Id
    @SequenceGenerator(name = "fileStorageRequestSequence", initialValue = 1, sequenceName = "seq_file_storage_request")
    @GeneratedValue(generator = "fileStorageRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * Business identifier to regroup file requests.
     */
    @Column(name = "group_id", nullable = false, length = 128)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ta_storage_request_group_ids",
                     joinColumns = @JoinColumn(name = "file_storage_request_id",
                                               foreignKey = @ForeignKey(name = "fk_ta_storage_request_group_ids_t_file_storage_request")))
    private final Set<String> groupIds = Sets.newHashSet();

    @Column(name = "owner")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ta_file_storage_request_owners",
                     joinColumns = @JoinColumn(name = "file_storage_request_id",
                                               foreignKey = @ForeignKey(name = "fk_ta_file_storage_request_owners_t_file_storage_request")))
    private final Set<String> owners = Sets.newHashSet();

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
    private FileRequestStatus status = FileRequestStatus.TO_DO;

    @Column(name = "error_cause", length = 512)
    private String errorCause;

    @Column(name = "creation_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private final OffsetDateTime creationDate;

    @Column(name = "job_id")
    private String jobId;

    @Column(name = "session_owner")
    private String sessionOwner;

    @Column(name = "session_name")
    private String session;

    public FileStorageRequest() {
        super();
        this.creationDate = OffsetDateTime.now();
    }

    public FileStorageRequest(String owner,
                              FileReferenceMetaInfo metaInfos,
                              String originUrl,
                              String storage,
                              Optional<String> storageSubDirectory,
                              String groupId,
                              String sessionOwner,
                              String session) {
        super();
        Assert.notNull(owner, FILE_STORAGE_REQUEST_NEED_A_OWNER);
        Assert.notNull(originUrl, "File storage request need an origin location !");
        Assert.notNull(storage, "File storage request need a destination location !");
        Assert.notNull(metaInfos, "File storage request need file meta information !");
        Assert.notNull(metaInfos.getChecksum(), "File storage request need file checkusm !");
        Assert.notNull(groupId, "Group id is mandatory");

        this.owners.add(owner);
        this.originUrl = originUrl;
        this.storage = storage;
        if (storageSubDirectory != null) {
            this.storageSubDirectory = storageSubDirectory.orElse(null);
        }
        this.metaInfo = metaInfos;
        this.groupIds.add(groupId);
        this.creationDate = OffsetDateTime.now();
        this.sessionOwner = sessionOwner;
        this.session = session;
    }

    public FileStorageRequest(Collection<String> owners,
                              FileReferenceMetaInfo metaInfos,
                              String originUrl,
                              String storage,
                              Optional<String> storageSubDirectory,
                              String groupId,
                              String sessionOwner,
                              String session) {
        super();
        Assert.notNull(owners, FILE_STORAGE_REQUEST_NEED_A_OWNER);
        Assert.isTrue(!owners.isEmpty(), FILE_STORAGE_REQUEST_NEED_A_OWNER);
        Assert.notNull(originUrl, "File storage request need an origin location !");
        Assert.notNull(storage, "File storage request need a destination location !");
        Assert.notNull(metaInfos, "File storage request need file meta information !");
        Assert.notNull(metaInfos.getChecksum(), "File storage request need file checkusm !");
        Assert.notNull(groupId, "GroupId is mandatory");

        this.owners.addAll(owners);
        this.originUrl = originUrl;
        this.storage = storage;
        if (storageSubDirectory != null) {
            this.storageSubDirectory = storageSubDirectory.orElse(null);
        }
        this.metaInfo = metaInfos;
        this.groupIds.add(groupId);
        this.creationDate = OffsetDateTime.now();
        this.sessionOwner = sessionOwner;
        this.session = session;
    }

    /**
     * Update an existing request from a new received request.
     */
    public void update(FileStorageRequestDTO request, String groupId) {
        if (!this.owners.contains(request.getOwner())) {
            this.owners.add(request.getOwner());
        }
        if (!this.groupIds.contains(groupId)) {
            this.groupIds.add(groupId);
        }
        this.storageSubDirectory = request.getSubDirectory();
        this.originUrl = request.getOriginUrl();
        if (this.metaInfo != null) {
            this.metaInfo.setFileName(request.getFileName());
            this.metaInfo.setType(request.getType());
        }
        this.session = request.getSession();
        this.sessionOwner = request.getSessionOwner();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<String> getOwners() {
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

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public Set<String> getGroupIds() {
        return groupIds;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
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
