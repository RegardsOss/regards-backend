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
@Table(name = "t_cached_file", indexes = { @Index(name = "idx_cached_file_checksum", columnList = "checksum") },
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
    @Column(length = FileReferenceMetaInfo.CHECKSUM_MAX_LENGTH)
    private String checksum;

    /**
     * the cached file size, this field is final because it should mirror the information from StorageDataFile
     */
    @Column(name = "file_size")
    private final Long fileSize;

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
     * Default constructor
     */
    public CachedFile() {
        fileSize = 0L;
    }

    /**
     * Constructor initializing the cached file from the parameters
     * @param df
     * @param expirationDate
     */
    public CachedFile(FileReference fileRef, OffsetDateTime expirationDate) {
        checksum = fileRef.getMetaInfo().getChecksum();
        fileSize = fileRef.getMetaInfo().getFileSize();
        expiration = expirationDate;
        lastRequestDate = OffsetDateTime.now();
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

    public OffsetDateTime getExpiration() {
        return expiration;
    }

    public void setExpiration(OffsetDateTime expiration) {
        this.expiration = expiration;
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
