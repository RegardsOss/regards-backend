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
package fr.cnes.regards.modules.storage.dao.entity.download;

import javax.persistence.*;
import javax.validation.constraints.Min;
import java.util.Objects;

@Entity
@Table(name = "t_default_download_quota_limits")
@SequenceGenerator(name = DefaultDownloadQuotaLimitsEntity.DEFAULT_DOWNLOAD_QUOTA_LIMIT_SEQUENCE, initialValue = 1, sequenceName = "seq_default_download_quota_limits")
public class DefaultDownloadQuotaLimitsEntity {

    public static final String DEFAULT_DOWNLOAD_QUOTA_LIMIT_SEQUENCE = "defaultDownloadQuotaLimitSequence";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = DEFAULT_DOWNLOAD_QUOTA_LIMIT_SEQUENCE)
    @Column(name = "id")
    private Long id;

    @Column(name = "max_quota", nullable = false)
    @Min(value = -1, message = "The default download quota cannot be inferior to -1 (minus one).")
    private Long maxQuota;

    @Column(name = "rate_limit", nullable = false)
    @Min(value = -1, message = "The default download rate cannot be inferior to -1 (minus one).")
    private Long rateLimit;

    public DefaultDownloadQuotaLimitsEntity() {
        super();
    }

    public DefaultDownloadQuotaLimitsEntity(Long id, Long maxQuota, Long rateLimit) {
        this.id = id;
        this.maxQuota = maxQuota;
        this.rateLimit = rateLimit;
    }

    public DefaultDownloadQuotaLimitsEntity(Long maxQuota, Long rateLimit) {
        this.maxQuota = maxQuota;
        this.rateLimit = rateLimit;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        DefaultDownloadQuotaLimitsEntity that = (DefaultDownloadQuotaLimitsEntity) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(maxQuota, that.maxQuota) &&
            Objects.equals(rateLimit, that.rateLimit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, maxQuota, rateLimit);
    }
}
