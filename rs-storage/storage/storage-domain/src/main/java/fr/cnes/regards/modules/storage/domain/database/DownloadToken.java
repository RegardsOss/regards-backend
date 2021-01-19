/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Download tokens are entities to temporarly access download to one file thanks to a given token.
 *
 * @author Sébastien Binda
 */
@Entity
@Table(name = "t_donwload_token", indexes = { @Index(name = "idx_download_token", columnList = "token, checksum") })
public class DownloadToken {

    /**
     * Internal database unique identifier
     */
    @Id
    @SequenceGenerator(name = "downloadTokenSequence", initialValue = 1, sequenceName = "seq_download_token")
    @GeneratedValue(generator = "downloadTokenSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private String checksum;

    @Column(nullable = false)
    private OffsetDateTime expirationDate;

    public static DownloadToken build(String token, String checksum, OffsetDateTime expirationDate) {
        DownloadToken dt = new DownloadToken();
        dt.checksum = checksum;
        dt.token = token;
        dt.expirationDate = expirationDate;
        return dt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public OffsetDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(OffsetDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Long getId() {
        return id;
    }

}
