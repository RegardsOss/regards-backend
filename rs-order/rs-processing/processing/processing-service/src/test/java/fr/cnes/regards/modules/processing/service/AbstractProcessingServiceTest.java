/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

/**
 * TODO: AbstractServiceTest description
 *
 * @author gandrieu
 */
package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.dao.IBatchEntityRepository;
import fr.cnes.regards.modules.processing.dao.IExecutionEntityRepository;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.PInputFile;
import fr.cnes.regards.modules.processing.domain.PProcess;
import fr.cnes.regards.modules.processing.domain.constraints.ConstraintChecker;
import fr.cnes.regards.modules.processing.domain.engine.IOutputToInputMapper;
import fr.cnes.regards.modules.processing.domain.engine.IWorkloadEngine;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import fr.cnes.regards.modules.processing.domain.forecast.IResultSizeForecast;
import fr.cnes.regards.modules.processing.domain.repository.IPBatchRepository;
import fr.cnes.regards.modules.processing.domain.repository.IPExecutionRepository;
import fr.cnes.regards.modules.processing.domain.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.domain.repository.IWorkloadEngineRepository;
import fr.cnes.regards.modules.processing.domain.service.IDownloadService;
import fr.cnes.regards.modules.processing.testutils.servlet.AbstractProcessingTest;
import io.vavr.Function1;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class AbstractProcessingServiceTest extends AbstractProcessingTest {

    public static final String THE_TENANT = "the-tenant";
    public static final String THE_USER = "the-user";
    public static final String THE_ROLE = "the-role";
    public static final String THE_TOKEN = "the-token";

    @Autowired protected BatchServiceImpl batchService;
    @Autowired protected ProcessUpdater processUpdater;
    @Autowired protected IPBatchRepository batchRepo;
    @Autowired protected IBatchEntityRepository batchEntityRepo;
    @Autowired protected ExecutionServiceImpl executionService;
    @Autowired protected IPExecutionRepository execRepo;
    @Autowired protected IExecutionEntityRepository execEntityRepo;

    @PostConstruct
    public void init() {
        configureProcessUpdater(p -> p);
    }

    protected void configureProcessUpdater(Function1<PProcess.ConcretePProcess, PProcess.ConcretePProcess> fn) {
        Mockito.reset(processUpdater);
        when(processUpdater.update(any())).thenAnswer(i -> fn.apply(i.getArgument(0)));
    }

    public interface ProcessUpdater {
        PProcess.ConcretePProcess update(PProcess.ConcretePProcess p);
    }

    @Configuration
    protected static class Config {

        protected String processName = "the-process-name";

        @Bean
        public ProcessUpdater processUpdater() {
            return Mockito.mock(ProcessUpdater.class);
        }

        @Bean
        public IWorkloadEngineRepository workloadEngineRepository() {
            return new IWorkloadEngineRepository() {
                @Override public Mono<IWorkloadEngine> findByName(String name) { return Mono.just(makeWorkloadEngine()); }
                @Override public Mono<IWorkloadEngine> register(IWorkloadEngine engine) { return Mono.just(engine); }
            };
        }

        @NotNull
        private IWorkloadEngine makeWorkloadEngine() {
            return new IWorkloadEngine() {
                @Override public String name() { return "TEST"; }
                @Override public Mono<PExecution> run(ExecutionContext context) {
                    return context
                        .getProcess()
                        .getExecutable()
                        .execute(context)
                        .map(ExecutionContext::getExec);
                }
                @Override public void selfRegisterInRepo() {}
            };
        }

        @Primary
        @Bean
        public IPProcessRepository processRepo(ProcessUpdater processUpdater) {
            return new IPProcessRepository() {
                @Override public Flux<PProcess> findAllByTenant(String tenant) { return Flux.empty(); }
                @Override public Mono<PProcess> findByTenantAndProcessBusinessID(String tenant, UUID processId) {
                    return Mono.just(processUpdater.update(makeProcess(processId)));
                }
                @Override
                public Mono<PProcess> findByBatch(PBatch batch) {
                    return Mono.just(processUpdater.update(makeProcess(batch.getProcessBusinessId())));
                }
            };
        }

        @Primary @Bean public IDownloadService downloadService() {
            return new IDownloadService() {
                @Override
                public Mono<Path> download(PInputFile file, Path dest) {
                    return Mono.just(dest);
                }
            };
        }

        protected PProcess.ConcretePProcess makeProcess(UUID processUuid) {
            return new PProcess.ConcretePProcess(
                    processUuid, processName,
                    HashMap.empty(), true,
                    ConstraintChecker.noViolation(), ConstraintChecker.noViolation(),
                    List.empty(),
                    new IResultSizeForecast() {
                        @Override public long expectedResultSizeInBytes(long inputSizeInBytes) { return inputSizeInBytes; }
                        @Override public String format() { return "*1"; }
                    },
                    inputSizeInBytes -> Duration.ofMillis(inputSizeInBytes),
                    makeWorkloadEngine(),
                    Mono::just,
                    IOutputToInputMapper.allMappings()
            );
        }
    }

}
