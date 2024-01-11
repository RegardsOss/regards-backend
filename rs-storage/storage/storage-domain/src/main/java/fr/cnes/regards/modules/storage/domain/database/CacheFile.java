package fr.cnes.regards.modules.storage.domain.database;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.converter.MimeTypeConverter;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import org.springframework.util.MimeType;

import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Database definition of the table containing the files currently in internal or external cache.
 *
 * @author Sébastien Binda
 */
@Entity
@Table(name = "t_cache_file",
       indexes = { @Index(name = "idx_cache_file_checksum", columnList = "checksum") },
       uniqueConstraints = { @UniqueConstraint(name = "uk_cache_file_checksum", columnNames = "checksum") })
public class CacheFile {

    /**
     * Technical unique identifier for the cache file in database
     */
    @Id
    @SequenceGenerator(name = "cacheFileSequence", initialValue = 1, sequenceName = "seq_cache_file")
    @GeneratedValue(generator = "cacheFileSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * The cache file checksum (unique identifier of cache file)
     */
    @NotNull
    @Column(length = FileReferenceMetaInfo.CHECKSUM_MAX_LENGTH, unique = true)
    private String checksum;

    /**
     * The cache file size in Bytes
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
     * Location of file into the cache
     */
    @Column
    private URL location;

    /**
     * Expiration date of the file into the cache
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
    @CollectionTable(name = "ta_cache_file_group_ids",
                     joinColumns = @JoinColumn(name = "cache_file_id",
                                               foreignKey = @ForeignKey(name = "fk_ta_cache_file_request_ids_t_file_cache")))
    private final Set<String> groupIds = Sets.newHashSet();

    /**
     * True when the file is stored inside internal REGARDS cache, false when file is stored inside external cache.
     * By default, the file is stored inside internal REGARDS cache.
     */
    @Column(name = "internal_cache")
    private boolean internalCache = true;

    /**
     * Business identifier of plugin to access file in external cache.
     */
    @Column(name = "external_cache_plugin", nullable = true)
    private String externalCachePlugin;

    /**
     * Default constructor
     */
    public CacheFile() {
        fileSize = 0L;
    }

    protected CacheFile(String checksum,
                        Long fileSize,
                        String fileName,
                        MimeType mimeType,
                        URL location,
                        OffsetDateTime expirationDate,
                        String groupId,
                        String type,
                        boolean internalCache,
                        @Nullable String externalCachePlugin) {
        this.checksum = checksum;
        this.fileSize = fileSize;
        this.location = location;
        this.expirationDate = expirationDate;
        this.groupIds.add(groupId);
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.type = type;
        this.internalCache = internalCache;
        this.externalCachePlugin = externalCachePlugin;
    }

    public boolean isInternalCache() {
        return internalCache;
    }

    public @Nullable String getExternalCachePlugin() {
        return externalCachePlugin;
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
        return "CacheFile["
               + "id="
               + id
               + ", checksum="
               + checksum
               + ", fileSize="
               + fileSize
               + ", fileName="
               + fileName
               + ", mimeType="
               + mimeType
               + ", type="
               + type
               + ", location="
               + location
               + ", expirationDate="
               + expirationDate
               + ", internalCache="
               + internalCache
               + ", externalCachePlugin="
               + externalCachePlugin
               + "]";
    }

    /**
     * Build a file managed by an internal cache.
     */
    public static CacheFile buildFileInternalCache(String checksum,
                                                   Long fileSize,
                                                   String fileName,
                                                   MimeType mimeType,
                                                   URL location,
                                                   OffsetDateTime expirationDate,
                                                   String groupId,
                                                   String type) {
        return new CacheFile(checksum,
                             fileSize,
                             fileName,
                             mimeType,
                             location,
                             expirationDate,
                             groupId,
                             type,
                             true,
                             null);

    }

    /**
     * Build a file managed by an external cache.
     */
    public static CacheFile buildFileExternalCache(String checksum,
                                                   Long fileSize,
                                                   String fileName,
                                                   MimeType mimeType,
                                                   URL location,
                                                   OffsetDateTime expirationDate,
                                                   String groupId,
                                                   String type,
                                                   String externalCachePlugin) {
        return new CacheFile(checksum,
                             fileSize,
                             fileName,
                             mimeType,
                             location,
                             expirationDate,
                             groupId,
                             type,
                             false,
                             externalCachePlugin);

    }
}
