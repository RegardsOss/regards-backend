package fr.cnes.regards.modules.storage.domain.database;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.springframework.util.MimeType;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.converter.MimeTypeConverter;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;

/**
 * Database definition of the table containing the files currently in cache.
 *
 * @author Sébastien Binda
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
     * the cache file size
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * The cache file name
     */
    @Column(name = "filename")
    private String fileName;

    @Column(nullable = false, name = "mime_type")
    @Convert(converter = MimeTypeConverter.class)
    private MimeType mimeType;

    /**
     * The cache file type
     */
    @Column(name = "type")
    private String type;

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
     * Business identifier to regroup file cache requests.
     * It is used to know who asked for this file availability in cache.
     */
    @Column(name = "group_id", nullable = false, length = 128)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ta_cache_file_group_ids", joinColumns = @JoinColumn(name = "cache_file_id",
            foreignKey = @ForeignKey(name = "fk_ta_cache_file_request_ids_t_file_cache")))
    private final Set<String> groupIds = Sets.newHashSet();

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
     * @param type
     */
    public CacheFile(String checksum, Long fileSize, String fileName, MimeType mimeType, URL location,
                     OffsetDateTime expirationDate, String groupId, String type) {
        this.checksum = checksum;
        this.fileSize = fileSize;
        this.location = location;
        this.expirationDate = expirationDate;
        this.groupIds.add(groupId);
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.type = type;
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

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public void addGroupId(String groupId) {
        this.groupIds.add(groupId);
    }

    public Set<String> getGroupIds() {
        return groupIds;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public MimeType getMimeType() {
        return mimeType;
    }

    public void setMimeType(MimeType mimeType) {
        this.mimeType = mimeType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
