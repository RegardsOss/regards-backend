/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * This class is the database entity corresponding to {@link fr.cnes.regards.modules.processing.domain.POutputFile}
 *
 * @author gandrieu
 */
@Data
@With
@AllArgsConstructor
@RequiredArgsConstructor

@Table("t_outputfile")
public class OutputFileEntity implements Persistable<UUID> {

    private @Id UUID id;

    /**
     * The execution this file has been generated for
     */
    private @Column("exec_id") UUID execId;

    /**
     * Where to download from
     */
    private @Column("url") URL url;

    /**
     * The file name
     */
    private @Column("name") String name;

    /**
     * The file checksum
     */
    private @Column("checksum_value") String checksumValue;

    private @Column("checksum_method") String checksumMethod;

    /**
     * The file size
     */
    private @Column("size_bytes") Long sizeInBytes;

    private @Column("input_correlation_ids") List<String> inputCorrelationIds;

    /**
     * The file creation time (not the entity creation time)
     */
    private @Column("created") OffsetDateTime created;

    /**
     * Whether the file has been downloaded or not
     */
    private @Column("downloaded") boolean downloaded;

    /**
     * Whether the file has been deleted or not
     */
    private @Column("deleted") boolean deleted;

    /**
     * Because the R2DBC driver has no post-load / post-persist hook for the moment,
     * this property is dealt with manually in the domain repository implementation.
     */
    @EqualsAndHashCode.Exclude
    @Transient
    private boolean persisted = true; // True when comes from the database

    public OutputFileEntity persisted() {
        this.persisted = true;
        return this;
    }

    @Override
    public boolean isNew() {
        return !persisted;
    }
}
