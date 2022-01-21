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
package fr.cnes.regards.modules.storage.dao.entity.download;

import javax.persistence.*;
import javax.validation.constraints.Min;
import java.util.Objects;

@Entity
@Table(name = "t_user_download_quota_limits",
    uniqueConstraints = @UniqueConstraint(name = DownloadQuotaLimitsEntity.UK_DOWNLOAD_QUOTA_LIMITS_EMAIL, columnNames = { "email" }))
@SequenceGenerator(name = DownloadQuotaLimitsEntity.DOWNLOAD_QUOTA_LIMIT_SEQUENCE, initialValue = 1, sequenceName = "seq_download_quota_limits")
public class DownloadQuotaLimitsEntity {

    public static final String UK_DOWNLOAD_QUOTA_LIMITS_EMAIL = "uk_download_quota_limits_email";
    public static final String DOWNLOAD_QUOTA_LIMIT_SEQUENCE = "downloadQuotaLimitSequence";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = DOWNLOAD_QUOTA_LIMIT_SEQUENCE)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "max_quota", nullable = false)
    @Min(value = -1, message = "The custom download quota cannot be inferior to -1 (minus one).")
    private Long maxQuota;

    @Column(name = "rate_limit", nullable = false)
    @Min(value = -1, message = "The custom download rate cannot be inferior to -1 (minus one).")
    private Long rateLimit;

    public DownloadQuotaLimitsEntity() {
        super();
    }

    public DownloadQuotaLimitsEntity(Long id, String email, Long maxQuota, Long rateLimit) {
        this.id = id;
        this.email = email;
        this.maxQuota = maxQuota;
        this.rateLimit = rateLimit;
    }

    public DownloadQuotaLimitsEntity(String email, Long maxQuota, Long rateLimit) {
        this.email = email;
        this.maxQuota = maxQuota;
        this.rateLimit = rateLimit;
    }

    public DownloadQuotaLimitsEntity(DownloadQuotaLimitsEntity other) {
        this.email = other.email;
        this.maxQuota = other.maxQuota;
        this.rateLimit = other.rateLimit;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getMaxQuota() {
        return maxQuota;
    }

    public void setMaxQuota(Long maxQuota) {
        this.maxQuota = maxQuota;
    }

    public Long getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(Long window) {
        this.rateLimit = window;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DownloadQuotaLimitsEntity that = (DownloadQuotaLimitsEntity) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(email, that.email) &&
            Objects.equals(maxQuota, that.maxQuota) &&
            Objects.equals(rateLimit, that.rateLimit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, maxQuota, rateLimit);
    }
}
