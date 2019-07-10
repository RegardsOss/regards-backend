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
package fr.cnes.regards.modules.storagelight.domain.database;

import java.util.Collection;
import java.util.List;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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

import fr.cnes.regards.modules.storagelight.domain.FileReferenceRequestStatus;

/**
 * @author Sébastien Binda
 *
 */
@Entity
@Table(name = "t_file_reference_request",
        indexes = { @Index(name = "idx_file_reference_request", columnList = "destination_storage, checksum") },
        uniqueConstraints = { @UniqueConstraint(columnNames = { "checksum", "destination_storage" }) })
public class FileReferenceRequest {

    /**
     * Internal database unique identifier
     */
    @Id
    @SequenceGenerator(name = "fileReferenceRequestSequence", initialValue = 0,
            sequenceName = "seq_file_reference_request")
    @GeneratedValue(generator = "fileReferenceRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "owner")
    @ElementCollection
    @CollectionTable(name = "file_ref_owners", joinColumns = @JoinColumn(name = "file_ref_id"))
    private final List<String> owners = Lists.newArrayList();

    @Embedded
    @AttributeOverrides({ @AttributeOverride(name = "storage", column = @Column(name = "origin_storage")),
            @AttributeOverride(name = "url", column = @Column(name = "origin_url")) })
    private FileLocation origin;

    @Embedded
    @AttributeOverrides({ @AttributeOverride(name = "storage", column = @Column(name = "destination_storage")),
            @AttributeOverride(name = "url", column = @Column(name = "destination_url")) })
    private FileLocation destination;

    @Embedded
    private FileReferenceMetaInfo metaInfo;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FileReferenceRequestStatus status = FileReferenceRequestStatus.TO_STORE;

    @Column(length = 512)
    private String errorCause;

    public FileReferenceRequest() {
        super();
    }

    public FileReferenceRequest(String owner, FileReferenceMetaInfo metaInfos, FileLocation origin,
            FileLocation destination) {
        super();
        Assert.notNull(owner, "File reference request need a owner !");
        Assert.notNull(origin, "File reference request need an origin location !");
        Assert.notNull(destination, "File reference request need a destination location !");
        Assert.notNull(metaInfos, "File reference request need file meta information !");
        Assert.notNull(metaInfos.getChecksum(), "File reference request need file checkusm !");

        this.owners.add(owner);
        this.origin = origin;
        this.destination = destination;
        this.metaInfo = metaInfos;
    }

    public FileReferenceRequest(Collection<String> owners, FileReferenceMetaInfo metaInfos, FileLocation origin,
            FileLocation destination) {
        super();
        Assert.notNull(owners, "File reference request need a owner !");
        Assert.isTrue(!owners.isEmpty(), "File reference request need a owner !");
        Assert.notNull(origin, "File reference request need an origin location !");
        Assert.notNull(destination, "File reference request need a destination location !");
        Assert.notNull(metaInfos, "File reference request need file meta information !");
        Assert.notNull(metaInfos.getChecksum(), "File reference request need file checkusm !");

        this.owners.addAll(owners);
        this.origin = origin;
        this.destination = destination;
        this.metaInfo = metaInfos;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the owners
     */
    public List<String> getOwners() {
        return owners;
    }

    /**
     * @return the origin
     */
    public FileLocation getOrigin() {
        return origin;
    }

    /**
     * @param origin the origin to set
     */
    public void setOrigin(FileLocation origin) {
        this.origin = origin;
    }

    /**
     * @return the destination
     */
    public FileLocation getDestination() {
        return destination;
    }

    /**
     * @param destination the destination to set
     */
    public void setDestination(FileLocation destination) {
        this.destination = destination;
    }

    /**
     * @return the metaInfos
     */
    public FileReferenceMetaInfo getMetaInfo() {
        return metaInfo;
    }

    /**
     * @param metaInfos the metaInfos to set
     */
    public void setMetaInfo(FileReferenceMetaInfo metaInfos) {
        this.metaInfo = metaInfos;
    }

    /**
     * @return the state
     */
    public FileReferenceRequestStatus getStatus() {
        return status;
    }

    /**
     * @param state the state to set
     */
    public void setStatus(FileReferenceRequestStatus status) {
        this.status = status;
    }

    /**
     * @return the errorCause
     */
    public String getErrorCause() {
        return errorCause;
    }

    /**
     * @param errorCause the errorCause to set
     */
    public void setErrorCause(String errorCause) {
        this.errorCause = errorCause;
    }

    /**
     * Indicates if the current request need to store the reference file.
     * The storage needs to be done if the origin and the destination of the request are different
     * @return
     */
    public boolean needFileStorage() {
        return (this.destination != null) ? this.destination.equals(this.origin) : false;
    }

}
