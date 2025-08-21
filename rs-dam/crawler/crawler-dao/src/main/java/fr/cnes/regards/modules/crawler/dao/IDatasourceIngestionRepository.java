package fr.cnes.regards.modules.crawler.dao;

import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.domain.IngestionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Interface for a JPA auto-generated CRUD repository to handle access to {@link DatasourceIngestion} entities.
 *
 * @author oroussel
 */
public interface IDatasourceIngestionRepository extends JpaRepository<DatasourceIngestion, String> {

    /**
     * Find all DatasourceIngestion whom next planned ingest date is less than given date
     * and with given status (usually 'STARTED')
     *
     * @return list of DatasourceIngestion entities matching the criteria, result list may be empty
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    List<DatasourceIngestion> findByNextPlannedIngestDateLessThanAndStatusNot(OffsetDateTime limitDate,
                                                                              IngestionStatus status);

    /**
     * Find all DatasourceIngestion ready to be ingested
     */
    default List<DatasourceIngestion> findAllReady(OffsetDateTime limitDate) {
        return findByNextPlannedIngestDateLessThanAndStatusNot(limitDate, IngestionStatus.STARTED);
    }
}
