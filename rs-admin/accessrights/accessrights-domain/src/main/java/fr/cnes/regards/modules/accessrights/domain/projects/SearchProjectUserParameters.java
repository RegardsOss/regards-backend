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
package fr.cnes.regards.modules.accessrights.domain.projects;

import fr.cnes.regards.framework.jpa.restriction.DatesRangeRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.framework.jpa.utils.AbstractSearchParameters;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Collection;

/**
 * This is criterias to filter on entity ProjectUser
 *
 * @author Stephane Cortine
 */
public class SearchProjectUserParameters implements AbstractSearchParameters<ProjectUser> {

    @Schema(description = "Filter on user email")
    private String email;

    @Schema(description = "Filter on user lastname")
    private String lastName;

    @Schema(description = "Filter on user firstName")
    private String firstName;

    @Schema(description = "Filter on status")
    private ValuesRestriction<UserStatus> status;

    @Schema(description = "Filter on origins")
    private ValuesRestriction<String> origins;

    @Schema(description = "Filter on roles")
    private ValuesRestriction<String> roles;

    @Schema(description = "Filter on creation date")
    private DatesRangeRestriction creationDate = new DatesRangeRestriction();

    @Schema(description = "Filter on last connection date")
    private DatesRangeRestriction lastConnection = new DatesRangeRestriction();

    @Schema(description = "Filter on access groups")
    private ValuesRestriction<String> accessGroups;

    @Schema(description = "Filter on quota warning count")
    private Long quotaWarningCount;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public SearchProjectUserParameters withEmail(String email) {
        this.email = email;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public SearchProjectUserParameters withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public SearchProjectUserParameters withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public ValuesRestriction<UserStatus> getStatus() {
        return status;
    }

    public void setStatus(ValuesRestriction<UserStatus> status) {
        this.status = status;
    }

    public SearchProjectUserParameters withStatusIncluded(Collection<UserStatus> status) {
        this.status = new ValuesRestriction<UserStatus>().withInclude(status);
        return this;
    }

    public SearchProjectUserParameters withStatusExcluded(Collection<UserStatus> status) {
        this.status = new ValuesRestriction<UserStatus>().withExclude(status);
        return this;
    }

    public ValuesRestriction<String> getOrigins() {
        return origins;
    }

    public void setOrigins(ValuesRestriction<String> origins) {
        this.origins = origins;
    }

    public SearchProjectUserParameters withOriginsIncluded(Collection<String> origins) {
        this.origins = new ValuesRestriction<String>().withInclude(origins);
        return this;
    }

    public SearchProjectUserParameters withOriginsExcluded(Collection<String> origins) {
        this.origins = new ValuesRestriction<String>().withExclude(origins);
        return this;
    }

    public ValuesRestriction<String> getRoles() {
        return roles;
    }

    public void setRoles(ValuesRestriction<String> roles) {
        this.roles = roles;
    }

    public SearchProjectUserParameters withRolesIncluded(Collection<String> roles) {
        this.roles = new ValuesRestriction<String>().withInclude(roles);
        return this;
    }

    public SearchProjectUserParameters withRolesExcluded(Collection<String> roles) {
        this.roles = new ValuesRestriction<String>().withExclude(roles);
        return this;
    }

    public DatesRangeRestriction getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(DatesRangeRestriction creationDate) {
        this.creationDate = creationDate;
    }

    public SearchProjectUserParameters withCreationDateAfter(OffsetDateTime after) {
        this.creationDate.setAfter(after);
        return this;
    }

    public SearchProjectUserParameters withCreationDateBefore(OffsetDateTime before) {
        this.creationDate.setBefore(before);
        return this;
    }

    public DatesRangeRestriction getLastConnection() {
        return lastConnection;
    }

    public void setLastConnection(DatesRangeRestriction lastConnection) {
        this.lastConnection = lastConnection;
    }

    public SearchProjectUserParameters withLastConnectionAfter(OffsetDateTime after) {
        this.lastConnection.setAfter(after);
        return this;
    }

    public SearchProjectUserParameters withLastConnectionBefore(OffsetDateTime before) {
        this.lastConnection.setBefore(before);
        return this;
    }

    public ValuesRestriction<String> getAccessGroups() {
        return accessGroups;
    }

    public void setAccessGroups(ValuesRestriction<String> accessGroups) {
        this.accessGroups = accessGroups;
    }

    public SearchProjectUserParameters withAccessGroupsIncluded(Collection<String> accessGroups) {
        this.accessGroups = new ValuesRestriction<String>().withInclude(accessGroups);
        return this;
    }

    public SearchProjectUserParameters withAccessGroupsExcluded(Collection<String> accessGroups) {
        this.accessGroups = new ValuesRestriction<String>().withExclude(accessGroups);
        return this;
    }

    public Long getQuotaWarningCount() {
        return quotaWarningCount;
    }

    public void setQuotaWarningCount(Long quotaWarningCount) {
        this.quotaWarningCount = quotaWarningCount;
    }

    public SearchProjectUserParameters withQuotaWarningCount(Long quotaWarningCount) {
        this.quotaWarningCount = quotaWarningCount;
        return this;
    }
}
