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
package fr.cnes.regards.modules.fileaccess.dto.request;

import fr.cnes.regards.modules.fileaccess.dto.FileReferenceMetaInfoDto;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Information about a file for a store request.<br/>
 * Mandatory information are : <ul>
 * <li> metaInfo.Filename</li>
 * <li> metaInfo.Checksum</li>
 * <li> metaInfo.Checksum algorithm </li>
 * <li> metaInfo.mimeType </li>
 * <li> Storage location where to delete the file</li>
 * <li> Owners referencing the file </li>
 * <li> originUrl where to access file to store. Must be locally accessible (file protocol for example) </li>
 * </ul>
 * See FilesStorageRequestEvent for more information about storage request process.
 *
 * @author Thibaud Michaudel
 */
public class FileStorageRequestAggregationDto {

    private final Long id;

    private final Set<String> owners = new HashSet<>();

    private final String originUrl;

    private final String storage;

    private final FileReferenceMetaInfoDto metaInfo;

    private final String subDirectory;

    private final String sessionOwner;

    private final String session;

    private final String jobId;

    private final String errorCause;

    private final FileRequestStatus status;

    private final OffsetDateTime creationDate;

    private final Set<String> groupIds = new HashSet<>();

    public FileStorageRequestAggregationDto(Long id,
                                            Set<String> owners,
                                            String originUrl,
                                            String storage,
                                            FileReferenceMetaInfoDto metaInfo,
                                            String subDirectory,
                                            String sessionOwner,
                                            String session,
                                            String jobId,
                                            String errorCause,
                                            FileRequestStatus status,
                                            OffsetDateTime creationDate,
                                            Set<String> groupIds) {
        this.id = id;
        if (owners != null) {
            this.owners.addAll(owners);
        }
        this.originUrl = originUrl;
        this.storage = storage;
        this.metaInfo = metaInfo;
        this.subDirectory = subDirectory;
        this.sessionOwner = sessionOwner;
        this.session = session;
        this.jobId = jobId;
        this.errorCause = errorCause;
        this.status = status;
        this.creationDate = creationDate;
        if (groupIds != null) {
            this.groupIds.addAll(groupIds);
        }

    }

    public Long getId() {
        return id;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public String getStorage() {
        return storage;
    }

    public FileReferenceMetaInfoDto getMetaInfo() {
        return metaInfo;
    }

    public String getSubDirectory() {
        return subDirectory;
    }

    public String getSessionOwner() {
        return sessionOwner;
    }

    public String getSession() {
        return session;
    }

    public Set<String> getOwners() {
        return owners;
    }

    public String getJobId() {
        return jobId;
    }

    public String getErrorCause() {
        return errorCause;
    }

    public FileRequestStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public Set<String> getGroupIds() {
        return groupIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FileStorageRequestAggregationDto that = (FileStorageRequestAggregationDto) o;

        if (!Objects.equals(id, that.id)) {
            return false;
        }
        if (owners != null ? !owners.equals(that.owners) : that.owners != null) {
            return false;
        }
        if (!Objects.equals(originUrl, that.originUrl)) {
            return false;
        }
        if (!Objects.equals(storage, that.storage)) {
            return false;
        }
        if (!Objects.equals(metaInfo, that.metaInfo)) {
            return false;
        }
        if (!Objects.equals(subDirectory, that.subDirectory)) {
            return false;
        }
        if (!Objects.equals(sessionOwner, that.sessionOwner)) {
            return false;
        }
        if (!Objects.equals(session, that.session)) {
            return false;
        }
        if (!Objects.equals(jobId, that.jobId)) {
            return false;
        }
        if (!Objects.equals(errorCause, that.errorCause)) {
            return false;
        }
        if (status != that.status) {
            return false;
        }
        if (!Objects.equals(creationDate, that.creationDate)) {
            return false;
        }
        return groupIds != null ? groupIds.equals(that.groupIds) : that.groupIds == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (owners != null ? owners.hashCode() : 0);
        result = 31 * result + (originUrl != null ? originUrl.hashCode() : 0);
        result = 31 * result + (storage != null ? storage.hashCode() : 0);
        result = 31 * result + (metaInfo != null ? metaInfo.hashCode() : 0);
        result = 31 * result + (subDirectory != null ? subDirectory.hashCode() : 0);
        result = 31 * result + (sessionOwner != null ? sessionOwner.hashCode() : 0);
        result = 31 * result + (session != null ? session.hashCode() : 0);
        result = 31 * result + (jobId != null ? jobId.hashCode() : 0);
        result = 31 * result + (errorCause != null ? errorCause.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (creationDate != null ? creationDate.hashCode() : 0);
        result = 31 * result + (groupIds != null ? groupIds.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FileStorageRequestAggregationDto{"
               + "id="
               + id
               + ", owners="
               + owners
               + ", originUrl='"
               + originUrl
               + '\''
               + ", storage='"
               + storage
               + '\''
               + ", metaInfo="
               + metaInfo
               + ", subDirectory='"
               + subDirectory
               + '\''
               + ", sessionOwner='"
               + sessionOwner
               + '\''
               + ", session='"
               + session
               + '\''
               + ", jobId='"
               + jobId
               + '\''
               + ", errorCause='"
               + errorCause
               + '\''
               + ", status="
               + status
               + ", creationDate="
               + creationDate
               + ", groupIds="
               + groupIds
               + '}';
    }
}
