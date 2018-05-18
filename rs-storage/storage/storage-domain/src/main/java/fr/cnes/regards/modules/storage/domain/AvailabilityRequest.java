package fr.cnes.regards.modules.storage.domain;

import java.time.OffsetDateTime;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * POJO to make availability request of files.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class AvailabilityRequest {

    /**
     * Checksums of file that are requested to be available
     */
    private Set<String> checksums;

    /**
     * Expiration date of the request
     */
    private OffsetDateTime expirationDate;

    /**
     * Default constructor
     */
    public AvailabilityRequest() {
    }

    /**
     * Constructor setting the parameters as attributes
     * @param expirationDate
     * @param checksums
     */
    public AvailabilityRequest(OffsetDateTime expirationDate, String... checksums) {
        this.checksums = Sets.newHashSet(checksums);
        this.expirationDate = expirationDate;
    }

    /**
     * @return the checksums
     */
    public Set<String> getChecksums() {
        return checksums;
    }

    /**
     * Set the checksums
     * @param checksums
     */
    public void setChecksums(Set<String> checksums) {
        this.checksums = checksums;
    }

    /**
     * @return the expiration date
     */
    public OffsetDateTime getExpirationDate() {
        return expirationDate;
    }

    /**
     * Set the expiration date
     * @param expirationDate
     */
    public void setExpirationDate(OffsetDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

}
