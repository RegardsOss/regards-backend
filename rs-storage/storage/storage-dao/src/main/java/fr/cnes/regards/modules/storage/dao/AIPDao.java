package fr.cnes.regards.modules.storage.dao;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;

/**
 * Implementation of {@link IAIPDao}
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class AIPDao implements IAIPDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPDao.class);

    /**
     * {@link IAIPEntityRepository} instance
     */
    private final IAIPEntityRepository repo;

    private final ICustomizedAIPEntityRepository custoRepo;

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
        long startFindDBAIP = System.currentTimeMillis();
        Optional<AIPEntity> fromDb = repo.findOneByAipId(toSave.getId().toString());
        long endFindDBAIP = System.currentTimeMillis();
        LOGGER.trace("AIPDao#save: Finding AIP from DB, for ID, took {} ms", endFindDBAIP - startFindDBAIP);
        if (fromDb.isPresent()) {
            toSaveInDb.setId(fromDb.get().getId());
        }
        long startSaveAIP = System.currentTimeMillis();
        AIPEntity saved = repo.save(toSaveInDb);
        long endSaveAIP = System.currentTimeMillis();
        LOGGER.trace("AIPDao#save: Saving AIP into DB took {} ms", endSaveAIP - startSaveAIP);
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
    public Page<AIP> findAllByIpIdStartingWith(String aipIdWithoutVersion, Pageable page) {
        Page<AIPEntity> aipEntities = repo.findAllByAipIdStartingWith(aipIdWithoutVersion, page);
        return new PageImpl<>(
                aipEntities.getContent().stream().map(this::buildAipFromAIPEntity).collect(Collectors.toList()), page,
                aipEntities.getContent().size());
    }

    @Override
    public Page<AIP> findAllByStateAndTagsInAndLastEventDateAfter(AIPState state, Set<String> tags,
            OffsetDateTime fromLastUpdateDate, Pageable pageable) {
        return repo.findAllByStateAndTagsInAndLastEventDateAfter(state, tags, fromLastUpdateDate, pageable)
                .map(this::buildAipFromAIPEntity);
    }

    @Override
    public Page<AIP> findAllByStateService(AIPState state, Pageable page) {
        Page<AIPEntity> aipEntities = repo.findAllByStateIn(state, page);
        Set<AIP> aips = Sets.newHashSet();
        if (!aipEntities.getContent().isEmpty()) {
            aips = aipEntities.getContent().stream().map(this::buildAipFromAIPEntity).collect(Collectors.toSet());
        }
        return new PageImpl<>(aips.stream().collect(Collectors.toList()), page, aips.size());
    }

    @Override
    public Optional<AIP> findOneByAipId(String aipId) {
        Optional<AIPEntity> aipDatabase = repo.findOneByAipId(aipId);
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
    public Page<AIP> findAllByStateInService(Collection<AIPState> states, Pageable page) {
        Page<AIPEntity> aipEntities = repo.findAllByStateIn(states, page);
        return new PageImpl<AIP>(
                aipEntities.getContent().stream().map(this::buildAipFromAIPEntity).collect(Collectors.toList()), page,
                aipEntities.getContent().size());
    }

    @Override
    public void remove(AIP aip) {
        Optional<AIPEntity> opt = repo.findOneByAipId(aip.getId().toString());
        if (opt.isPresent()) {
            repo.delete(opt.get());
        }
    }

    @Override
    public Set<AIP> findAllByAipIdIn(Collection<String> aipIds) {
        return repo.findAllByAipIdIn(aipIds).stream().map(this::buildAipFromAIPEntity).collect(Collectors.toSet());
    }

    @Override
    public Stream<UniformResourceName> findUrnsByAipIdIn(Collection<String> aipIds) {
        return repo.findByAipIdIn(aipIds).map(aipEntity -> aipEntity.getAip().getId());
    }

    @Override
    public Set<AIP> findAllByTags(String tag) {
        return repo.findAllByTags(tag).stream().map(this::buildAipFromAIPEntity).collect(Collectors.toSet());
    }

    @Override
    public Set<AIP> findAllBySipId(String sipId) {
        return repo.findAllBySipId(sipId).stream().map(this::buildAipFromAIPEntity).collect(Collectors.toSet());
    }

    @Override
    public Set<AIP> findAllBySipIdIn(Collection<String> sipIds) {
        return repo.findAllBySipIdIn(sipIds).stream().map(this::buildAipFromAIPEntity).collect(Collectors.toSet());
    }

    @Override
    public Page<AIP> findPageBySipIdIn(Collection<String> sipIds, Pageable page) {
        return repo.findPageBySipIdIn(sipIds, page).map(this::buildAipFromAIPEntity);
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
