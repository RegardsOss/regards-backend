package fr.cnes.regards.modules.storagelight.domain.database;

import java.net.URL;
import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;

/**
 * Representation of a StorageDataFile which is in the cache
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Entity
@Table(name = "t_cache_file", indexes = { @Index(name = "idx_cache_file_checksum", columnList = "checksum") },
        uniqueConstraints = { @UniqueConstraint(name = "uk_cache_file_checksum", columnNames = "checksum") })
public class CacheFile {

    /**
     * db id
     */
    @Id
    @SequenceGenerator(name = "cacheFileSequence", initialValue = 1, sequenceName = "seq_cache_file")
    @GeneratedValue(generator = "cacheFileSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * The cache file checksum
     */
    @NotNull
    @Column(length = FileReferenceMetaInfo.CHECKSUM_MAX_LENGTH)
    private String checksum;

    /**
     * the cache file size, this field is final because it should mirror the information from StorageDataFile
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
    @Column(name = "expiration_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime expirationDate;

    /**
     * Date of the last request to make the file available.
     */
    @Column(name = "last_request_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastRequestDate;

    /**
     * Default constructor
     */
    public CacheFile() {
        fileSize = 0L;
    }

    /**
     * Constructor initializing the cache file from the parameters
     * @param df
     * @param expirationDate
     */
    public CacheFile(String checksum, Long fileSize, URL location, OffsetDateTime expirationDate) {
        this.checksum = checksum;
        this.fileSize = fileSize;
        this.location = location;
        this.expirationDate = expirationDate;
        this.lastRequestDate = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public URL getLocation() {
        return location;
    }

    public void setLocation(URL location) {
        this.location = location;
    }

    public OffsetDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(OffsetDateTime expiration) {
        this.expirationDate = expiration;
    }

    public OffsetDateTime getLastRequestDate() {
        return lastRequestDate;
    }

    public void setLastRequestDate(OffsetDateTime pLastRequestDate) {
        lastRequestDate = pLastRequestDate;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String pChecksum) {
        checksum = pChecksum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        CacheFile that = (CacheFile) o;

        return checksum.equals(that.checksum);
    }

    @Override
    public int hashCode() {
        return checksum.hashCode();
    }

    @Override
    public String toString() {
        return "CacheFile[id=" + id + ", checksum=" + checksum + "]";
    }
}
