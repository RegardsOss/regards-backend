package fr.cnes.regards.modules.access.services.domain.user;

import fr.cnes.regards.modules.accessrights.domain.projects.Role;

import java.util.List;

public class AccessSettingsDto {

    private Long id;

    private String mode;

    private Role role;

    private List<String> groups;

    private Long maxQuota;

    private Long rateLimit;

    public AccessSettingsDto(Long id, String mode, Role role, List<String> groups, Long maxQuota, Long rateLimit) {
        this.id = id;
        this.mode = mode;
        this.role = role;
        this.groups = groups;
        this.maxQuota = maxQuota;
        this.rateLimit = rateLimit;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
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
