package fr.cnes.regards.modules.storagelight.dao;


import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileCopyRequest;

public interface IFileCopyRequestRepository extends JpaRepository<FileCopyRequest, Long>, JpaSpecificationExecutor<FileCopyRequest> {
	
	Page<FileCopyRequest> findByStatus(FileRequestStatus status, Pageable page);
	
	Optional<FileCopyRequest> findByFileCacheRequestId(String fileCacheRequestId);
	
	Optional<FileCopyRequest> findByFileStorageRequestId(String storageRequestId);
	
	Optional<FileCopyRequest> findOneByMetaInfoChecksumAndStorage(String checksum, String storage);

}
