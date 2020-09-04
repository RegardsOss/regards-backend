package fr.cnes.regards.modules.processing.storage;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionFileParameterValue;
import io.vavr.Function2;
import io.vavr.collection.Seq;
import io.vavr.control.Option;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

import static fr.cnes.regards.modules.processing.domain.PStep.failure;
import static fr.cnes.regards.modules.processing.domain.PStep.running;
import static fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent.event;
import static fr.cnes.regards.modules.processing.utils.ReactorErrorTransformers.addInContext;

public class Executables {

    public static IExecutable prepareWorkdir() {
        return context -> {
            IExecutionLocalWorkdirService workdirService = context.getWorkdirService();
            PExecution exec = context.getExec();
            Seq<ExecutionFileParameterValue> inputFiles = exec.getInputFiles();
            return workdirService.makeWorkdir(exec)
                .flatMap(workdir -> workdirService.writeInputFilesToWorkdirInput(workdir, inputFiles))
                .map(workdir -> context)
                .subscriberContext(addInContext(PExecution.class, exec));
        };
    }

    public static IExecutable storeOutputFiles() {
        return context -> {
            ISharedStorageService storageService = context.getStorageService();
            PExecution exec = context.getExec();
            return storageService.storeResult(context)
                .flatMap(out -> context.sendEvent(() -> event(out)))
                .subscriberContext(addInContext(PExecution.class, exec));
        };
    }

}
