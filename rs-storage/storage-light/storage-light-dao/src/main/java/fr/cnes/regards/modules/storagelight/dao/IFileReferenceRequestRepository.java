package fr.cnes.regards.modules.storagelight.dao;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.cnes.regards.modules.storagelight.domain.FileReferenceRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceRequest;

/**
 * Repository handling JPA representation of metadata of files associated to aips
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IFileReferenceRequestRepository
        extends JpaRepository<FileReferenceRequest, Long>, JpaSpecificationExecutor<FileReference> {

    Page<FileReferenceRequest> findByDestinationStorage(String destinationStorage, Pageable pageable);

    Optional<FileReferenceRequest> findByMetaInfoChecksumAndDestinationStorage(String checksum, String storage);

    @Query("select destination.storage from FileReferenceRequest where status = :status")
    Set<String> findDestinationStoragesByStatus(@Param("status") FileReferenceRequestStatus status);

    Page<FileReferenceRequest> findAllByDestinationStorage(String storage, Pageable page);

    Page<FileReferenceRequest> findAllByDestinationStorageAndOwnersIn(String storage, Collection<String> owners,
            Pageable page);

    @Modifying
    @Query("update FileReferenceRequest frr set frr.status = :status where frr.id = :id")
    int updateStatus(@Param("status") FileReferenceRequestStatus status, @Param("id") Long id);

}
