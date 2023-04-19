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

import javax.persistence.*;
import java.util.Objects;

/**
 *
 */
@Entity
@Table(name = "t_glacier_archive",
       indexes = { @Index(name = "idx_glacier_archive", columnList = "url") },
       uniqueConstraints = { @UniqueConstraint(name = "uk_t_glacier_archive_url", columnNames = { "url" }) })
public class GlacierArchive {

    /**
     * Internal database unique identifier
     */
    @Id
    @SequenceGenerator(name = "glacierArchiveSequence", initialValue = 1, sequenceName = "seq_glacier_archive")
    @GeneratedValue(generator = "glacierArchiveSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "url")
    private String url;

    @Column(name = "checksum", nullable = false)
    private String checksum;

    @Column(name = "size_ko")
    private Long archiveSize = 0L;

    public GlacierArchive(String url, String checksum, Long archiveSize) {
        super();
        this.url = url;
        this.checksum = checksum;
        this.archiveSize = archiveSize;
    }

    public GlacierArchive() {
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public Long getArchiveSize() {
        return archiveSize;
    }

    public void setArchiveSize(Long archiveSize) {
        this.archiveSize = archiveSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GlacierArchive that = (GlacierArchive) o;
        return Objects.equals(url, that.url) && Objects.equals(checksum, that.checksum) && Objects.equals(archiveSize,
                                                                                                          that.archiveSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, checksum, archiveSize);
    }
}
