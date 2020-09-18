package fr.cnes.regards.modules.processing.plugins.impl;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PInputFile;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.storage.ExecutionLocalWorkdir;
import fr.cnes.regards.modules.processing.storage.IExecutionLocalWorkdirService;
import fr.cnes.regards.modules.processing.storage.ISharedStorageService;
import io.vavr.collection.Seq;
import org.springframework.beans.factory.annotation.Autowired;

import static fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent.event;
import static fr.cnes.regards.modules.processing.utils.ReactorErrorTransformers.addInContext;

public abstract class AbstractBaseForecastedStorageAwareProcessPlugin extends AbstractBaseForecastedProcessPlugin {

    @Autowired
    protected IExecutionLocalWorkdirService workdirService;

    @Autowired
    protected ISharedStorageService storageService;

    public IExecutable prepareWorkdir() {
        return context -> {
            PExecution exec = context.getExec();
            Seq<PInputFile> inputFiles = exec.getInputFiles();
            return workdirService.makeWorkdir(exec)
                .flatMap(wd -> workdirService.writeInputFilesToWorkdirInput(wd, inputFiles))
                .map(wd -> context.withParam(ExecutionLocalWorkdir.class, wd))
                .subscriberContext(addInContext(PExecution.class, exec));
        };
    }



    public IExecutable storeOutputFiles() {
        return context -> {
            PExecution exec = context.getExec();
            return context.getParam(ExecutionLocalWorkdir.class)
                .flatMap(wd -> storageService.storeResult(context, wd))
                .flatMap(out -> context.sendEvent(() -> event(out)))
                .subscriberContext(addInContext(PExecution.class, exec));
        };
    }

    public IExecutionLocalWorkdirService getWorkdirService() {
        return workdirService;
    }

    public void setWorkdirService(IExecutionLocalWorkdirService workdirService) {
        this.workdirService = workdirService;
    }

    public ISharedStorageService getStorageService() {
        return storageService;
    }

    public void setStorageService(ISharedStorageService storageService) {
        this.storageService = storageService;
    }
}
