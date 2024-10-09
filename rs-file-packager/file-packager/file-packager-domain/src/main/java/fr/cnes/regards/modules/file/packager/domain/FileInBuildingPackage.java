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

import com.google.common.base.Strings;
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.framework.module.manager.ConfigIgnore;
import jakarta.annotation.Nullable;
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

    @Column(name = "storage_request_id", nullable = false)
    private Long storageRequestId;

    @Column(nullable = false)
    private String storage;

    @Column(nullable = false)
    private String checksum;

    @Column(nullable = false)
    private String filename;

    /**
     * The {@link PackageReference} containing the file.
     * null if the file is not yet associated to a package.
     */
    @GsonIgnore
    @ManyToOne()
    @JoinColumn(name = "package_id", foreignKey = @ForeignKey(name = "fk_acq_file_id"))
    private PackageReference packageReference;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private FileInBuildingPackageStatus status;

    @Column(name = "last_update_date", nullable = false)
    private OffsetDateTime lastUpdateDate;

    @Column(name = "storage_subdirectory", nullable = false)
    private String storageSubdirectory;

    @Column(name = "final_archive_parent_url", nullable = false)
    private String finalArchiveParentUrl;

    @Column(name = "file_cache_path", nullable = false)
    private String fileCachePath;

    @Column(name = "keep_in_cache_until_date")
    @Nullable
    private OffsetDateTime keepInCacheUntilDate;

    @Column(nullable = false)
    private Long fileSize;

    @Column(name = "error_cause")
    @Nullable
    private String errorCause;

    public FileInBuildingPackage(Long storageRequestId,
                                 String storage,
                                 String checksum,
                                 String filename,
                                 String storageSubdirectory,
                                 String finalArchiveParentUrl,
                                 String fileCachePath,
                                 Long fileSize) {
        this.storageRequestId = storageRequestId;
        this.storage = storage;
        this.checksum = checksum;
        this.filename = filename;
        this.fileCachePath = fileCachePath;
        this.fileSize = fileSize;
        this.status = FileInBuildingPackageStatus.WAITING_PACKAGE;
        this.storageSubdirectory = storageSubdirectory;
        this.finalArchiveParentUrl = finalArchiveParentUrl;
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

    public String getStorageSubdirectory() {
        return storageSubdirectory;
    }

    public String getFinalArchiveParentUrl() {
        return finalArchiveParentUrl;
    }

    public String getFileCachePath() {
        return fileCachePath;
    }

    public OffsetDateTime getKeepInCacheUntilDate() {
        return keepInCacheUntilDate;
    }

    public Long getFileSize() {
        return fileSize;
    }

    @Nullable
    public String getErrorCause() {
        return errorCause;
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

    public void setErrorCause(@Nullable String errorCause) {
        this.errorCause = errorCause;
    }

    public void updateStatus(FileInBuildingPackageStatus status, @Nullable String errorCause) {
        this.setStatus(status);
        this.setLastUpdateDate(OffsetDateTime.now());

        if (!Strings.isNullOrEmpty(errorCause)) {
            this.setErrorCause(errorCause);
        }
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
               + ", storageSubdirectory='"
               + storageSubdirectory
               + '\''
               + ", finalArchiveParentUrl='"
               + finalArchiveParentUrl
               + '\''
               + ", fileCachePath='"
               + fileCachePath
               + '\''
               + ", keepInCacheUntilDate="
               + keepInCacheUntilDate
               + ", fileSize="
               + fileSize
               + ", errorCause='"
               + errorCause
               + '\''
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
        return Objects.equals(id, that.id)
               && Objects.equals(storageRequestId, that.storageRequestId)
               && Objects.equals(storage,
                                 that.storage)
               && Objects.equals(checksum, that.checksum)
               && Objects.equals(filename, that.filename)
               && Objects.equals(packageReference, that.packageReference)
               && status == that.status
               && Objects.equals(lastUpdateDate, that.lastUpdateDate)
               && Objects.equals(storageSubdirectory, that.storageSubdirectory)
               && Objects.equals(finalArchiveParentUrl, that.finalArchiveParentUrl)
               && Objects.equals(fileCachePath, that.fileCachePath)
               && Objects.equals(keepInCacheUntilDate, that.keepInCacheUntilDate)
               && Objects.equals(fileSize, that.fileSize)
               && Objects.equals(errorCause, that.errorCause);
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
                            storageSubdirectory,
                            finalArchiveParentUrl,
                            fileCachePath,
                            keepInCacheUntilDate,
                            fileSize,
                            errorCause);
    }
}
