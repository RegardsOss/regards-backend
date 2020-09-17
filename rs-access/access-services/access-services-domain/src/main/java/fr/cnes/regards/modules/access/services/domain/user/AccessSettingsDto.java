package fr.cnes.regards.modules.access.services.domain.user;

public class AccessSettingsDto {

    private Long id;

    private String mode;

    private Long maxQuota;

    private Long rateLimit;

    public AccessSettingsDto(Long id, String mode, Long maxQuota, Long rateLimit) {
        this.id = id;
        this.mode = mode;
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
