package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.domain.dto.PBatchRequest;
import fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent;
import fr.cnes.regards.modules.processing.domain.engine.IExecutionEventNotifier;
import fr.cnes.regards.modules.processing.domain.events.PExecutionRequestEvent;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.size.FileSetStatistics;
import fr.cnes.regards.modules.processing.domain.step.PStepFinal;
import fr.cnes.regards.modules.processing.utils.TimeUtils;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;

public class ExecutionEventNotifierImplIT extends AbstractProcessingServiceIT {

    @Test
    public void testNotifyEvent() {
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

        // Launch exec to get the execution context
        PExecution exec = executionService.launchExecution(execReq).block();

        PStepFinal finalStep = new PStepFinal(SUCCESS, TimeUtils.nowUtc(), "yes");
        ExecutionEvent.FinalEvent finalEvent = new ExecutionEvent.FinalEvent(finalStep, List.empty());

        // Get the notifier from the context
        IExecutionEventNotifier notifier = ctxRef.get().getEventNotifier();
        assertThat(notifier).isInstanceOf(ExecutionEventNotifierImpl.class);

        // Ensure that the step has been added to the execution
        PExecution notifiedExec = notifier.notifyEvent(finalEvent).block();
        assertThat(notifiedExec.getSteps().last().getStatus()).isEqualTo(finalStep.getStatus());
        assertThat(notifiedExec.getSteps().last().getMessage()).isEqualTo(finalStep.getMessage());

    }
}