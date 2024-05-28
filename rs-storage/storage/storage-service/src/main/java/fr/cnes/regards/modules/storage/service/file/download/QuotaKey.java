package fr.cnes.regards.modules.storage.service.file.download;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;

public final class QuotaKey {

    private String tenant;

    private String userEmail;

    private QuotaKey() {
    }

    private QuotaKey(String tenant, String userEmail) {
        this.tenant = tenant;
        this.userEmail = userEmail;
    }

    public String getTenant() {
        return tenant;
    }

    public String getUserEmail() {
        return userEmail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QuotaKey quotaKey = (QuotaKey) o;
        return Objects.equals(tenant, quotaKey.tenant) && Objects.equals(userEmail, quotaKey.userEmail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenant, userEmail);
    }

    public static QuotaKey make(@NotNull String tenant, @NotNull String userEmail) {
        return new QuotaKey(Objects.requireNonNull(tenant, "tenant must not be null"),
                            Objects.requireNonNull(userEmail, "userEmail must not be null"));
    }
}
