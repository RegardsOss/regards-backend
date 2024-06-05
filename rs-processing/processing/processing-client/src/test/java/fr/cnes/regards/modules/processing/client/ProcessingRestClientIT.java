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
package fr.cnes.regards.modules.processing.client;

import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.domain.dto.PBatchRequest;
import fr.cnes.regards.modules.processing.domain.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.domain.repository.IWorkloadEngineRepository;
import fr.cnes.regards.modules.processing.domain.service.IBatchService;
import fr.cnes.regards.modules.processing.domain.service.IProcessService;
import fr.cnes.regards.modules.processing.testutils.servlet.AbstractProcessingIT;
import fr.cnes.regards.modules.processing.testutils.servlet.TestSpringConfiguration;
import io.vavr.collection.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static fr.cnes.regards.modules.processing.utils.random.RandomUtils.randomInstance;
import static fr.cnes.regards.modules.processing.utils.random.RandomUtils.randomList;
import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = { TestSpringConfiguration.class, ProcessingRestClientIT.Config.class })
public class ProcessingRestClientIT extends AbstractProcessingIT {


    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingRestClientIT.class);

    private IProcessingRestClient client;

    @Test
    public void download() {
        List<PProcessDTO> ps = client.listAll().getBody();
        ps.forEach(p -> LOGGER.info("Found process: {}", p));
        assertThat(ps).isEqualTo(Values.processes);
    }

    @Before
    public void init() throws IOException, ModuleException {
        runtimeTenantResolver.forceTenant(TENANT_PROJECTA);
        client = FeignClientBuilder.build(new TokenClientProvider<>(IProcessingRestClient.class,
                                                                    "http://" + serverAddress + ":" + port,
                                                                    feignSecurityManager), gson);
        FeignSecurityManager.asSystem();
    }

    interface Values {

        List<PProcessDTO> processes = randomList(PProcessDTO.class, 20);
        PBatch batch = randomInstance(PBatch.class);
    }

    @Configuration
    static class Config {

        @Bean
        public IProcessService processService() {
            return new IProcessService() {

                @Override
                public Flux<PProcessDTO> findByTenant(String tenant) {
                    return Flux.fromIterable(Values.processes);
                }
            };
        }

        @Bean
        public IBatchService batchService() {
            return new IBatchService() {

                @Override
                public Mono<PBatch> checkAndCreateBatch(PUserAuth auth, PBatchRequest data) {
                    return Mono.just(Values.batch);
                }
            };
        }

        @Bean
        public IWorkloadEngineRepository engineRepo() {
            return Mockito.mock(IWorkloadEngineRepository.class);
        }
    }

}
