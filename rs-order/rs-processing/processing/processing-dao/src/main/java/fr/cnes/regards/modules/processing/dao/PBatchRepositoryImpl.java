package fr.cnes.regards.modules.processing.dao;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.entity.BatchEntity;
import fr.cnes.regards.modules.processing.entity.mapping.DomainEntityMapper;
import fr.cnes.regards.modules.processing.domain.repository.IPBatchRepository;
import io.vavr.control.Option;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class PBatchRepositoryImpl implements IPBatchRepository {

    static Cache<UUID, PBatch> cache = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    private final IBatchEntityRepository delegate;

    private final DomainEntityMapper.Batch mapper;

    @Autowired
    public PBatchRepositoryImpl(
            IBatchEntityRepository delegate,
            DomainEntityMapper.Batch mapper
    ) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override public Mono<PBatch> save(PBatch domain) {
        return delegate
            .save(mapper.toEntity(domain))
            .map(BatchEntity::persisted)
            .map(mapper::toDomain)
            .doOnNext(b -> cache.put(b.getId(), b));
    }

    @Override public Mono<PBatch> findById(UUID id) {
        return Option.of(cache.getIfPresent(id))
            .map(Mono::just)
            .getOrElse(() -> delegate
                .findById(id)
                .map(BatchEntity::persisted)
                .map(mapper::toDomain)
                .doOnNext(b -> cache.put(b.getId(), b))
            );
    }
}
