package fr.cnes.regards.modules.processing.events;

import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.PExecutionStep;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.UUID;

import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;

/**
 * This event is emitted by the
 */
@Value @AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DecoratedExecutionStepEvent {

    PExecutionStep step;
    Seq<POutputFile> outputFiles;

    public static DecoratedExecutionStepEvent register(UUID execId, String msg) {
        return new DecoratedExecutionStepEvent(new PExecutionStep(null, execId, ExecutionStatus.REGISTERED, nowUtc(), msg), List.empty());
    }

    public static DecoratedExecutionStepEvent running(UUID execId, String msg) {
        return new DecoratedExecutionStepEvent(new PExecutionStep(null, execId, ExecutionStatus.RUNNING, nowUtc(), msg), List.empty());
    }

    public static DecoratedExecutionStepEvent timedOut(UUID execId, String msg) {
        return new DecoratedExecutionStepEvent(new PExecutionStep(null, execId, ExecutionStatus.TIMED_OUT, nowUtc(), msg), List.empty());
    }

    public static DecoratedExecutionStepEvent failure(UUID execId, String msg) {
        return new DecoratedExecutionStepEvent(new PExecutionStep(null, execId, ExecutionStatus.FAILURE, nowUtc(), ""), List.empty());
    }

    public static DecoratedExecutionStepEvent success(UUID execId, Seq<POutputFile> outputFiles) {
        return new DecoratedExecutionStepEvent(new PExecutionStep(null, execId, ExecutionStatus.SUCCESS, nowUtc(), ""), outputFiles);
    }

}
