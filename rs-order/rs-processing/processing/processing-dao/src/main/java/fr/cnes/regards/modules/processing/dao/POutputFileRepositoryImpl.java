package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.entity.ExecutionEntity;
import fr.cnes.regards.modules.processing.entity.OutputFileEntity;
import fr.cnes.regards.modules.processing.entity.mapping.DomainEntityMapper;
import fr.cnes.regards.modules.processing.repository.IPOutputFilesRepository;
import io.vavr.collection.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Component
public class POutputFileRepositoryImpl implements IPOutputFilesRepository {

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
                .map(OutputFileEntity::persisted)
                .map(mapper::toDomain);
    }

    @Override public Flux<POutputFile> findByExecId(UUID execId) {
        return entityOutputFileRepo.findByExecId(execId)
                .map(mapper::toDomain);
    }

    @Override public Flux<POutputFile> findByIdIn(List<UUID> ids) {
        return entityOutputFileRepo.findAllById(ids)
                .map(mapper::toDomain);
    }

    @Override public Flux<POutputFile> findByDownloadedIsTrueAndDeletedIsFalse() {
        return entityOutputFileRepo.findByDownloadedIsTrueAndDeletedIsFalse()
                .map(mapper::toDomain);
    }
}
