package fr.cnes.regards.modules.storagelight.dao;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.StorageMonitoringAggregation;

/**
 *
 * @author Sébastien Binda
 */
public interface IFileReferenceRepository
        extends JpaRepository<FileReference, Long>, JpaSpecificationExecutor<FileReference> {

    Page<FileReference> findByLocationStorage(String storage, Pageable page);

    Optional<FileReference> findByMetaInfoChecksumAndLocationStorage(String checksum, String storage);

    Set<FileReference> findByMetaInfoChecksum(String checksum);

    @Query("select fr.location.storage as storage, sum(fr.metaInfo.fileSize) as usedSize, count(*) as numberOfFileReference, max(fr.id) as lastFileReferenceId"
            + " from FileReference fr group by storage")
    Collection<StorageMonitoringAggregation> getTotalFileSizeAggregation();

    @Query("select fr.location.storage as storage, sum(fr.metaInfo.fileSize) as usedSize, count(*) as numberOfFileReference, max(fr.id) as lastFileReferenceId"
            + " from FileReference fr where fr.id > :id group by fr.location.storage")
    Collection<StorageMonitoringAggregation> getTotalFileSizeAggregation(@Param("id") Long fromFileReferenceId);

    Long countByLocationStorage(String storage);

}
