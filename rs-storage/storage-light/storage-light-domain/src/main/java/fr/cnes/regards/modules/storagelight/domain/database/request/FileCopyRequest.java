package fr.cnes.regards.modules.storagelight.domain.database.request;

import javax.persistence.Column;
import javax.persistence.Embedded;
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

import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;

@Entity
@Table(name = "t_file_copy_request",
        indexes = { @Index(name = "idx_file_copy_request", columnList = "storage, checksum") },
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
    
    @Column(name = "request_id", nullable = false, length = 128)
    private String requestId;

    @Embedded
    private FileReferenceMetaInfo metaInfo;

    @Column(name = "storage_subdirectory", length = FileLocation.URL_MAX_LENGTH)
    private String storageSubDirectory;

    @Column(name = "storage", length = FileLocation.STORAGE_MAX_LENGTH)
    private String storage;
    
    @Column(name = "cache_request_id", length = 128)
    private String fileCacheRequestId;
    
    @Column(name = "storage_request_id", length = 128)
    private String fileStorageRequestId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FileRequestStatus status = FileRequestStatus.TODO;

    @Column(name = "error_cause", length = 512)
    private String errorCause;

    public FileCopyRequest() {
        super();
    }

	public Long getId() {
		return id;
	}

	public String getRequestId() {
		return requestId;
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

	public String getFileCacheRequestId() {
		return fileCacheRequestId;
	}

	public void setFileCacheRequestId(String fileCacheRequestId) {
		this.fileCacheRequestId = fileCacheRequestId;
	}

	public void setStatus(FileRequestStatus status) {
		this.status = status;
	}

	public void setErrorCause(String errorCause) {
		this.errorCause = errorCause;
	}

	public String getFileStorageRequestId() {
		return fileStorageRequestId;
	}

	public void setFileStorageRequestId(String fileStorageRequestId) {
		this.fileStorageRequestId = fileStorageRequestId;
	}
    
}
