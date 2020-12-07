/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.plugins.impl;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PInputFile;
import fr.cnes.regards.modules.processing.domain.PStep;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.storage.ExecutionLocalWorkdir;
import fr.cnes.regards.modules.processing.storage.IExecutionLocalWorkdirService;
import fr.cnes.regards.modules.processing.storage.ISharedStorageService;
import io.vavr.collection.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import static fr.cnes.regards.modules.processing.domain.engine.ExecutionEvent.event;
import static fr.cnes.regards.modules.processing.utils.ReactorErrorTransformers.addInContext;

/**
 * This class is a base abstract class for process plugins which interact with the
 * storage to store input/output files.
 *
 * @author gandrieu
 */
public abstract class AbstractBaseForecastedStorageAwareProcessPlugin extends AbstractBaseForecastedProcessPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBaseForecastedStorageAwareProcessPlugin.class);
    
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
                .subscriberContext(addInContext(PExecution.class, exec))
                .switchIfEmpty(Mono.just(context))
                .log();
        };
    }

    public IExecutable storeOutputFiles() {
        return context -> {
            PExecution exec = context.getExec();
            return context.getParam(ExecutionLocalWorkdir.class)
                .flatMap(wd -> storageService.storeResult(context, wd))
                .flatMap(out -> context.sendEvent(event(PStep.success(""), out)))
                .subscriberContext(addInContext(PExecution.class, exec));
        };
    }

    public IExecutable cleanWorkdir() {
        return context -> context.getParam(ExecutionLocalWorkdir.class)
                .flatMap(workdirService::cleanupWorkdir)
                .map(wd -> context)
                .onErrorResume(t -> {
                    LOGGER.error("execId={} Failed to cleanup execution workdir", context.getExec().getId(), t);
                    return Mono.just(context);
                })
                .switchIfEmpty(Mono.just(context));
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
