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
package fr.cnes.regards.modules.access.services.domain.user;

import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.fileaccess.dto.quota.UserCurrentQuotasDto;

import java.time.OffsetDateTime;
import java.util.ArrayList;

public class ProjectUserReadDto extends ProjectUserBaseDto {

    private Long id;

    private OffsetDateTime lastConnection;

    private OffsetDateTime lastUpdate;

    private OffsetDateTime created;

    private UserStatus status;

    private Role role;

    private boolean licenseAccepted;

    private String origin;

    private Long currentQuota;

    private Long currentRate;

    public ProjectUserReadDto(ProjectUser projectUser, Long rateLimit, Long currentRate) {
        this.id = projectUser.getId();
        this.email = projectUser.getEmail();
        this.firstName = projectUser.getFirstName();
        this.lastName = projectUser.getLastName();
        this.lastConnection = projectUser.getLastConnection();
        this.lastUpdate = projectUser.getLastUpdate();
        this.created = projectUser.getCreated();
        this.status = projectUser.getStatus();
        this.metadata = new ArrayList<>(projectUser.getMetadata());
        this.role = projectUser.getRole();
        this.permissions = projectUser.getPermissions();
        this.licenseAccepted = projectUser.isLicenseAccepted();
        this.origin = projectUser.getOrigin();
        this.accessGroups = projectUser.getAccessGroups();
        this.maxQuota = projectUser.getMaxQuota();
        this.currentQuota = projectUser.getCurrentQuota();
        this.rateLimit = rateLimit;
        this.currentRate = currentRate;
    }

    public ProjectUserReadDto(ProjectUser projectUser, UserCurrentQuotasDto currentQuotas) {
        this(projectUser, currentQuotas.getRateLimit(), currentQuotas.getCurrentRate());
    }

    public ProjectUserReadDto(UserCurrentQuotasDto currentQuotas, ProjectUser projectUser) {
        this(projectUser, currentQuotas);
    }

    public Long getId() {
        return id;
    }

    public ProjectUserReadDto setId(Long id) {
        this.id = id;
        return this;
    }

    public OffsetDateTime getLastConnection() {
        return lastConnection;
    }

    public ProjectUserReadDto setLastConnection(OffsetDateTime lastConnection) {
        this.lastConnection = lastConnection;
        return this;
    }

    public OffsetDateTime getLastUpdate() {
        return lastUpdate;
    }

    public ProjectUserReadDto setLastUpdate(OffsetDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
        return this;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public ProjectUserReadDto setCreated(OffsetDateTime created) {
        this.created = created;
        return this;
    }

    public UserStatus getStatus() {
        return status;
    }

    public ProjectUserReadDto setStatus(UserStatus status) {
        this.status = status;
        return this;
    }

    public Role getRole() {
        return role;
    }

    public ProjectUserReadDto setRole(Role role) {
        this.role = role;
        return this;
    }

    public boolean isLicenseAccepted() {
        return licenseAccepted;
    }

    public ProjectUserReadDto setLicenseAccepted(boolean licenseAccepted) {
        this.licenseAccepted = licenseAccepted;
        return this;
    }

    public String getOrigin() {
        return origin;
    }

    public ProjectUserReadDto setOrigin(String origin) {
        this.origin = origin;
        return this;
    }

    public Long getCurrentQuota() {
        return currentQuota;
    }

    public ProjectUserReadDto setCurrentQuota(Long currentQuota) {
        this.currentQuota = currentQuota;
        return this;
    }

    public Long getCurrentRate() {
        return currentRate;
    }

    public ProjectUserReadDto setCurrentRate(Long currentRate) {
        this.currentRate = currentRate;
        return this;
    }

}
