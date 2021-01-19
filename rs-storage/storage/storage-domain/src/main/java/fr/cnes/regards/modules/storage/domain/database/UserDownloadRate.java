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

import java.time.LocalDateTime;
import java.util.Objects;

public class UserDownloadRate {

    private final Long id;

    private final String instance;

    private final String tenant;

    private final String email;

    private final Long gauge;

    private final LocalDateTime expiry;

    public UserDownloadRate(
        String instance,
        String tenant,
        String email,
        Long gauge,
        LocalDateTime expiry
    ) {
        this.id = null;
        this.instance = instance;
        this.tenant = tenant;
        this.email = email;
        this.gauge = gauge;
        this.expiry = expiry;
    }

    public UserDownloadRate(
        Long id,
        String instance,
        String tenant,
        String email,
        Long gauge,
        LocalDateTime expiry
    ) {
        this.id = id;
        this.instance = instance;
        this.tenant = tenant;
        this.email = email;
        this.gauge = gauge;
        this.expiry = expiry;
    }

    public Long getId() {
        return id;
    }

    public String getInstance() {
        return instance;
    }

    public String getTenant() {
        return this.tenant;
    }

    public String getEmail() {
        return email;
    }

    public Long getGauge() {
        return gauge;
    }

    public LocalDateTime getExpiry() {
        return expiry;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserDownloadRate that = (UserDownloadRate) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(instance, that.instance) &&
            Objects.equals(tenant, that.tenant) &&
            Objects.equals(email, that.email) &&
            Objects.equals(gauge, that.gauge) &&
            Objects.equals(expiry, that.expiry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, instance, tenant, email, gauge, expiry);
    }
}
