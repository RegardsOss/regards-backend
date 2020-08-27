package fr.cnes.regards.modules.processing.plugins.impl;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.processing.client.IReactiveRolesClient;
import fr.cnes.regards.modules.processing.domain.*;
import fr.cnes.regards.modules.processing.domain.engine.IExecutable;
import fr.cnes.regards.modules.processing.domain.engine.IWorkloadEngine;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionFileParameterValue;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionStringParameterValue;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import fr.cnes.regards.modules.processing.plugins.repository.ProcessRepositoryImpl;
import fr.cnes.regards.modules.processing.repository.IRightsPluginConfigurationRepository;
import fr.cnes.regards.modules.processing.repository.IWorkloadEngineRepository;
import fr.cnes.regards.modules.processing.storage.*;
import fr.cnes.regards.modules.processing.utils.IPUserAuthFactory;
import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class SimpleShellProcessPluginTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleShellProcessPluginTest.class);

    @Test
    public void runSimpleScript() throws Exception {
        UUID execId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();

        Path tempWorkdirBase = Files.createTempDirectory("workdirs");
        Path tempStorageBase = Files.createTempDirectory("storage");

        IExecutionLocalWorkdirService workdirService = new ExecutionLocalWorkdirService(
                tempWorkdirBase,
                new DownloadService(
                    Proxy.NO_PROXY,
                    HashSet.empty(),
                    null
                )
        );
        ISharedStorageService storageService = new SharedStorageService(tempStorageBase);

        IWorkloadEngine engine = makeEngine();
        IWorkloadEngineRepository engineRepo = makeEngineRepo(engine);
        ProcessRepositoryImpl processRepo = makeProcessRepo(engineRepo);
        SimpleShellProcessPlugin shellProcessPlugin = makePlugin(workdirService, storageService);

        RightsPluginConfiguration rpc = makeRightsPluginConfig();
        PProcess process = processRepo.fromPlugin(rpc, shellProcessPlugin).block();
        PBatch batch = makeBatch(batchId, process);
        PExecution exec = makeExec(execId, batchId, batch.getProcessBusinessId());
        ExecutionContext ctx = new ExecutionContext(exec, batch, process);


        AtomicReference<Seq<POutputFile>> outputFiles = new AtomicReference<>();
        IExecutable executable = shellProcessPlugin.executable();
        Flux.<PStep>create(sink -> {
                executable
                    .execute(ctx, sink)
                    .doOnTerminate(sink::complete)
                    .subscribe(
                        outputFiles::set,
                        e -> LOGGER.error(e.getMessage(), e)
                    );
            })
            .doOnSubscribe(s -> LOGGER.info("Flux subscription: {}", s))
            .doOnError(t -> LOGGER.error(t.getMessage(), t))
            .doOnNext(s -> LOGGER.info("Step received: {}", s))
            .collect(List.collector())
            .block();
    }

    @NotNull public RightsPluginConfiguration makeRightsPluginConfig() {
        return new RightsPluginConfiguration(
                1L,
                new PluginConfiguration("label", SimpleShellProcessPlugin.SIMPLE_SHELL_PROCESS_PLUGIN),
                "tenant",
                "EXPLOIT",
                List.<Long>empty().toJavaList()
        );
    }

    private ProcessRepositoryImpl makeProcessRepo(IWorkloadEngineRepository engineRepo) throws Exception {
        IRightsPluginConfigurationRepository rightsRepo = Mockito.mock(IRightsPluginConfigurationRepository.class);
        when(rightsRepo.findByPluginConfiguration(any())).thenAnswer(i -> makeRightsPluginConfig());
        IReactiveRolesClient rolesClient = Mockito.mock(IReactiveRolesClient.class);
        when(rolesClient.shouldAccessToResourceRequiring(anyString(), anyString())).thenReturn(Mono.just(true));
        IPUserAuthFactory authFactory = Mockito.mock(IPUserAuthFactory.class);
        when(authFactory.authFromUserEmailAndRole(anyString(), anyString(), anyString()))
                .thenAnswer(i -> new PUserAuth(i.getArgument(0), i.getArgument(1), i.getArgument(2), "authToken"));

        return new ProcessRepositoryImpl(
                Mockito.mock(IPluginService.class),
                engineRepo,
                rightsRepo,
                Mockito.mock(IRuntimeTenantResolver.class),
                rolesClient,
                authFactory
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

    private SimpleShellProcessPlugin makePlugin(IExecutionLocalWorkdirService workdirService,
            ISharedStorageService storageService) {
        SimpleShellProcessPlugin shellProcessPlugin = new SimpleShellProcessPlugin(workdirService,
                                                                                         storageService);
        // TODO: try removing these two setters to fix a cast exception
        shellProcessPlugin.setDurationForecast("10min");
        shellProcessPlugin.setSizeForecast("*1");

        shellProcessPlugin.setShellScriptName(Paths.get("src/test/resources/simpleScript.sh").toFile().getAbsolutePath());
        shellProcessPlugin.setEnvVariableNames(List.of("SIMPLE_FOO", "SIMPLE_BAR").toJavaList());

        return shellProcessPlugin;

    }

    private PExecution makeExec(UUID execId, UUID batchId, UUID processBusinessId) throws Exception {
        return new PExecution(
            execId, batchId,
            Duration.ofSeconds(10),
            List.of(
                    new ExecutionFileParameterValue("one", "one.raw", "text/plain", Paths.get("src/test/resources/one.raw").toUri().toURL(), 3L, "checksum", false),
                    new ExecutionFileParameterValue("two", "two.raw", "text/plain", Paths.get("src/test/resources/two.raw").toUri().toURL(), 3L, "checksum", false)
            ),
            List.empty(),
            "tenant",
            "user@ema.il",
            processBusinessId,
            "processName",
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
            process.getBusinessId(),
            process.getProcessName(),
            "tenant", "user", "role",
            List.of(
                new ExecutionStringParameterValue("SIMPLE_FOO", "foo"),
                new ExecutionStringParameterValue("SIMPLE_BAR", "bar")
            ),
            HashMap.empty(),
            true
        );
    }

}