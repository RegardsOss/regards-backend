package fr.cnes.regards.modules.storage.dao;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPDataBase;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class AIPDao implements IAIPDao {

    private final IAIPDataBaseRepository repo;

    public AIPDao(IAIPDataBaseRepository repo) {
        this.repo = repo;
    }

    @Override
    public AIP save(AIP toSave) {
        AIPDataBase toSaveInDb = new AIPDataBase(toSave);
        Optional<AIPDataBase> fromDb = repo.findOneByIpId(toSave.getId().toString());
        if (fromDb.isPresent()) {
            toSaveInDb.setId(fromDb.get().getId());
        }
        AIPDataBase saved = repo.save(toSaveInDb);
        return buildAipFromAIPDataBase(saved);
    }

    /**
     * Build the {@link AIP} as we are using it into the system from the {@link AIPDataBase} saved in db.
     * @param fromDb {@link AIPDataBase}
     * @return {@link AIP}
     */
    private AIP buildAipFromAIPDataBase(AIPDataBase fromDb) {
        AIP aip = fromDb.getAip();
        // as fromDb.getAip gives us the aip serialized, we have to restore ignored attributes as state
        aip.setState(fromDb.getState());
        return aip;
    }

    @Override
    public Page<AIP> findAllByState(AIPState state, Pageable pageable) {
        Page<AIPDataBase> fromDb = repo.findAllByStateIn(state, pageable);
        return fromDb.map(this::buildAipFromAIPDataBase);

    }

    @Override
    public Page<AIP> findAllBySubmissionDateAfter(OffsetDateTime submissionAfter, Pageable pageable) {
        return repo.findAllBySubmissionDateAfter(submissionAfter, pageable).map(this::buildAipFromAIPDataBase);
    }

    @Override
    public Page<AIP> findAllByLastEventDateBefore(OffsetDateTime lastEventBefore, Pageable pageable) {
        return repo.findAllByLastEventDateBefore(lastEventBefore, pageable).map(this::buildAipFromAIPDataBase);
    }

    @Override
    public Page<AIP> findAllByStateAndLastEventDateBefore(AIPState state, OffsetDateTime lastEventBefore,
            Pageable pageable) {
        return repo.findAllByStateAndLastEventDateBefore(state, lastEventBefore, pageable)
                .map(this::buildAipFromAIPDataBase);
    }

    @Override
    public Set<AIP> findAllByIpIdStartingWith(String ipIdWithoutVersion) {
        return repo.findAllByIpIdStartingWith(ipIdWithoutVersion).stream().map(this::buildAipFromAIPDataBase)
                .collect(Collectors.toSet());
    }

    @Override
    public Page<AIP> findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(AIPState state,
            OffsetDateTime submissionAfter, OffsetDateTime lastEventBefore, Pageable pageable) {
        return repo
                .findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(state, submissionAfter, lastEventBefore,
                                                                            pageable)
                .map(this::buildAipFromAIPDataBase);
    }

    @Override
    public Page<AIP> findAllByStateAndSubmissionDateAfter(AIPState state, OffsetDateTime submissionAfter,
            Pageable pageable) {
        return repo.findAllByStateAndSubmissionDateAfter(state, submissionAfter, pageable)
                .map(this::buildAipFromAIPDataBase);
    }

    @Override
    public Page<AIP> findAllBySubmissionDateAfterAndLastEventDateBefore(OffsetDateTime submissionAfter,
            OffsetDateTime lastEventBefore, Pageable pageable) {
        return repo.findAllBySubmissionDateAfterAndLastEventDateBefore(submissionAfter, lastEventBefore, pageable)
                .map(this::buildAipFromAIPDataBase);
    }

    @Override
    public Set<AIP> findAllByStateService(AIPState state) {
        return repo.findAllByStateIn(state).stream().map(this::buildAipFromAIPDataBase).collect(Collectors.toSet());
    }

    @Override
    public Optional<AIP> findOneByIpId(String ipId) {
        Optional<AIPDataBase> aipDatabase = repo.findOneByIpId(ipId);
        if (aipDatabase.isPresent()) {
            return Optional.of(buildAipFromAIPDataBase(aipDatabase.get()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void deleteAll() {
        repo.deleteAll();
    }

    @Override
    public Set<AIP> findAllByStateInService(AIPState... states) {
        return repo.findAllByStateIn(states).stream().map(this::buildAipFromAIPDataBase).collect(Collectors.toSet());
    }

    @Override
    public void remove(AIP associatedAIP) {
        Optional<AIPDataBase> opt = repo.findOneByIpId(associatedAIP.getId().toString());
        if(opt.isPresent()) {
            repo.delete(opt.get());
        }
    }

    @Override
    public Set<AIP> findAllByIpIdIn(Collection<String> ipIds) {
        return repo.findAllByIpIdIn(ipIds).stream().map(this::buildAipFromAIPDataBase).collect(Collectors.toSet());
    }

    @Override
    public Set<AIP> findAllByTags(String tag) {
        return repo.findAllByTags(tag).stream().map(this::buildAipFromAIPDataBase).collect(Collectors.toSet());
    }

    @Override
    public Page<AIP> findAll(Pageable pPageable) {
        return repo.findAll(pPageable).map(this::buildAipFromAIPDataBase);
    }

}
