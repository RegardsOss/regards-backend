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
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.event.FileRequestType;
import org.hibernate.annotations.Type;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.util.Collection;
import java.util.Set;

/**
 * @author sbinda
 */
@Entity
@Table(name = "t_request_result_info",
       indexes = { @Index(name = "idx_group_id", columnList = "group_id"),
                   @Index(name = "idx_group_file_ref_id", columnList = "result_file_ref_id") })
public class RequestResultInfo {

    @Id
    @SequenceGenerator(name = "groupRequestsInfoSequence", initialValue = 1, sequenceName = "seq_groups_requests_info")
    @GeneratedValue(generator = "groupRequestsInfoSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * Business identifier to regroup file requests.
     */
    @Column(name = "group_id", nullable = false, length = 128)
    private String groupId;

    /**
     * Request type
     */
    @Column(name = "request_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FileRequestType requestType;

    /**
     * File reference associated to the request
     */
    @OneToOne
    @JoinColumn(name = "result_file_ref_id", referencedColumnName = "id")
    private FileReference resultFile;

    @Column(name = "request_checksum", length = FileReferenceMetaInfo.CHECKSUM_MAX_LENGTH, nullable = false)
    private String requestChecksum;

    @Column(name = "request_storage", length = FileLocation.STORAGE_MAX_LENGTH)
    private String requestStorage;

    @Column(name = "request_store_path", length = FileLocation.URL_MAX_LENGTH)
    private String requestStorePath;

    @Column(columnDefinition = "jsonb", name = "request_owners")
    @Type(type = "jsonb")
    private final Set<String> requestOwners = Sets.newHashSet();

    @Column
    private boolean error;

    @Column(name = "error_cause", length = 512)
    private String errorCause;

    public RequestResultInfo() {
        super();
    }

    public RequestResultInfo(String groupId,
                             FileRequestType requestType,
                             String checksum,
                             String storage,
                             String storePath,
                             Collection<String> requestOwners) {
        Assert.notNull(groupId, "groupId can not be null");
        Assert.notNull(checksum, "checksum can not be null");
        Assert.notNull(requestType, "requestType can not be null");
        this.groupId = groupId;
        this.requestType = requestType;
        this.requestChecksum = checksum;
        this.requestStorage = storage;
        this.requestStorePath = storePath;
        if ((requestOwners != null) && !requestOwners.isEmpty()) {
            this.requestOwners.addAll(requestOwners);
        }
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public FileRequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(FileRequestType requestType) {
        this.requestType = requestType;
    }

    public FileReference getResultFile() {
        return resultFile;
    }

    public void setResultFile(FileReference fileReference) {
        this.resultFile = fileReference;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public Long getId() {
        return id;
    }

    public String getErrorCause() {
        return errorCause;
    }

    public void setErrorCause(String errorCause) {
        if (errorCause != null && errorCause.length() > 512) {
            this.errorCause = errorCause.substring(0, 511);
        } else {
            this.errorCause = errorCause;
        }
    }

    public String getRequestChecksum() {
        return requestChecksum;
    }

    public String getRequestStorage() {
        return requestStorage;
    }

    public String getRequestStorePath() {
        return requestStorePath;
    }

    public Set<String> getRequestOwners() {
        return requestOwners;
    }

}
