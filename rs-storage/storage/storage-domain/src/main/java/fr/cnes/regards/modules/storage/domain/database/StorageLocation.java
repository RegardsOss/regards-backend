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
package fr.cnes.regards.modules.storage.domain.database;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Database definition of the table containing monitoring information about all storage location known.
 *
 * @author Sébastien Binda
 */
@Entity
@Table(name = "t_storage_location",
       indexes = { @Index(name = "idx_storage_location", columnList = "name") },
       uniqueConstraints = { @UniqueConstraint(name = "uk_t_storage_location_name", columnNames = { "name" }) })
public class StorageLocation {

    /**
     * Internal database unique identifier
     */
    @Id
    @SequenceGenerator(name = "storageLocationSequence", initialValue = 1, sequenceName = "seq_storage_location")
    @GeneratedValue(generator = "storageLocationSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(length = 128)
    private String name;

    @Column(name = "nb_ref_files")
    private Long numberOfReferencedFiles = 0L;

    @Column(name = "total_size_ko")
    private Long totalSizeOfReferencedFiles = 0L;

    @Column(name = "nb_ref_pending")
    private Long numberOfPendingFiles = 0L;

    @Column(name = "last_update_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastUpdateDate;

    @Column(name = "pending_action_remaining")
    private boolean pendingActionRemaining = false;

    public StorageLocation(String name) {
        super();
        this.name = name;
    }

    public StorageLocation() {
        super();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getNumberOfReferencedFiles() {
        return numberOfReferencedFiles;
    }

    public void setNumberOfReferencedFiles(Long numberOfReferencedFiles) {
        this.numberOfReferencedFiles = numberOfReferencedFiles;
    }

    public Long getNumberOfPendingFiles() {
        return numberOfPendingFiles;
    }

    public void setNumberOfPendingFiles(Long numberOfPendingFiles) {
        this.numberOfPendingFiles = numberOfPendingFiles;
    }

    public Long getTotalSizeOfReferencedFilesInKo() {
        return totalSizeOfReferencedFiles;
    }

    public void setTotalSizeOfReferencedFilesInKo(Long totalSizeOfReferencedFiles) {
        this.totalSizeOfReferencedFiles = totalSizeOfReferencedFiles;
    }

    public OffsetDateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(OffsetDateTime lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public boolean getPendingActionRemaining() {
        return pendingActionRemaining;
    }

    public void setPendingActionRemaining(boolean pendingActionRemaining) {
        this.pendingActionRemaining = pendingActionRemaining;
    }
}
