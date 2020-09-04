package fr.cnes.regards.modules.processing.dao;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.entity.mapping.DomainEntityMapper;
import fr.cnes.regards.modules.processing.repository.IPOutputFilesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class POutputFileRepositoryImpl implements IPOutputFilesRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(POutputFileRepositoryImpl.class);

    private static Cache<UUID, PExecution> cache = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    private final IOutputFileEntityRepository entityOutputFileRepo;

    private final DomainEntityMapper.OutputFile mapper;

    @Autowired
    public POutputFileRepositoryImpl(
            IOutputFileEntityRepository entityOutputFileRepo,
            DomainEntityMapper.OutputFile mapper
    ) {
        this.entityOutputFileRepo = entityOutputFileRepo;
        this.mapper = mapper;
    }

    @Override public Flux<POutputFile> save(Flux<POutputFile> files) {
        return entityOutputFileRepo.saveAll(files.map(mapper::toEntity))
                .map(mapper::toDomain);
    }

    @Override public Flux<POutputFile> findByExecId(UUID execId) {
        return entityOutputFileRepo.findByExecId(execId)
                .map(mapper::toDomain);
    }
}
