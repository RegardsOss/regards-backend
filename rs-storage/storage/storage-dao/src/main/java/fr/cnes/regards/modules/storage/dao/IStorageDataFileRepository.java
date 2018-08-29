package fr.cnes.regards.modules.storage.dao;

import javax.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;
import fr.cnes.regards.modules.storage.domain.database.MonitoringAggregation;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * Repository handling JPA representation of metadata of files associated to aips
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IStorageDataFileRepository extends JpaRepository<StorageDataFile, Long> {

    /**
     * Find all data files which state is the given one and which are associated to the provided aip entity
     * @param stored
     * @param aipEntity
     * @return data files which state is the given one and which are associated to the provided aip entity
     */
    @EntityGraph(value = "graph.datafile.full")
    Set<StorageDataFile> findAllByStateAndAipEntity(DataFileState stored, AIPEntity aipEntity);

    /**
     * Find all data files which state is the given one
     * @param stored
     * @return data files which state is the given one
     */
    @EntityGraph(value = "graph.datafile.full")
    Set<StorageDataFile> findAllByState(DataFileState stored);

    @EntityGraph(value = "graph.datafile.full")
    Page<StorageDataFile> findAllByState(DataFileState state, Pageable pageable);

    /**
     * Find all {@link StorageDataFile}s associated to the given aip entity
     * @param aipEntity
     * @return {@link StorageDataFile}s
     */
    @EntityGraph(value = "graph.datafile.full")
    Set<StorageDataFile> findAllByAipEntity(AIPEntity aipEntity);

    /**
     * Find all {@link StorageDataFile}s associated to the given aip entities
     * @param aipEntities some AipEntities
     * @return {@link StorageDataFile}s
     */
    @EntityGraph(value = "graph.datafile.full")
    Set<StorageDataFile> findAllByAipEntityIn(Collection<AIPEntity> aipEntities);

    /**
     * Find the data file associated to the given aip entity and which type the provided one
     * @param aipEntity
     * @param dataType
     * @return the data file wrapped into an optional to avoid nulls
     */
    @EntityGraph(value = "graph.datafile.full")
    Optional<StorageDataFile> findByAipEntityAndDataType(AIPEntity aipEntity, DataType dataType);

    /**
     * Retrieve a data file by its id
     * @param dataFileId
     * @return the data file wrapped into an optional to avoid nulls
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<StorageDataFile> findLockedOneById(Long dataFileId);

    /**
     * Retrieve a data file by its id
     * @param dataFileId
     * @return the data file wrapped into an optional to avoid nulls
     */
    @EntityGraph(value = "graph.datafile.full")
    Optional<StorageDataFile> findOneById(Long dataFileId);

    /**
     * Find all data files of which checksum is one of the provided ones
     * @param checksums
     * @return data files of which checksum is one of the provided ones
     */
    @EntityGraph(value = "graph.datafile.full")
    Set<StorageDataFile> findAllByChecksumIn(Set<String> checksums);

    /**
     * Find all data files which state is the provided one and that are associated to at least one of the provided aip entities
     * @param dataFileState
     * @param aipEntities
     * @return data files which state is the provided one and that are associated to at least one of the provided aip entities
     */
    @EntityGraph(value = "graph.datafile.full")
    Set<StorageDataFile> findAllByStateAndAipEntityIn(DataFileState dataFileState, Collection<AIPEntity> aipEntities);

    /**
     * Calculate the monitoring information on all data files of the database
     * @return the monitoring aggregation
     */
    @Query("select pds.id as dataStorageUsedId, sum(df.fileSize) as usedSize from StorageDataFile df join df.prioritizedDataStorages pds"
            + " where df.state = 'STORED' group by pds.id")
    Collection<MonitoringAggregation> getMonitoringAggregation();

    /**
     * Get one StorageDataFile stored by given prioritized data storage
     * @param pdsId prioritized data storage id
     * @return a StorageDataFile
     */
    StorageDataFile findTopByPrioritizedDataStoragesId(Long pdsId);

    /**
     * Count number of files stored for the given plugin conf
     * @param pdsId {@link PrioritizedDataStorage} identifier
     */
    long countByPrioritizedDataStoragesId(Long pdsId);

    long countByChecksum(String checksum);
}
