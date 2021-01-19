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

import java.util.Objects;

public class DefaultDownloadQuotaLimits {

    private final Long maxQuota;

    private final Long rateLimit;

    public DefaultDownloadQuotaLimits(Long maxQuota, Long rateLimit) {
        this.maxQuota = maxQuota;
        this.rateLimit = rateLimit;
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
        DefaultDownloadQuotaLimits that = (DefaultDownloadQuotaLimits) o;
        return Objects.equals(maxQuota, that.maxQuota) &&
            Objects.equals(rateLimit, that.rateLimit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxQuota, rateLimit);
    }
}
