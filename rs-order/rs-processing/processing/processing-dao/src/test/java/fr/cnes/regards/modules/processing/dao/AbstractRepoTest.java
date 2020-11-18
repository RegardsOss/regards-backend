package fr.cnes.regards.modules.processing.dao;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.repository.IPBatchRepository;
import fr.cnes.regards.modules.processing.domain.repository.IPExecutionRepository;
import fr.cnes.regards.modules.processing.domain.repository.IPOutputFilesRepository;
import fr.cnes.regards.modules.processing.testutils.AbstractProcessingTest;
import fr.cnes.regards.modules.processing.utils.random.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;
import static io.vavr.collection.List.empty;

public abstract class AbstractRepoTest extends AbstractProcessingTest implements RandomUtils {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractRepoTest.class);
    
    @Autowired protected IPBatchRepository domainBatchRepo;
    @Autowired protected IPExecutionRepository domainExecRepo;
    @Autowired protected IPOutputFilesRepository domainOutputFilesRepo;
    @Autowired protected IBatchEntityRepository entityBatchRepo;
    @Autowired protected IExecutionEntityRepository entityExecRepo;
    @Autowired protected IOutputFileEntityRepository entityOutputFilesRepo;


    protected Mono<PBatch> saveBatch(int i, PBatch pBatch) {
        return domainBatchRepo.save(pBatch)
                .doOnNext(b -> LOGGER.info("ATTEMPT {}, Saved batch {}", i, b));
    }

    protected Mono<PExecution> saveExec(int i, PExecution pExec) {
        return domainExecRepo.create(pExec)
                .doOnNext(e -> LOGGER.info("ATTEMPT {}, Saved exec {}", i, e));
    }

    protected Mono<PExecution> addStep(int i, PExecution persistedExec, PStep step) {
        PExecution executionWithNewStep = persistedExec.addStep(step);
        return domainExecRepo.update(executionWithNewStep)
            .doOnNext(e -> LOGGER.info("ATTEMPT {}, Added step {} into {}", i, step, e))
            .doOnError(t -> LOGGER.error("ATTEMPT {}, Failed to save exec with new step: persisted={}, version={}, exec={}",
                 i, executionWithNewStep.isPersisted(), executionWithNewStep.getVersion(), executionWithNewStep, t));
    }

    protected Mono<PBatch> findBatch(int i, PBatch pBatch) {
        return domainBatchRepo.findById(pBatch.getId())
                .doOnNext(b -> LOGGER.info("ATTEMPT {}, Found batch {}", i, b));
    }

    protected Mono<PExecution> findExec(int i, PExecution pExec) {
        return domainExecRepo.findById(pExec.getId())
                .doOnNext(e -> LOGGER.info("ATTEMPT {}, Found exec {}", i, e));
    }

    protected static PExecution asNew(PExecution exec) {
        OffsetDateTime lastUpdated = nowUtc();
        return exec
                .withSteps(empty())
                .withId(UUID.randomUUID())
                .withPersisted(false)
                .withVersion(0)
                .withCreated(lastUpdated)
                .withLastUpdated(lastUpdated);
    }

}
