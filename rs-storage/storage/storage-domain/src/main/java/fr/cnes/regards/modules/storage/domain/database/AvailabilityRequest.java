package fr.cnes.regards.modules.storage.domain.database;

import java.time.OffsetDateTime;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class AvailabilityRequest {

    private Set<String> checksums;

    private OffsetDateTime expirationDate;

    public AvailabilityRequest() {
    }

    public AvailabilityRequest(OffsetDateTime expirationDate, String... checksums) {
        this.checksums = Sets.newHashSet(checksums);
        this.expirationDate = expirationDate;
    }

    public Set<String> getChecksums() {
        return checksums;
    }

    public void setChecksums(Set<String> checksums) {
        this.checksums = checksums;
    }

    public OffsetDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(OffsetDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

}
