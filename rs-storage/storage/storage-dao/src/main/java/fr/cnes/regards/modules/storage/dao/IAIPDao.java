package fr.cnes.regards.modules.storage.dao;

import java.time.OffsetDateTime;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IAIPDao {

    AIP save(AIP toSave, PluginConfiguration dataStorageUsed);

    Page<AIP> findAllByState(AIPState state, Pageable pageable);

    Page<AIP> findAllBySubmissionDateAfter(OffsetDateTime submissionAfter, Pageable pageable);

    Page<AIP> findAllByLastEventDateBefore(OffsetDateTime lastEventBefore, Pageable pageable);

    Page<AIP> findAllByStateAndLastEventDateBefore(AIPState state, OffsetDateTime lastEventBefore, Pageable pageable);

    Set<AIP> findAllByIpIdStartingWith(String ipIdWithoutVersion);

    Page<AIP> findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(AIPState state, OffsetDateTime submissionAfter,
            OffsetDateTime lastEventBefore, Pageable pageable);

    Page<AIP> findAllByStateAndSubmissionDateAfter(AIPState state, OffsetDateTime submissionAfter, Pageable pageable);

    Page<AIP> findAllBySubmissionDateAfterAndLastEventDateBefore(OffsetDateTime submissionAfter,
            OffsetDateTime lastEventBefore, Pageable pageable);
}
