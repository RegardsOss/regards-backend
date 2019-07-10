package fr.cnes.regards.modules.dao;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.cnes.regards.modules.storagelight.domain.FileReferenceRequestStatus;
import fr.cnes.regards.modules.storagelight.domain.StorageMonitoringAggregation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;

/**
 * Repository handling JPA representation of metadata of files associated to aips
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IFileReferenceRepository
        extends JpaRepository<FileReference, Long>, JpaSpecificationExecutor<FileReference> {

    /**
     * Retrieve one {@link FileReference} by his checksum and storage
     * @param checksum
     * @param storage
     * @return {@link FileReference}
     */
    Optional<FileReference> findByChecksumAndStorage(String checksum, String storage);

    /**
     * Retrieve all storage location of the referenced file for the given state
     * @param state
     * @return Collection of storage as String
     */
    Set<String> findStoragesByState(FileReferenceRequestStatus state);

    @Query("select storage as dataStorage, sum(fileSize) as usedSize, count(*) as numberOfFileReference, max(id) as lastFileReferenceId"
            + " from FileReference group by storage")
    Collection<StorageMonitoringAggregation> getTotalFileSizeAggregation();

    @Query("select storage as dataStorage, sum(fileSize) as usedSize, count(*) as numberOfFileReference, max(id) as lastFileReferenceId"
            + " from FileReference where id >= :id")
    Collection<StorageMonitoringAggregation> getTotalFileSizeAggregation(@Param("id") Long fromFileReferenceId);

}
