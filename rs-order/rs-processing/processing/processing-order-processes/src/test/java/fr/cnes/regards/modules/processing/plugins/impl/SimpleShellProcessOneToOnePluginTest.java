/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.processing.domain.*;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.domain.engine.IWorkloadEngine;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.repository.IWorkloadEngineRepository;
import fr.cnes.regards.modules.processing.domain.service.IDownloadService;
import fr.cnes.regards.modules.processing.domain.service.IPUserAuthService;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import fr.cnes.regards.modules.processing.order.Cardinality;
import fr.cnes.regards.modules.processing.order.Scope;
import fr.cnes.regards.modules.processing.repository.IRightsPluginConfigurationRepository;
import fr.cnes.regards.modules.processing.repository.OrderProcessRepositoryImpl;
import fr.cnes.regards.modules.processing.storage.ExecutionLocalWorkdirService;
import fr.cnes.regards.modules.processing.storage.IExecutionLocalWorkdirService;
import fr.cnes.regards.modules.processing.storage.ISharedStorageService;
import fr.cnes.regards.modules.processing.storage.SharedStorageService;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.*;
import static fr.cnes.regards.modules.processing.utils.OrderInputFileMetadataUtils.inputMetadataAsMap;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SimpleShellProcessOneToOnePluginTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleShellProcessOneToOnePluginTest.class);

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
        IExecutionLocalWorkdirService workdirService = new ExecutionLocalWorkdirService(tempWorkdirBase, downloadService);
        ISharedStorageService storageService = new SharedStorageService(tempStorageBase);

        IWorkloadEngine engine = makeEngine();
        IWorkloadEngineRepository engineRepo = makeEngineRepo(engine);
        OrderProcessRepositoryImpl processRepo = makeProcessRepo(engineRepo);
        SimpleShellProcessPlugin shellProcessPlugin = makePlugin(workdirService, storageService);

        RightsPluginConfiguration rpc = makeRightsPluginConfig();
        PProcess process = processRepo.fromPlugin(rpc, shellProcessPlugin, "tenant").block();
        PBatch batch = makeBatch(batchId, process);
        PExecution exec = makeExec(execId, batchId, batch.getProcessBusinessId());

        AtomicReference<Seq<PStep>> steps = new AtomicReference<>(List.empty());
        AtomicReference<Seq<POutputFile>> outputFiles = new AtomicReference<>();
        AtomicReference<PExecution> execRef = new AtomicReference<>(exec);
        AtomicReference<ExecutionContext> finalContext = new AtomicReference<>();

        ExecutionContext ctx = new ExecutionContext(
                exec,
                batch,
                process,
                s -> {
                    Seq<POutputFile> execOutFiles = s.outputFiles();
                    if (!execOutFiles.isEmpty()) { outputFiles.set(execOutFiles); }
                    return Mono
                        .fromCallable(() -> steps.updateAndGet(ss -> s.step().map(ss::append).getOrElse(ss)))
                        .map(ss -> execRef.updateAndGet(e -> e.withSteps(ss)));
                }
        );

        CountDownLatch subscriptionLatch = new CountDownLatch(1);

        IExecutable executable = shellProcessPlugin.executable();
        executable
            .execute(ctx)
            .subscribeOn(Schedulers.immediate())
            .subscribe(
                c -> { LOGGER.info("Success: {}", c); finalContext.set(c); },
                e -> LOGGER.error("Failure", e),
                subscriptionLatch::countDown
            );

        subscriptionLatch.await(1L, MINUTES);

        assertThat(finalContext.get()).isNotNull();
        assertThat(finalContext.get().getExec().getSteps()).hasSize(3);
        assertThat(finalContext.get().getExec().getSteps().get(0).getStatus()).isEqualTo(PREPARE);
        assertThat(finalContext.get().getExec().getSteps().get(1).getStatus()).isEqualTo(RUNNING);
        assertThat(finalContext.get().getExec().getSteps().get(2).getStatus()).isEqualTo(SUCCESS);

        assertThat(outputFiles.get()).isNotNull();
        assertThat(outputFiles.get()).hasSize(2);

        LOGGER.info("Steps during execution: {}", steps.get());
    }


    @Test
    public void runSimpleScriptOnError() throws Exception {
        UUID execId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();

        Path tempWorkdirBase = Files.createTempDirectory("workdirs");
        Path tempStorageBase = Files.createTempDirectory("storage");

        IDownloadService downloadService = (file, dest) -> Mono.fromCallable(() -> {
            //throw new RuntimeException("wups on download");
            FileUtils.copyFile(new File(file.getUrl().toURI().toString().replace("file:", "")), dest.toFile());
            return dest;
        });
        IExecutionLocalWorkdirService workdirService = new ExecutionLocalWorkdirService(tempWorkdirBase, downloadService);
        ISharedStorageService storageService = mock(ISharedStorageService.class);
        when(storageService.storeResult(any(), any())).thenAnswer(i -> Mono.error(new RuntimeException("Could not store")));

        IWorkloadEngine engine = makeEngine();
        IWorkloadEngineRepository engineRepo = makeEngineRepo(engine);
        OrderProcessRepositoryImpl processRepo = makeProcessRepo(engineRepo);
        SimpleShellProcessPlugin shellProcessPlugin = makePlugin(workdirService, storageService);

        RightsPluginConfiguration rpc = makeRightsPluginConfig();
        PProcess process = processRepo.fromPlugin(rpc, shellProcessPlugin, "tenant").block();
        PBatch batch = makeBatch(batchId, process);
        PExecution exec = makeExec(execId, batchId, batch.getProcessBusinessId());

        AtomicReference<Seq<PStep>> steps = new AtomicReference<>(List.empty());
        AtomicReference<Seq<POutputFile>> outputFiles = new AtomicReference<>();
        AtomicReference<PExecution> execRef = new AtomicReference<>(exec);
        AtomicReference<ExecutionContext> finalContext = new AtomicReference<>();

        ExecutionContext ctx = new ExecutionContext(
                exec,
                batch,
                process,
                s -> {
                    Seq<POutputFile> execOutFiles = s.outputFiles();
                    if (!execOutFiles.isEmpty()) { outputFiles.set(execOutFiles); }
                    return Mono
                            .fromCallable(() -> steps.updateAndGet(ss -> s.step().map(ss::append).getOrElse(ss)))
                            .map(ss -> execRef.updateAndGet(e -> e.withSteps(ss)));
                }
        );

        CountDownLatch subscriptionLatch = new CountDownLatch(1);

        IExecutable executable = shellProcessPlugin.executable();
        executable
                .execute(ctx)
                .subscribeOn(Schedulers.immediate())
                .subscribe(
                        c -> { LOGGER.info("Success: {}", c); finalContext.set(c); },
                        e -> LOGGER.error("Failure", e),
                        subscriptionLatch::countDown
                );

        subscriptionLatch.await(1L, MINUTES);

        assertThat(finalContext.get()).isNotNull();
        assertThat(finalContext.get().getExec().getSteps()).hasSize(3);
        assertThat(finalContext.get().getExec().getSteps().get(0).getStatus()).isEqualTo(PREPARE);
        assertThat(finalContext.get().getExec().getSteps().get(1).getStatus()).isEqualTo(RUNNING);
        assertThat(finalContext.get().getExec().getSteps().get(2).getStatus()).isEqualTo(FAILURE);

        assertThat(outputFiles.get()).isNull();

        LOGGER.info("Steps during execution: {}", steps.get());
    }

    @NotNull public RightsPluginConfiguration makeRightsPluginConfig() {
        UUID bid = UUID.randomUUID();
        PluginConfiguration pc = new PluginConfiguration("label",
                                                            SimpleShellProcessPlugin.SIMPLE_SHELL_PROCESS_PLUGIN);
        pc.setBusinessId(bid.toString());

        return new RightsPluginConfiguration(
                1L, pc, bid,
                "EXPLOIT",
                new String[]{},
                false
        );
    }

    private OrderProcessRepositoryImpl makeProcessRepo(IWorkloadEngineRepository engineRepo) throws Exception {
        IRightsPluginConfigurationRepository rightsRepo = mock(IRightsPluginConfigurationRepository.class);
        when(rightsRepo.findByPluginConfiguration(any())).thenAnswer(i -> makeRightsPluginConfig());
        IPUserAuthService authFactory = mock(IPUserAuthService.class);
        when(authFactory.authFromUserEmailAndRole(anyString(), anyString(), anyString()))
                .thenAnswer(i -> new PUserAuth(i.getArgument(0), i.getArgument(1), i.getArgument(2), "authToken"));

        return new OrderProcessRepositoryImpl(
                mock(IPluginService.class),
                engineRepo,
                rightsRepo,
                mock(IRuntimeTenantResolver.class)
        );
    }

    private IWorkloadEngineRepository makeEngineRepo(IWorkloadEngine engine) {
        return new IWorkloadEngineRepository() {
            @Override public Mono<IWorkloadEngine> findByName(String name) {
                return Mono.just(engine);
            }
            @Override public Mono<IWorkloadEngine> register(IWorkloadEngine engine) {
                return Mono.just(engine);
            }
        };
    }

    private IWorkloadEngine makeEngine() {
        return new IWorkloadEngine() {
            @Override public String name() {
                return "JOB";
            }
            @Override public void selfRegisterInRepo() { }
            @Override public Mono<PExecution> run(ExecutionContext context) {
                return Mono.just(context.getExec());
            }
        };
    }

    private SimpleShellProcessPlugin makePlugin(IExecutionLocalWorkdirService workdirService, ISharedStorageService storageService) {
        SimpleShellProcessPlugin shellProcessPlugin = new SimpleShellProcessPlugin();

        shellProcessPlugin.setWorkdirService(workdirService);
        shellProcessPlugin.setStorageService(storageService);

        shellProcessPlugin.setScope(Scope.SUBORDER);
        shellProcessPlugin.setCardinality(Cardinality.ONE_PER_INPUT_FILE);

        // TODO: try removing these two setters to fix a cast exception
        shellProcessPlugin.setDurationForecast("10min");
        shellProcessPlugin.setSizeForecast("*1");

        shellProcessPlugin.setShellScriptName(Paths.get("src/test/resources/copyInputToOutput.sh").toFile().getAbsolutePath());
        shellProcessPlugin.setEnvVariables("SIMPLE_FOO=foo&SIMPLE_BAR=bar");

        return shellProcessPlugin;

    }

    private PExecution makeExec(UUID execId, UUID batchId, UUID processBusinessId) throws Exception {
        return new PExecution(
            execId, "exec cid",  batchId, "batch cid",
            Duration.ofSeconds(10),
            List.of(
                    new PInputFile("one", "one.raw", "text/plain", Paths.get("src/test/resources/one.raw").toUri().toURL(), 3L, "checksum", inputMetadataAsMap(false, "urn"), "one"),
                    new PInputFile("two", "two.raw", "text/plain", Paths.get("src/test/resources/two.raw").toUri().toURL(), 3L, "checksum", inputMetadataAsMap(false, "urn"), "two")
            ),
            List.empty(),
            "tenant",
            "user@ema.il",
            processBusinessId,
            OffsetDateTime.now().minusMinutes(2),
            OffsetDateTime.now().minusMinutes(1),
            0,
            true
        );
    }

    private PBatch makeBatch(UUID batchId, PProcess process) {
        return new PBatch(
            "corr",
            batchId,
            process.getProcessId(),
            "tenant", "user", "role",
            List.of(),
            HashMap.empty(),
            true
        );
    }

}