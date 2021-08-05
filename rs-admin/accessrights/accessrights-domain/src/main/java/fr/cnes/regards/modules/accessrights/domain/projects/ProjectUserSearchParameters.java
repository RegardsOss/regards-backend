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
package fr.cnes.regards.modules.accessrights.domain.projects;

import fr.cnes.regards.framework.jpa.utils.AbstractSearchParameters;

import java.time.OffsetDateTime;

public class ProjectUserSearchParameters implements AbstractSearchParameters<ProjectUser> {

    private String email;
    private String lastName;
    private String firstName;
    private String status;
    private String origin;
    private String role;
    private OffsetDateTime createdBefore;
    private OffsetDateTime createdAfter;
    private OffsetDateTime lastConnectionBefore;
    private OffsetDateTime lastConnectionAfter;
    private Long quotaWarningCount;
    private String accessGroup;


    public String getEmail() {
        return email;
    }

    public ProjectUserSearchParameters setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public ProjectUserSearchParameters setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public ProjectUserSearchParameters setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public ProjectUserSearchParameters setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getOrigin() {
        return origin;
    }

    public ProjectUserSearchParameters setOrigin(String origin) {
        this.origin = origin;
        return this;
    }

    public String getRole() {
        return role;
    }

    public ProjectUserSearchParameters setRole(String role) {
        this.role = role;
        return this;
    }

    public OffsetDateTime getCreatedBefore() {
        return createdBefore;
    }

    public ProjectUserSearchParameters setCreatedBefore(OffsetDateTime createdBefore) {
        this.createdBefore = createdBefore;
        return this;
    }

    public OffsetDateTime getCreatedAfter() {
        return createdAfter;
    }

    public ProjectUserSearchParameters setCreatedAfter(OffsetDateTime createdAfter) {
        this.createdAfter = createdAfter;
        return this;
    }

    public OffsetDateTime getLastConnectionBefore() {
        return lastConnectionBefore;
    }

    public ProjectUserSearchParameters setLastConnectionBefore(OffsetDateTime lastConnectionBefore) {
        this.lastConnectionBefore = lastConnectionBefore;
        return this;
    }

    public OffsetDateTime getLastConnectionAfter() {
        return lastConnectionAfter;
    }

    public ProjectUserSearchParameters setLastConnectionAfter(OffsetDateTime lastConnectionAfter) {
        this.lastConnectionAfter = lastConnectionAfter;
        return this;
    }

    public Long getQuotaWarningCount() {
        return quotaWarningCount;
    }

    public ProjectUserSearchParameters setQuotaWarningCount(Long quotaWarningCount) {
        this.quotaWarningCount = quotaWarningCount;
        return this;
    }

    public String getAccessGroup() {
        return accessGroup;
    }

    public ProjectUserSearchParameters setAccessGroup(String accessGroup) {
        this.accessGroup = accessGroup;
        return this;
    }

}
