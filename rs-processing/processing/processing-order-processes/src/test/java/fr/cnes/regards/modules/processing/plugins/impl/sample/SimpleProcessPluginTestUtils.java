/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PInputFile;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.domain.engine.IWorkloadEngine;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.repository.IWorkloadEngineRepository;
import fr.cnes.regards.modules.processing.domain.service.IPUserAuthService;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import static fr.cnes.regards.modules.processing.order.Constants.FEATURE_ID;
import static fr.cnes.regards.modules.processing.order.Constants.INTERNAL;
import fr.cnes.regards.modules.processing.repository.IRightsPluginConfigurationRepository;
import fr.cnes.regards.modules.processing.repository.OrderProcessRepositoryImpl;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import reactor.core.publisher.Mono;

/**
 * @author Iliana Ghazali
 **/
public class SimpleProcessPluginTestUtils {

    public static final Path resourceFileBasePath = Paths.get("src/test/resources/").toAbsolutePath();

    public static PBatch makeBatch(UUID batchId, PProcess process) {
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

    public static PExecution makeExec(UUID execId, UUID batchId, UUID processBusinessId) throws Exception {
        return new PExecution(
                execId, "exec cid", batchId, "batch cid",
                Duration.ofSeconds(10),
                List.of(new PInputFile("one", "one.raw", "text/plain",
                                       resourceFileBasePath.resolve("one.raw").toUri().toURL(), 3L,
                                       "checksum", inputMetadataAsMap(false, "urn"), "one"),
                        new PInputFile("two", "two.raw", "text/plain",
                                       resourceFileBasePath.resolve("two.raw")
                                               .toUri().toURL(), 3L, "checksum", inputMetadataAsMap(false, "urn"),
                                       "two")), List.empty(),
                "tenant",
                "user@ema.il",
                processBusinessId,
                OffsetDateTime.now().minusMinutes(2),
                OffsetDateTime.now().minusMinutes(1),
                0,
                true
        );
    }

    public static IWorkloadEngine makeEngine() {
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


    public static IWorkloadEngineRepository makeEngineRepo(IWorkloadEngine engine) {
        return new IWorkloadEngineRepository() {
            @Override public Mono<IWorkloadEngine> findByName(String name) {
                return Mono.just(engine);
            }
            @Override public Mono<IWorkloadEngine> register(IWorkloadEngine engine) {
                return Mono.just(engine);
            }
        };
    }

    public static OrderProcessRepositoryImpl makeProcessRepo(IWorkloadEngineRepository engineRepo) {
        IRightsPluginConfigurationRepository rightsRepo = mock(IRightsPluginConfigurationRepository.class);
        IPUserAuthService authFactory = mock(IPUserAuthService.class);
       /* when(authFactory.authFromUserEmailAndRole(anyString(), anyString(), anyString()))
                .thenAnswer(i -> new PUserAuth(i.getArgument(0), i.getArgument(1), i.getArgument(2), "authToken"));*/
        return new OrderProcessRepositoryImpl(
                mock(IPluginService.class),
                engineRepo,
                rightsRepo,
                mock(IRuntimeTenantResolver.class)
        );
    }

    public static OrderProcessRepositoryImpl makeProcessRepo(IWorkloadEngineRepository engineRepo,
            RightsPluginConfiguration rightsConfig) {
        IRightsPluginConfigurationRepository rightsRepo = mock(IRightsPluginConfigurationRepository.class);
        when(rightsRepo.findByPluginConfiguration(any())).thenAnswer(i -> rightsConfig);
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


    @NotNull
    public static RightsPluginConfiguration makeRightsPluginConfig(String pluginId) {
        UUID bid = UUID.randomUUID();
        PluginConfiguration pc = new PluginConfiguration("label", pluginId);
        pc.setBusinessId(bid.toString());

        return new RightsPluginConfiguration(
                1L, pc, bid,
                "EXPLOIT",
                new String[]{},
                false
        );
    }

    public static HashMap<String, String> inputMetadataAsMap(boolean internal, String urn) {
        return HashMap.of(INTERNAL, internal + "", FEATURE_ID, urn);
    }
}
