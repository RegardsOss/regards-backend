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
package fr.cnes.regards.modules.file.packager.domain;

import fr.cnes.regards.framework.module.manager.ConfigIgnore;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entity that represent a package.
 * A package is associated to multiple {@link FileInBuildingPackage} and will be used to create an archive containing
 * all the files.
 * The {@link #status} attribute inform on the current step of the process :
 * {@link PackageReferenceStatus#BUILDING BUILDING} ->
 * {@link PackageReferenceStatus#TO_STORE TO_STORE} ->
 * {@link PackageReferenceStatus#STORE_IN_PROGRESS} ->
 * {@link PackageReferenceStatus#STORED}
 * <p/>
 * Once stored, the entity will not be deleted (unless the files inside are deleted) as it will be used to retrieve
 * the archive during the restoration process.
 *
 * @author Thibaud Michaudel
 **/
@Entity
@Table(name = "t_package_reference")
public class PackageReference {

    @Id
    @SequenceGenerator(name = "packageReferenceSequence", initialValue = 1, sequenceName = "seq_package_reference")
    @GeneratedValue(generator = "packageReferenceSequence", strategy = GenerationType.SEQUENCE)
    @ConfigIgnore
    private Long id;

    @Column(name = "storage_subidrectory", nullable = false)
    private String storageSubdirectory;

    @Column(name = "creation_date", nullable = false)
    private OffsetDateTime creationDate;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PackageReferenceStatus status;

    @Column(name = "checksum")
    @Nullable
    private String checksum;

    @Column(name = "error_cause")
    @Nullable
    private String errorCause;

    @Column(name = "storage", nullable = false)
    private String storage;

    @Column(name = "size", nullable = false)
    private Long size;

    public PackageReference(String storage, String storageSubdirectory) {
        this.storageSubdirectory = storageSubdirectory;
        this.storage = storage;
        this.creationDate = OffsetDateTime.now();
        this.status = PackageReferenceStatus.BUILDING;
        size = 0L;
    }

    public PackageReference() {

    }

    public void addFileSize(Long sizeToAdd) {
        size += sizeToAdd;
    }

    public void setStatus(PackageReferenceStatus status) {
        this.status = status;
    }

    public void setChecksum(String storeCorrelationId) {
        this.checksum = storeCorrelationId;
    }

    public void setErrorCause(String errorCause) {
        this.errorCause = errorCause;
    }

    public Long getId() {
        return id;
    }

    public String getStorageSubdirectory() {
        return storageSubdirectory;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public PackageReferenceStatus getStatus() {
        return status;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getErrorCause() {
        return errorCause;
    }

    public String getStorage() {
        return storage;
    }

    public Long getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "PackageReference{"
               + "id="
               + id
               + ", storageSubdirectory='"
               + storageSubdirectory
               + '\''
               + ", creationDate="
               + creationDate
               + ", status="
               + status
               + ", checksum='"
               + checksum
               + '\''
               + ", errorCause='"
               + errorCause
               + '\''
               + ", storage='"
               + storage
               + '\''
               + ", size="
               + size
               + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PackageReference that = (PackageReference) o;
        return Objects.equals(id, that.id)
               && Objects.equals(storageSubdirectory, that.storageSubdirectory)
               && Objects.equals(creationDate, that.creationDate)
               && status == that.status
               && Objects.equals(checksum, that.checksum)
               && Objects.equals(errorCause, that.errorCause)
               && Objects.equals(storage, that.storage)
               && Objects.equals(size, that.size);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, storageSubdirectory, creationDate, status, checksum, errorCause, storage, size);
    }
}
