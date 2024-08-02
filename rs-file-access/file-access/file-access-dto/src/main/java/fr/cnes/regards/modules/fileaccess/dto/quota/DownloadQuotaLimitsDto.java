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
package fr.cnes.regards.modules.fileaccess.dto.quota;

import java.util.Objects;

public class DownloadQuotaLimitsDto {

    private final String email;

    private final Long maxQuota;

    private final Long rateLimit;

    public DownloadQuotaLimitsDto(String email, Long maxQuota, Long rateLimit) {
        this.email = email;
        this.maxQuota = maxQuota;
        this.rateLimit = rateLimit;
    }

    public String getEmail() {
        return email;
    }

    public Long getMaxQuota() {
        return maxQuota;
    }

    public Long getRateLimit() {
        return rateLimit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DownloadQuotaLimitsDto that = (DownloadQuotaLimitsDto) o;
        return Objects.equals(email, that.email) && Objects.equals(maxQuota, that.maxQuota) && Objects.equals(rateLimit,
                                                                                                              that.rateLimit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, maxQuota, rateLimit);
    }
}
