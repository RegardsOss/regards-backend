package fr.cnes.regards.framework.modules.session.commons.dao;

import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA Repository for {@link SnapshotProcess}
 *
 * @author Iliana Ghazali
 **/
public interface ISnapshotProcessRepository extends JpaRepository<SnapshotProcess, Long> {

    Optional<SnapshotProcess> findByJobId(UUID jobId);

    Set<SnapshotProcess> findBySourceIn(Collection<String> sources);

    Set<SnapshotProcess> findByJobIdIn(List<UUID> jobIds);
}