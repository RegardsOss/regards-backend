package fr.cnes.regards.modules.storage.dao;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPDataBase;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class AIPDao implements IAIPDao {

    private IAIPDataBaseRepository repo;

    public AIPDao(IAIPDataBaseRepository repo) {
        this.repo = repo;
    }

    @Override
    public AIP save(AIP toSave) {
        AIPDataBase saved = repo.save(new AIPDataBase(toSave));
        return reconstructAip(saved);
    }

    /**
     * Allows us to reconstruct the AIP as we are using it into the system.
     * @param fromDb
     * @return
     */
    private AIP reconstructAip(AIPDataBase fromDb) {
        AIP aip=fromDb.getAip();
        // as fromDb.getAip gives us the aip serialized, we have to restore ignored attributes as state
        aip.setState(fromDb.getState());
        return aip;
    }

    @Override
    public Page<AIP> findAllByState(AIPState state, Pageable pageable) {
        Page<AIPDataBase> fromDb = repo.findAllByState(state, pageable);
        return fromDb.map(this::reconstructAip);

    }

    @Override
    public Page<AIP> findAllBySubmissionDateAfter(OffsetDateTime submissionAfter, Pageable pageable) {
        return repo.findAllBySubmissionDateAfter(submissionAfter, pageable).map(this::reconstructAip);
    }

    @Override
    public Page<AIP> findAllByLastEventDateBefore(OffsetDateTime lastEventBefore, Pageable pageable) {
        return repo.findAllByLastEventDateBefore(lastEventBefore, pageable).map(this::reconstructAip);
    }

    @Override
    public Page<AIP> findAllByStateAndLastEventDateBefore(AIPState state, OffsetDateTime lastEventBefore,
            Pageable pageable) {
        return repo.findAllByStateAndLastEventDateBefore(state, lastEventBefore, pageable).map(this::reconstructAip);
    }

    @Override
    public Set<AIP> findAllByIpIdStartingWith(String ipIdWithoutVersion) {
        return repo.findAllByIpIdStartingWith(ipIdWithoutVersion).stream().map(this::reconstructAip)
                .collect(Collectors.toSet());
    }

    @Override
    public Page<AIP> findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(AIPState state,
            OffsetDateTime submissionAfter, OffsetDateTime lastEventBefore, Pageable pageable) {
        return repo.findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(state, submissionAfter, lastEventBefore,
                                                                               pageable).map(this::reconstructAip);
    }

    @Override
    public Page<AIP> findAllByStateAndSubmissionDateAfter(AIPState state, OffsetDateTime submissionAfter,
            Pageable pageable) {
        return repo.findAllByStateAndSubmissionDateAfter(state, submissionAfter, pageable).map(this::reconstructAip);
    }

    @Override
    public Page<AIP> findAllBySubmissionDateAfterAndLastEventDateBefore(OffsetDateTime submissionAfter,
            OffsetDateTime lastEventBefore, Pageable pageable) {
        return repo.findAllBySubmissionDateAfterAndLastEventDateBefore(submissionAfter, lastEventBefore, pageable)
                .map(this::reconstructAip);
    }

    @Override
    public Set<AIP> findAllByStateService(AIPState state) {
        return repo.findAllByState(state).stream().map(this::reconstructAip).collect(Collectors.toSet());
    }

}
