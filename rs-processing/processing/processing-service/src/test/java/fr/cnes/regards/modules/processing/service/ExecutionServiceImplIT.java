package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.domain.dto.PBatchRequest;
import fr.cnes.regards.modules.processing.domain.events.PExecutionRequestEvent;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.size.FileSetStatistics;
import fr.cnes.regards.modules.processing.entity.ExecutionEntity;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class ExecutionServiceImplIT extends AbstractProcessingServiceIT {

    @Test
    public void testLaunchExecution() throws InterruptedException {
        // GIVEN
        AtomicReference<ExecutionContext> ctxRef = new AtomicReference<>();
        configureProcessUpdater(p -> p.withExecutable(ctx -> {
            ctxRef.set(ctx);
            return Mono.just(ctx);
        }));

        UUID processBusinessId = UUID.randomUUID();

        PBatchRequest batchRequest = new PBatchRequest("bcid",
                                                       processBusinessId,
                                                       THE_TENANT,
                                                       THE_USER,
                                                       THE_ROLE,
                                                       HashMap.empty(),
                                                       HashMap.of("dataset1",
                                                                  new FileSetStatistics("dataset1", 5, 123456L)));

        AtomicReference<Throwable> throwableRef = new AtomicReference<>();

        PBatch batch = batchService.checkAndCreateBatch(new PUserAuth(THE_TENANT, THE_USER, THE_ROLE, THE_TOKEN),
                                                        batchRequest).block();

        PExecutionRequestEvent execReq = new PExecutionRequestEvent("ecid", batch.getId(), List.empty());

        // WHEN
        // Launch exec
        PExecution exec = executionService.launchExecution(execReq).block();

        //THEN
        assertThat(exec.getExecutionCorrelationId()).isEqualTo(execReq.getExecutionCorrelationId());

        // Make sure it's in database
        ExecutionEntity execEntity = execEntityRepo.findById(exec.getId()).block();
        assertThat(execEntity.getId()).isEqualTo(exec.getId());

        assertThat(ctxRef.get()).isNotNull();
        assertThat(ctxRef.get().getExec()).isEqualTo(exec);
        assertThat(ctxRef.get().getBatch()).isEqualTo(batch);

        System.out.println("lastUpdated=" + execEntity.getLastUpdated());
        System.out.println("timeOutAfterMillis=" + execEntity.getTimeoutAfterMillis());

        execEntityRepo.save(execEntity.withCurrentStatus(ExecutionStatus.RUNNING)
                                      .withTimeoutAfterMillis(1000L)
                                      .withLastUpdated(OffsetDateTime.now().minusHours(4)));
        execRepo.getTimedOutExecutions().subscribe(e -> System.out.println(e));
        executionService.scheduledTimeoutNotify();

    }

}