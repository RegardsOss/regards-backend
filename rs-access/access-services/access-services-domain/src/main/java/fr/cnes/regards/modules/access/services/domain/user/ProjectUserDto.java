package fr.cnes.regards.modules.access.services.domain.user;

import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.storage.domain.dto.quota.DownloadQuotaLimitsDto;

import java.time.OffsetDateTime;
import java.util.List;

public class ProjectUserDto {
    private Long id;

    private String email;

    private OffsetDateTime lastConnection;

    private OffsetDateTime lastUpdate;

    private UserStatus status;

    private List<MetaData> metadata;

    private Role role;

    @GsonIgnore
    private List<ResourcesAccess> permissions;

    private boolean licenseAccepted;

    private Long maxQuota;

    private Long rateLimit;

    public ProjectUserDto(ProjectUser projectUser, DownloadQuotaLimitsDto limits) {
        this.id = projectUser.getId();
        this.email = projectUser.getEmail();
        this.lastConnection = projectUser.getLastConnection();
        this.lastUpdate = projectUser.getLastUpdate();
        this.status = projectUser.getStatus();
        this.metadata = projectUser.getMetadata();
        this.role = projectUser.getRole();
        this.permissions = projectUser.getPermissions();
        this.licenseAccepted = projectUser.isLicenseAccepted();
        this.maxQuota = limits.getMaxQuota();
        this.rateLimit = limits.getRateLimit();
    }

    public ProjectUserDto(DownloadQuotaLimitsDto limits, ProjectUser projectUser) {
        this(projectUser, limits);
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

    public OffsetDateTime getLastConnection() {
        return lastConnection;
    }

    public void setLastConnection(OffsetDateTime lastConnection) {
        this.lastConnection = lastConnection;
    }

    public OffsetDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(OffsetDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public List<MetaData> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<MetaData> metadata) {
        this.metadata = metadata;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<ResourcesAccess> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<ResourcesAccess> permissions) {
        this.permissions = permissions;
    }

    public boolean isLicenseAccepted() {
        return licenseAccepted;
    }

    public void setLicenseAccepted(boolean licenseAccepted) {
        this.licenseAccepted = licenseAccepted;
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

    public void setRateLimit(Long rateLimit) {
        this.rateLimit = rateLimit;
    }
}
