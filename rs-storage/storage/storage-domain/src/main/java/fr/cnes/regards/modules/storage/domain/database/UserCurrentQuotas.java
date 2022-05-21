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

import java.util.Objects;

public class UserCurrentQuotas {

    private String email;

    private Long maxQuota;

    private Long rateLimit;

    private Long currentQuota;

    private Long currentRate;

    public UserCurrentQuotas(String email, Long maxQuota, Long rateLimit, Long currentQuota, Long currentRate) {
        this.email = email;
        this.maxQuota = maxQuota;
        this.rateLimit = rateLimit;
        this.currentQuota = currentQuota;
        this.currentRate = currentRate;
    }

    public UserCurrentQuotas(String email) {
        this.email = email;
    }

    public UserCurrentQuotas() {
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

    public Long getCurrentQuota() {
        return currentQuota;
    }

    public Long getCurrentRate() {
        return currentRate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserCurrentQuotas that = (UserCurrentQuotas) o;
        return Objects.equals(email, that.email) && Objects.equals(maxQuota, that.maxQuota) && Objects.equals(rateLimit,
                                                                                                              that.rateLimit)
            && Objects.equals(currentQuota, that.currentQuota) && Objects.equals(currentRate, that.currentRate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, maxQuota, rateLimit, currentQuota, currentRate);
    }

}
