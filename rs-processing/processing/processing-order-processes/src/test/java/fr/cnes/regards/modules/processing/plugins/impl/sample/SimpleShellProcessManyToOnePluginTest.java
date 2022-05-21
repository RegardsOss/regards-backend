/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.plugins.impl.sample;

import fr.cnes.regards.modules.processing.domain.*;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.domain.engine.IWorkloadEngine;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.repository.IWorkloadEngineRepository;
import fr.cnes.regards.modules.processing.domain.service.IDownloadService;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import fr.cnes.regards.modules.processing.order.Cardinality;
import fr.cnes.regards.modules.processing.order.Scope;
import fr.cnes.regards.modules.processing.repository.OrderProcessRepositoryImpl;
import fr.cnes.regards.modules.processing.storage.ExecutionLocalWorkdirService;
import fr.cnes.regards.modules.processing.storage.IExecutionLocalWorkdirService;
import fr.cnes.regards.modules.processing.storage.ISharedStorageService;
import fr.cnes.regards.modules.processing.storage.SharedStorageService;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.*;
import static fr.cnes.regards.modules.processing.plugins.impl.sample.SimpleProcessPluginTestUtils.*;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;

public class SimpleShellProcessManyToOnePluginTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleShellProcessManyToOnePluginTest.class);

    @Test
    public void runSimpleScript() throws Exception {
        UUID execId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();

        Path tempWorkdirBase = Files.createTempDirectory("workdirs");
        Path tempStorageBase = Files.createTempDirectory("storage");

        IDownloadService downloadService = (file, dest) -> Mono.fromCallable(() -> {
            FileUtils.copyFile(new File(file.getUrl().toURI().toString().replace("file:", "")), dest.toFile());
            return dest;
        });
        IExecutionLocalWorkdirService workdirService = new ExecutionLocalWorkdirService(tempWorkdirBase,
                                                                                        downloadService);
        ISharedStorageService storageService = new SharedStorageService(tempStorageBase);

        IWorkloadEngine engine = makeEngine();
        IWorkloadEngineRepository engineRepo = makeEngineRepo(engine);
        OrderProcessRepositoryImpl processRepo = makeProcessRepo(engineRepo);
        SimpleShellProcessPlugin shellProcessPlugin = makePlugin(workdirService, storageService);

        RightsPluginConfiguration rpc = makeRightsPluginConfig(SimpleShellProcessPlugin.SIMPLE_SHELL_PROCESS_PLUGIN);
        PProcess process = processRepo.fromPlugin(rpc, shellProcessPlugin, "tenant").block();
        PBatch batch = makeBatch(batchId, process);
        PExecution exec = makeExec(execId, batchId, batch.getProcessBusinessId());

        AtomicReference<Seq<PStep>> steps = new AtomicReference<>(List.empty());
        AtomicReference<Seq<POutputFile>> outputFiles = new AtomicReference<>();
        AtomicReference<PExecution> execRef = new AtomicReference<>(exec);
        AtomicReference<ExecutionContext> finalContext = new AtomicReference<>();

        ExecutionContext ctx = new ExecutionContext(exec, batch, process, s -> {
            Seq<POutputFile> execOutFiles = s.outputFiles();
            if (!execOutFiles.isEmpty()) {
                outputFiles.set(execOutFiles);
            }
            return Mono.fromCallable(() -> steps.updateAndGet(ss -> s.step().map(ss::append).getOrElse(ss)))
                       .map(ss -> execRef.updateAndGet(e -> e.withSteps(ss)));
        });

        CountDownLatch subscriptionLatch = new CountDownLatch(1);

        IExecutable executable = shellProcessPlugin.executable();
        executable.execute(ctx).subscribeOn(Schedulers.immediate()).subscribe(c -> {
            LOGGER.info("Success: {}", c);
            finalContext.set(c);
        }, e -> LOGGER.error("Failure", e), subscriptionLatch::countDown);

        subscriptionLatch.await(1L, MINUTES);

        assertThat(finalContext.get()).isNotNull();
        assertThat(finalContext.get().getExec().getSteps()).hasSize(3);
        assertThat(finalContext.get().getExec().getSteps().get(0).getStatus()).isEqualTo(PREPARE);
        assertThat(finalContext.get().getExec().getSteps().get(1).getStatus()).isEqualTo(RUNNING);
        assertThat(finalContext.get().getExec().getSteps().get(2).getStatus()).isEqualTo(SUCCESS);

        assertThat(outputFiles.get()).isNotNull();
        assertThat(outputFiles.get()).hasSize(1);
        assertThat(outputFiles.get().get(0).getName()).isEqualTo("tarred_file.tar");
        assertThat(outputFiles.get().get(0).getUrl()).hasProtocol("file");
        assertThat(new File(outputFiles.get().get(0).getUrl().toString().replace("file:", ""))).exists();

        LOGGER.info("Steps during execution: {}", steps.get());
    }

    private SimpleShellProcessPlugin makePlugin(IExecutionLocalWorkdirService workdirService,
                                                ISharedStorageService storageService) {
        SimpleShellProcessPlugin shellProcessPlugin = new SimpleShellProcessPlugin();

        shellProcessPlugin.setWorkdirService(workdirService);
        shellProcessPlugin.setStorageService(storageService);

        shellProcessPlugin.setScope(Scope.SUBORDER);
        shellProcessPlugin.setCardinality(Cardinality.ONE_PER_EXECUTION);

        // TODO: try removing these two setters to fix a cast exception
        shellProcessPlugin.setDurationForecast("10min");
        shellProcessPlugin.setSizeForecast("*1");

        shellProcessPlugin.setShellScriptName(Paths.get("src/test/resources/tarInputs.sh").toFile().getAbsolutePath());
        shellProcessPlugin.setEnvVariables("OUTPUT_NAME=tarred_file");
        shellProcessPlugin.setMaxFeaturesInInput(100);

        return shellProcessPlugin;

    }
}