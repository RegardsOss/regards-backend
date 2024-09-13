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

import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.framework.module.manager.ConfigIgnore;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entity that represent a file that will be packaged.
 * The {@link #status} attribute inform on the current step of the process :
 * {@link FileInBuildingPackageStatus#WAITING_PACKAGE WAITING_PACKAGE} ->
 * {@link FileInBuildingPackageStatus#BUILDING BUILDING} ->
 * {@link FileInBuildingPackageStatus#TO_LOCAL_DELETE TO_LOCAL_DELETE} ->
 * {@link FileInBuildingPackageStatus#DELETING DELETING}
 *
 * @author Thibaud Michaudel
 **/
@Entity
@Table(name = "t_file_in_building_package",
       uniqueConstraints = { @UniqueConstraint(name = "uk_file_building_storage_checksum",
                                               columnNames = { "storage", "checksum" }) })
public class FileInBuildingPackage {

    @Id
    @SequenceGenerator(name = "fileInBuildingPackageSequence",
                       initialValue = 1,
                       sequenceName = "seq_file_in_building_package")
    @GeneratedValue(generator = "fileInBuildingPackageSequence", strategy = GenerationType.SEQUENCE)
    @ConfigIgnore
    private Long id;

    @Column(name = "storage_request_id")
    private Long storageRequestId;

    private String storage;

    private String checksum;

    private String filename;

    /**
     * The {@link PackageReference} containing the file.
     * null if the file is not yet associated to a package.
     */
    @GsonIgnore
    @ManyToOne
    @JoinColumn(name = "package_id", foreignKey = @ForeignKey(name = "fk_acq_file_id"), updatable = false)
    private PackageReference packageReference;

    private FileInBuildingPackageStatus status;

    @Column(name = "last_update_date")
    private OffsetDateTime lastUpdateDate;

    @Column(name = "store_parent_path")
    private String storeParentPath;

    @Column(name = "store_parent_url")
    private String storeParentUrl;

    @Column(name = "keep_in_cache_until_date")
    private OffsetDateTime keepInCacheUntilDate;

    private Long fileSize;

    public FileInBuildingPackage(Long storageRequestId,
                                 String storage,
                                 String checksum,
                                 String filename,
                                 String storeParentPath,
                                 String storeParentUrl,
                                 Long fileSize) {
        this.storageRequestId = storageRequestId;
        this.storage = storage;
        this.checksum = checksum;
        this.filename = filename;
        this.fileSize = fileSize;
        this.status = FileInBuildingPackageStatus.WAITING_PACKAGE;
        this.storeParentPath = storeParentPath;
        this.storeParentUrl = storeParentUrl;
        this.lastUpdateDate = OffsetDateTime.now();
    }

    public FileInBuildingPackage() {

    }

    public Long getId() {
        return id;
    }

    public Long getStorageRequestId() {
        return storageRequestId;
    }

    public String getStorage() {
        return storage;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getFilename() {
        return filename;
    }

    public PackageReference getPackageReference() {
        return packageReference;
    }

    public FileInBuildingPackageStatus getStatus() {
        return status;
    }

    public OffsetDateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public String getStoreParentPath() {
        return storeParentPath;
    }

    public String getStoreParentUrl() {
        return storeParentUrl;
    }

    public OffsetDateTime getKeepInCacheUntilDate() {
        return keepInCacheUntilDate;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setLastUpdateDate(OffsetDateTime lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public void setKeepInCacheUntilDate(OffsetDateTime keepInCacheUntilDate) {
        this.keepInCacheUntilDate = keepInCacheUntilDate;
    }

    public void setPackageReference(PackageReference packageId) {
        this.packageReference = packageId;
    }

    public void setStatus(FileInBuildingPackageStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "FileInBuildingPackage{"
               + "id="
               + id
               + ", storageRequestId="
               + storageRequestId
               + ", storage='"
               + storage
               + '\''
               + ", checksum='"
               + checksum
               + '\''
               + ", filename='"
               + filename
               + '\''
               + ", packageReference="
               + packageReference
               + ", status="
               + status
               + ", lastUpdateDate="
               + lastUpdateDate
               + ", storeParentPath='"
               + storeParentPath
               + '\''
               + ", storeParentUrl='"
               + storeParentUrl
               + '\''
               + ", keepInCacheUntilDate="
               + keepInCacheUntilDate
               + ", fileSize="
               + fileSize
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
        FileInBuildingPackage that = (FileInBuildingPackage) o;
        return Objects.equals(id, that.id) || (Objects.equals(storageRequestId, that.storageRequestId)
                                               && Objects.equals(storage, that.storage)
                                               && Objects.equals(checksum, that.checksum)
                                               && Objects.equals(filename, that.filename)
                                               && Objects.equals(packageReference, that.packageReference)
                                               && status == that.status
                                               && Objects.equals(lastUpdateDate, that.lastUpdateDate)
                                               && Objects.equals(storeParentPath, that.storeParentPath)
                                               && Objects.equals(storeParentUrl, that.storeParentUrl)
                                               && Objects.equals(keepInCacheUntilDate, that.keepInCacheUntilDate)
                                               && Objects.equals(fileSize, that.fileSize));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id,
                            storageRequestId,
                            storage,
                            checksum,
                            filename,
                            packageReference,
                            status,
                            lastUpdateDate,
                            storeParentPath,
                            storeParentUrl,
                            keepInCacheUntilDate,
                            fileSize);
    }
}
