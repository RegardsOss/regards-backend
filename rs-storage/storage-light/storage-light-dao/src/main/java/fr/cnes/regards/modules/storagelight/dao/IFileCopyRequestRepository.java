package fr.cnes.regards.modules.storagelight.dao;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import fr.cnes.regards.modules.storagelight.domain.database.request.FileCopyRequest;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileRequestStatus;

public interface IFileCopyRequestRepository
        extends JpaRepository<FileCopyRequest, Long>, JpaSpecificationExecutor<FileCopyRequest> {

    Page<FileCopyRequest> findByStatus(FileRequestStatus status, Pageable page);

    Optional<FileCopyRequest> findByFileCacheGroupId(String groupId);

    Optional<FileCopyRequest> findByFileStorageGroupId(String groupId);

    Optional<FileCopyRequest> findOneByMetaInfoChecksumAndStorage(String checksum, String storage);

    Optional<FileCopyRequest> findByMetaInfoChecksumAndFileCacheGroupId(String checksum, String groupId);

    Optional<FileCopyRequest> findByMetaInfoChecksumAndFileStorageGroupId(String checksum, String groupId);

    boolean existsByGroupId(String groupId);

    Set<FileCopyRequest> findByGroupId(String groupId);

    boolean existsByGroupIdAndStatusNot(String groupId, FileRequestStatus error);

}
