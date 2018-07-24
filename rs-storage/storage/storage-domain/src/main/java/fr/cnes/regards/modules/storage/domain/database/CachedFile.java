package fr.cnes.regards.modules.storage.domain.database;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import java.net.URL;
import java.time.OffsetDateTime;

/**
 * Representation of a StorageDataFile which is in the cache
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Entity
@Table(name = "t_cached_file", indexes = { @Index(name = "idx_cached_file_checksum", columnList = "checksum"),
        @Index(name = "idx_cached_file_state", columnList = "state") },
        uniqueConstraints = { @UniqueConstraint(name = "uk_cached_file_checksum", columnNames = "checksum") })
public class CachedFile {

    /**
     * db id
     */
    @Id
    @SequenceGenerator(name = "cachedFileSequence", initialValue = 1, sequenceName = "seq_cached_file")
    @GeneratedValue(generator = "cachedFileSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * The cached file checksum
     */
    @NotNull
    @Column(length = 128)
    private String checksum;

    /**
     * the cached file size
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * location into the cache
     */
    @Column
    private URL location;

    /**
     * expiration date of the file into the cache
     */
    @Column
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime expiration;

    /**
     * Date of the last request to make the file available.
     */
    @Column(name = "last_request_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastRequestDate;

    /**
     * The cached file state
     */
    @Column
    @Enumerated(EnumType.STRING)
    private CachedFileState state;

    /**
     * Causes why a file could not be set into the cache
     */
    @Column(length = 512, name = "failure_cause")
    private String failureCause;

    /**
     * Default constructor
     */
    public CachedFile() {
    }

    /**
     * Constructor initializing the cached file from the parameters
     * @param df
     * @param expirationDate
     * @param fileState
     */
    public CachedFile(StorageDataFile df, OffsetDateTime expirationDate, CachedFileState fileState) {
        checksum = df.getChecksum();
        fileSize = df.getFileSize();
        expiration = expirationDate;
        lastRequestDate = OffsetDateTime.now();
        state = fileState;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Set the id
     * @param id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the location
     */
    public URL getLocation() {
        return location;
    }

    /**
     * Set the location
     * @param location
     */
    public void setLocation(URL location) {
        this.location = location;
    }

    /**
     * @return the expiration date
     */
    public OffsetDateTime getExpiration() {
        return expiration;
    }

    /**
     * Set the expiration date
     * @param expiration
     */
    public void setExpiration(OffsetDateTime expiration) {
        this.expiration = expiration;
    }

    /**
     * @return the state
     */
    public CachedFileState getState() {
        return state;
    }

    /**
     * Set the state
     * @param state
     */
    public void setState(CachedFileState state) {
        this.state = state;
    }

    /**
     * @return the failure cause
     */
    public String getFailureCause() {
        return failureCause;
    }

    /**
     * Set the failure cause
     * @param failureCause
     */
    public void setFailureCause(String failureCause) {
        this.failureCause = failureCause;
    }

    /**
     * @return the last request date
     */
    public OffsetDateTime getLastRequestDate() {
        return lastRequestDate;
    }

    /**
     * Set last request date
     * @param pLastRequestDate
     */
    public void setLastRequestDate(OffsetDateTime pLastRequestDate) {
        lastRequestDate = pLastRequestDate;
    }

    /**
     * @return the file size
     */
    public Long getFileSize() {
        return fileSize;
    }

    /**
     * Set the file size
     * @param pFileSize
     */
    public void setFileSize(Long pFileSize) {
        fileSize = pFileSize;
    }

    /**
     * @return the checksum
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * Set the checksum
     * @param pChecksum
     */
    public void setChecksum(String pChecksum) {
        checksum = pChecksum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CachedFile that = (CachedFile) o;

        return checksum.equals(that.checksum);
    }

    @Override
    public int hashCode() {
        return checksum.hashCode();
    }

    @Override
    public String toString() {
        return "CachedFile[id=" + id + ", checksum=" + checksum + "]";
    }
}
