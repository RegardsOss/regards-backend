package fr.cnes.regards.modules.storage.domain.database.request;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "t_file_copy_request",
       indexes = { @Index(name = "idx_file_copy_request", columnList = "storage, checksum"),
                   @Index(name = "idx_file_copy_request_grp", columnList = "group_id"),
                   @Index(name = "idx_file_copy_request_cache_grp", columnList = "cache_group_id"),
                   @Index(name = "idx_file_copy_request_storage_grp", columnList = "storage_group_id") },
       uniqueConstraints = { @UniqueConstraint(name = "t_file_copy_request_checksum_storage",
                                               columnNames = { "checksum", "storage" }) })
public class FileCopyRequest {

    /**
     * Internal database unique identifier
     */
    @Id
    @SequenceGenerator(name = "fileStorageRequestSequence", initialValue = 1, sequenceName = "seq_file_storage_request")
    @GeneratedValue(generator = "fileStorageRequestSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * Business identifier to regroup file requests.
     */
    @Column(name = "group_id", nullable = false, length = 128)
    private String groupId;

    @Embedded
    private FileReferenceMetaInfo metaInfo;

    @Column(name = "storage_subdirectory", length = FileLocation.URL_MAX_LENGTH)
    private String storageSubDirectory;

    @Column(name = "storage", length = FileLocation.STORAGE_MAX_LENGTH)
    private String storage;

    @Column(name = "cache_group_id", length = 128)
    private String fileCacheGroupId;

    @Column(name = "storage_group_id", length = 128)
    private String fileStorageGroupId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FileRequestStatus status = FileRequestStatus.TO_DO;

    @Column(name = "error_cause", length = 512)
    private String errorCause;

    @Column(name = "creation_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private final OffsetDateTime creationDate;

    @Column(name = "session_owner")
    private String sessionOwner;

    @Column(name = "session_name")
    private String session;

    public FileCopyRequest() {
        super();
        this.creationDate = OffsetDateTime.now();
    }

    public FileCopyRequest(String groupId,
                           FileReferenceMetaInfo metaInfo,
                           String storageSubDirectory,
                           String storage,
                           String sessionOwner,
                           String session) {
        super();
        this.groupId = groupId;
        this.metaInfo = metaInfo;
        this.storageSubDirectory = storageSubDirectory;
        this.storage = storage;
        this.status = FileRequestStatus.TO_DO;
        this.creationDate = OffsetDateTime.now();
        this.sessionOwner = sessionOwner;
        this.session = session;
    }

    public Long getId() {
        return id;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getStorageSubDirectory() {
        return storageSubDirectory;
    }

    public String getStorage() {
        return storage;
    }

    public FileReferenceMetaInfo getMetaInfo() {
        return metaInfo;
    }

    public FileRequestStatus getStatus() {
        return status;
    }

    public String getErrorCause() {
        return errorCause;
    }

    public String getFileCacheGroupId() {
        return fileCacheGroupId;
    }

    public void setFileCacheGroupId(String fileCacheGroupId) {
        this.fileCacheGroupId = fileCacheGroupId;
    }

    public void setStatus(FileRequestStatus status) {
        this.status = status;
    }

    public void setErrorCause(String errorCause) {
        if (errorCause != null && errorCause.length() > 512) {
            this.errorCause = errorCause.substring(0, 511);
        } else {
            this.errorCause = errorCause;
        }
    }

    public String getFileStorageGroupId() {
        return fileStorageGroupId;
    }

    public void setFileStorageGroupId(String fileCacheGroupId) {
        this.fileStorageGroupId = fileCacheGroupId;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setMetaInfo(FileReferenceMetaInfo metaInfo) {
        this.metaInfo = metaInfo;
    }

    public String getSessionOwner() {
        return sessionOwner;
    }

    public void setSessionOwner(String sessionOwner) {
        this.sessionOwner = sessionOwner;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }
}
