package fr.cnes.regards.modules.storage.dao;

import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link IAIPDao}
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class AIPDao implements IAIPDao {

    /**
     * {@link IAIPEntityRepository} instance
     */
    private final IAIPEntityRepository repo;

    private ICustomizedAIPEntityRepository custoRepo;

    /**
     * Constructor setting the parameter as attribute
     * @param repo
     */
    public AIPDao(IAIPEntityRepository repo, ICustomizedAIPEntityRepository custoRepo) {
        this.repo = repo;
        this.custoRepo = custoRepo;
    }

    @Override
    public AIP save(AIP toSave, AIPSession aipSession) {
        AIPEntity toSaveInDb = new AIPEntity(toSave, aipSession);
        Optional<AIPEntity> fromDb = repo.findOneByIpId(toSave.getId().toString());
        if (fromDb.isPresent()) {
            toSaveInDb.setId(fromDb.get().getId());
        }
        AIPEntity saved = repo.save(toSaveInDb);
        return buildAipFromAIPEntity(saved);
    }

    /**
     * Build the {@link AIP} as we are using it into the system from the {@link AIPEntity} saved in db.
     * @param fromDb {@link AIPEntity}
     * @return {@link AIP}
     */
    private AIP buildAipFromAIPEntity(AIPEntity fromDb) {
        AIP aip = fromDb.getAip();
        // as fromDb.getAip gives us the aip serialized, we have to restore ignored attributes as state
        aip.setState(fromDb.getState());
        aip.setRetry(fromDb.isRetry());
        return aip;
    }

    @Override
    public Page<AIP> findAllByState(AIPState state, Pageable pageable) {
        Page<AIPEntity> fromDb = repo.findAllByStateIn(state, pageable);
        return fromDb.map(this::buildAipFromAIPEntity);
    }

    @Override
    public Page<AIP> findAllWithLockByState(AIPState state, Pageable pageable) {
        Page<AIPEntity> fromDb = repo.findAllWithLockByState(state, pageable);
        return fromDb.map(this::buildAipFromAIPEntity);
    }

    @Override
    public Set<AIP> findAllByIpIdStartingWith(String ipIdWithoutVersion) {
        return repo.findAllByIpIdStartingWith(ipIdWithoutVersion).stream().map(this::buildAipFromAIPEntity)
                .collect(Collectors.toSet());
    }

    @Override
    public Page<AIP> findAllByStateAndTagsInAndLastEventDateAfter(AIPState state, Set<String> tags,
            OffsetDateTime fromLastUpdateDate, Pageable pageable) {
        return repo.findAllByStateAndTagsInAndLastEventDateAfter(state, tags, fromLastUpdateDate, pageable)
                .map(this::buildAipFromAIPEntity);
    }

    @Override
    public Page<AIP> findAllByStateAndTagsIn(AIPState state, Set<String> tags, Pageable pageable) {
        return repo.findAllByStateAndTagsIn(state, tags, pageable).map(this::buildAipFromAIPEntity);
    }

    @Override
    public Set<AIP> findAllByStateService(AIPState state) {
        return repo.findAllByStateIn(state).stream().map(this::buildAipFromAIPEntity).collect(Collectors.toSet());
    }

    @Override
    public Optional<AIP> findOneByIpId(String ipId) {
        Optional<AIPEntity> aipDatabase = repo.findOneByIpId(ipId);
        if (aipDatabase.isPresent()) {
            return Optional.of(buildAipFromAIPEntity(aipDatabase.get()));
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
        return repo.findAllByStateIn(states).stream().map(this::buildAipFromAIPEntity).collect(Collectors.toSet());
    }

    @Override
    public void remove(AIP aip) {
        Optional<AIPEntity> opt = repo.findOneByIpId(aip.getId().toString());
        if (opt.isPresent()) {
            repo.delete(opt.get());
        }
    }

    @Override
    public Set<AIP> findAllByIpIdIn(Collection<String> ipIds) {
        return repo.findAllByIpIdIn(ipIds).stream().map(this::buildAipFromAIPEntity).collect(Collectors.toSet());
    }

    @Override
    public Stream<UniformResourceName> findUrnsByIpIdIn(Collection<String> ipIds) {
        return repo.findByIpIdIn(ipIds).map(aipEntity -> aipEntity.getAip().getId());
    }

    @Override
    public Set<AIP> findAllByTags(String tag) {
        return repo.findAllByTags(tag).stream().map(this::buildAipFromAIPEntity).collect(Collectors.toSet());
    }

    @Override
    public Set<AIP> findAllBySipId(String sipIpId) {
        return repo.findAllBySipId(sipIpId).stream().map(this::buildAipFromAIPEntity).collect(Collectors.toSet());
    }

    @Override
    public Page<AIP> findAllByStateAndLastEventDateAfter(AIPState state, OffsetDateTime fromLastUpdateDate,
            Pageable pageable) {
        return repo.findAllByStateAndLastEventDateAfter(state, fromLastUpdateDate, pageable)
                .map(this::buildAipFromAIPEntity);
    }

    @Override
    public Page<AIP> findAll(String sqlQuery, Pageable pPageable) {
        return custoRepo.findAll(sqlQuery, pPageable).map(this::buildAipFromAIPEntity);
    }

    @Override
    public Set<AIP> findAll(String sqlQuery) {
        return custoRepo.findAll(sqlQuery).stream().map(this::buildAipFromAIPEntity).collect(Collectors.toSet());
    }

    @Override
    public long countBySessionId(String sessionId) {
        return repo.countBySessionId(sessionId);
    }

    @Override
    public long countBySessionIdAndStateIn(String sessionId, Collection<AIPState> states) {
        return repo.countBySessionIdAndStateIn(sessionId, states);
    }

    @Override
    public List<String> findAllByCustomQuery(String query) {
        return custoRepo.getDistinctTags(query);
    }

}
