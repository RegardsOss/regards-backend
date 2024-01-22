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

import com.fasterxml.jackson.annotation.JsonFilter;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.fileaccess.dto.FileLocationDto;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceDto;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceMetaInfoDto;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceWithoutOwnersDto;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Database POJO definition for referenced files
 *
 * @author Sébastien Binda
 */
@Entity
@JsonFilter("owners")
@Table(name = "t_file_reference",
       indexes = { @Index(name = "idx_file_reference_checksum", columnList = "checksum"),
                   @Index(name = "idx_file_reference_storage", columnList = "storage"),
                   @Index(name = "idx_file_reference_storage_checksum", columnList = "checksum, storage"),
                   @Index(name = "idx_file_reference_type", columnList = "type") },
       uniqueConstraints = { @UniqueConstraint(name = "uk_t_file_reference_checksum_storage",
                                               columnNames = { "checksum", "storage" }) })
@NamedEntityGraph(name = "graph.filereference.owners", attributeNodes = { @NamedAttributeNode(value = "owners") })
public class FileReference {

    /**
     * Internal database unique identifier
     */
    @Id
    @SequenceGenerator(name = "fileReferenceSequence", initialValue = 1, sequenceName = "seq_file_reference")
    @GeneratedValue(generator = "fileReferenceSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * Stored date
     */
    @Column
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime storageDate;

    @ElementCollection(fetch = FetchType.LAZY)
    @Column(name = "owner")
    @CollectionTable(name = "ta_file_reference_owner", joinColumns = @JoinColumn(name = "file_ref_id"))
    private final Set<String> owners = Sets.newHashSet();

    /**
     * Meta information about current file reference
     */
    @Embedded
    private FileReferenceMetaInfo metaInfo;

    /**
     * Location of current reference file
     */
    @Embedded
    private FileLocation location;

    @Column(name = "referenced")
    private boolean referenced = false;

    /**
     * By default, nearlineConfirmed is set to False, we don't know if the file is stored in online mode (datalake Tier2) or in nearline mode
     * (datalake Tier3).
     * Otherwise, nearlineConfirmed set to True means that the file is stored in nearline mode (datalake Tier3).
     */
    @Column(name = "nearline_confirmed")
    private boolean nearlineConfirmed = false;

    public FileReference() {
        super();
    }

    public FileReference(String owner, FileReferenceMetaInfo metaInfo, FileLocation location) {
        this(Sets.newHashSet(owner), metaInfo, location);
    }

    public FileReference(Collection<String> owners, FileReferenceMetaInfo metaInfo, FileLocation location) {
        super();
        Assert.notNull(owners, "File reference needs at least one owner to be created");
        Assert.isTrue(!owners.isEmpty(), "File reference needs at least one owner to be created");
        Assert.notNull(metaInfo, "File reference needs meta informations to be created");
        Assert.notNull(location, "File reference needs a storage location to be created");

        if (owners != null) {
            this.owners.addAll(owners);
        }
        this.metaInfo = metaInfo;
        this.location = location;
        this.storageDate = OffsetDateTime.now();
    }

    private FileReference(Long id,
                          OffsetDateTime storageDate,
                          FileReferenceMetaInfo metaInfo,
                          FileLocation location,
                          Set<String> owners,
                          boolean referenced,
                          boolean nearlineConfirmed) {
        this.id = id;
        this.storageDate = storageDate;
        this.metaInfo = metaInfo;
        this.location = location;
        if (owners != null) {
            this.owners.addAll(owners);
        }
        this.referenced = referenced;
        this.nearlineConfirmed = nearlineConfirmed;
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
     * @return the location
     */
    public FileLocation getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(FileLocation location) {
        this.location = location;
    }

    /**
     * @return the metaInfo
     */
    public FileReferenceMetaInfo getMetaInfo() {
        return metaInfo;
    }

    /**
     * @param metaInfo the metaInfo to set
     */
    public void setMetaInfo(FileReferenceMetaInfo metaInfo) {
        this.metaInfo = metaInfo;
    }

    /**
     * @return the storageDate
     */
    public OffsetDateTime getStorageDate() {
        return storageDate;
    }

    /**
     * @param storageDate the storageDate to set
     */
    public void setStorageDate(OffsetDateTime storageDate) {
        this.storageDate = storageDate;
    }

    /**
     *
     */
    public Collection<String> getLazzyOwners() {
        return owners;
    }

    /**
     * @return if the file is referenced (true only the file is not stored physically)
     */
    public boolean isReferenced() {
        return referenced;
    }

    /**
     * if the file is referenced (not stored physically), set to true
     *
     * @param referenced if the file is referenced
     */
    public void setReferenced(boolean referenced) {
        this.referenced = referenced;
    }

    public boolean isNearlineConfirmed() {
        return nearlineConfirmed;
    }

    public void setNearlineConfirmed(boolean nearlineConfirmed) {
        this.nearlineConfirmed = nearlineConfirmed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileReference that = (FileReference) o;
        return referenced == that.referenced
               && Objects.equals(id, that.id)
               && Objects.equals(storageDate,
                                 that.storageDate)
               && Objects.equals(owners, that.owners)
               && Objects.equals(metaInfo, that.metaInfo)
               && Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, storageDate, metaInfo, location, referenced);
    }

    public FileReferenceDto toDto() {
        return new FileReferenceDto(id,
                                    storageDate,
                                    metaInfo.toDto(),
                                    location.toDto(),
                                    owners,
                                    referenced,
                                    nearlineConfirmed);
    }

    public FileReferenceWithoutOwnersDto toDtoWithoutOwners() {
        return new FileReferenceWithoutOwnersDto(id,
                                                 storageDate,
                                                 metaInfo.toDto(),
                                                 location.toDto(),
                                                 referenced,
                                                 nearlineConfirmed);
    }

    public static FileReference fromDto(FileReferenceDto dto) {
        FileReferenceMetaInfoDto metaInfo = dto.getMetaInfo();
        Assert.notNull(metaInfo, "File reference needs meta informations to be created");
        FileLocationDto location = dto.getLocation();
        Assert.notNull(location, "File reference needs a storage location to be created");
        return new FileReference(dto.getId(),
                                 dto.getStorageDate(),
                                 FileReferenceMetaInfo.buildFromDto(metaInfo),
                                 FileLocation.buildFromDto(location),
                                 new HashSet<>(dto.getOwners()),
                                 dto.isReferenced(),
                                 dto.isNearlineConfirmed());
    }

    public static FileReference fromDto(FileReferenceWithoutOwnersDto dto) {
        FileReferenceMetaInfoDto metaInfo = dto.getMetaInfo();
        Assert.notNull(metaInfo, "File reference needs meta informations to be created");
        FileLocationDto location = dto.getLocation();
        Assert.notNull(location, "File reference needs a storage location to be created");
        return new FileReference(dto.getId(),
                                 dto.getStorageDate(),
                                 FileReferenceMetaInfo.buildFromDto(dto.getMetaInfo()),
                                 FileLocation.buildFromDto(dto.getLocation()),
                                 null,
                                 dto.isReferenced(),
                                 dto.isNearlineConfirmed());
    }
}
