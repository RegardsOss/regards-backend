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
package fr.cnes.regards.modules.access.services.domain.user;

import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;

import java.util.List;
import java.util.Set;

abstract class ProjectUserBaseDto {

    protected String email;

    protected String firstName;

    protected String lastName;

    @GsonIgnore
    protected List<ResourcesAccess> permissions;

    protected List<MetaData> metadata;

    protected Set<String> accessGroups;

    protected Long maxQuota;

    protected Long rateLimit;

    public String getEmail() {
        return email;
    }

    public ProjectUserBaseDto setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public ProjectUserBaseDto setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public ProjectUserBaseDto setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public List<ResourcesAccess> getPermissions() {
        return permissions;
    }

    public ProjectUserBaseDto setPermissions(List<ResourcesAccess> permissions) {
        this.permissions = permissions;
        return this;
    }

    public List<MetaData> getMetadata() {
        return metadata;
    }

    public ProjectUserBaseDto setMetadata(List<MetaData> metadata) {
        this.metadata = metadata;
        return this;
    }

    public Set<String> getAccessGroups() {
        return accessGroups;
    }

    public ProjectUserBaseDto setAccessGroups(Set<String> accessGroups) {
        this.accessGroups = accessGroups;
        return this;
    }

    public Long getMaxQuota() {
        return maxQuota;
    }

    public ProjectUserBaseDto setMaxQuota(Long maxQuota) {
        this.maxQuota = maxQuota;
        return this;
    }

    public Long getRateLimit() {
        return rateLimit;
    }

    public ProjectUserBaseDto setRateLimit(Long rateLimit) {
        this.rateLimit = rateLimit;
        return this;
    }

}
