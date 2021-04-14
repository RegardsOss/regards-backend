package fr.cnes.regards.framework.modules.session.commons.dao;

import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author Iliana Ghazali
 **/
public interface ISnapshotProcessRepository extends JpaRepository<SnapshotProcess, Long> {

    Optional<SnapshotProcess> findBySource(String source);
}
