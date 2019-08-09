package fr.cnes.regards.modules.storagelight.dao;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.cnes.regards.modules.storagelight.domain.FileRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;

/**
 * Repository handling JPA representation of metadata of files associated to aips
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IFileStorageRequestRepository extends JpaRepository<FileStorageRequest, Long> {

    Page<FileStorageRequest> findByStorage(String storage, Pageable pageable);

    Optional<FileStorageRequest> findByMetaInfoChecksumAndStorage(String checksum, String storage);

    @Query("select storage from FileStorageRequest where status = :status")
    Set<String> findStoragesByStatus(@Param("status") FileRequestStatus status);

    Page<FileStorageRequest> findAllByStorage(String storage, Pageable page);

    Page<FileStorageRequest> findAllByStorageAndOwnersIn(String storage, Collection<String> owners, Pageable page);

    @Modifying
    @Query("update FileStorageRequest fsr set fsr.status = :status where fsr.id = :id")
    int updateStatus(@Param("status") FileRequestStatus status, @Param("id") Long id);

    boolean existsByRequestIds(String requestId);

    Set<FileStorageRequest> findByRequestIds(String requestId);

    Set<FileStorageRequest> findByRequestIdsAndStatus(String requestId, FileRequestStatus error);

    Page<FileStorageRequest> findByOwnersInAndStatus(Collection<String> owners, FileRequestStatus error, Pageable page);

}
