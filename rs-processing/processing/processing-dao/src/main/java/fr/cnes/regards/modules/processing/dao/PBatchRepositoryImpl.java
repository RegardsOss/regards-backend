/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.processing.dao;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.repository.IPBatchRepository;
import fr.cnes.regards.modules.processing.entity.BatchEntity;
import fr.cnes.regards.modules.processing.entity.mapping.DomainEntityMapper;
import fr.cnes.regards.modules.processing.exceptions.ProcessingException;
import fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * This class implements is a bridge between batch domain entities and database entities.
 *
 * @author gandrieu
 */
@Component
public class PBatchRepositoryImpl implements IPBatchRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(PBatchRepositoryImpl.class);

    static Cache<UUID, PBatch> cache = Caffeine.newBuilder()
                                               .expireAfterAccess(30, TimeUnit.MINUTES)
                                               .maximumSize(10000)
                                               .build();

    private final IBatchEntityRepository delegate;

    private final DomainEntityMapper.Batch mapper;

    @Autowired
    public PBatchRepositoryImpl(IBatchEntityRepository delegate, DomainEntityMapper.Batch mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public Mono<PBatch> save(PBatch domain) {
        return delegate.save(mapper.toEntity(domain))
                       .map(BatchEntity::persisted)
                       .map(mapper::toDomain)
                       .doOnNext(b -> cache.put(b.getId(), b));
    }

    @Override
    public Mono<PBatch> findById(UUID id) {
        return Option.of(cache.getIfPresent(id))
                     .map(Mono::just)
                     .getOrElse(() -> delegate.findById(id)
                                              .map(BatchEntity::persisted)
                                              .map(mapper::toDomain)
                                              .doOnNext(b -> cache.put(b.getId(), b)))
                     .switchIfEmpty(Mono.defer(() -> Mono.error(new BatchNotFoundException(id))));
    }

    @Override
    public Mono<Void> deleteAllFinishedForMoreThan(long batchRipeForDeleteAgeMs) {
        return delegate.deleteAll(delegate.getCleanableBatches(batchRipeForDeleteAgeMs)
                                          .doOnNext(b -> LOGGER.info("Deleting batch {}", b)));
    }

    @Override
    public Mono<Void> deleteByProcessBusinessId(UUID processBusinessId) {
        return delegate.deleteAll(delegate.findByProcessBusinessId(processBusinessId));
    }

    @Override
    public Mono<Void> deleteAll() {
        return delegate.deleteAll().doOnTerminate(() -> {
            cache.invalidateAll();
            cache.cleanUp();
        });
    }

    public static final class BatchNotFoundException extends ProcessingException {

        public BatchNotFoundException(UUID batchId) {
            super(ProcessingExceptionType.BATCH_NOT_FOUND_EXCEPTION,
                  String.format("Batch uuid not found: %s", batchId));
        }

        @Override
        public String getMessage() {
            return desc;
        }
    }
}
