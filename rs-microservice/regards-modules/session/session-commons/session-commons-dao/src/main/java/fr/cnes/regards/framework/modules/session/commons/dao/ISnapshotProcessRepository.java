package fr.cnes.regards.framework.modules.session.commons.dao;

import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * JPA Repository for {@link SnapshotProcess}
 *
 * @author Iliana Ghazali
 **/
public interface ISnapshotProcessRepository extends JpaRepository<SnapshotProcess, Long> {

    Optional<SnapshotProcess> findBySource(String source);

    Set<SnapshotProcess> findBySourceIn(Collection<String> sources);

    Set<SnapshotProcess> findByJobIdIn(List<UUID> jobIds);

    @Modifying
    @Query("DELETE FROM SnapshotProcess p where p.source NOT IN (SELECT s.source FROM SessionStep s) "
            + "AND (p.lastUpdateDate IS NULL OR p.lastUpdateDate <= ?1)")
    int deleteUnusedProcess(OffsetDateTime limitDate);
}