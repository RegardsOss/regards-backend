package fr.cnes.regards.framework.modules.session.sessioncommons.dao;

import fr.cnes.regards.framework.modules.session.sessioncommons.domain.SnapshotProcess;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Iliana Ghazali
 **/
public interface ISnapshotProcessRepository extends JpaRepository<SnapshotProcess, Long> {

    Optional<SnapshotProcess> findBySource(String source);

    Optional<SnapshotProcess> findByJobId(UUID jobId);
}
