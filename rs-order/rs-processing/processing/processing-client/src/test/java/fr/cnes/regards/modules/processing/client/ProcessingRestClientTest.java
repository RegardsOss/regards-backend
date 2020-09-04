package fr.cnes.regards.modules.processing.client;

import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.dto.PBatchRequest;
import fr.cnes.regards.modules.processing.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.repository.IPProcessRepository;
import fr.cnes.regards.modules.processing.repository.IWorkloadEngineRepository;
import fr.cnes.regards.modules.processing.service.IBatchService;
import fr.cnes.regards.modules.processing.service.IProcessService;
import fr.cnes.regards.modules.processing.testutils.AbstractProcessingTest;
import fr.cnes.regards.modules.processing.testutils.TestSpringConfiguration;
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

import static fr.cnes.regards.modules.processing.testutils.RandomUtils.randomInstance;
import static fr.cnes.regards.modules.processing.testutils.RandomUtils.randomList;
import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = { TestSpringConfiguration.class, ProcessingRestClientTest.Config.class })
public class ProcessingRestClientTest extends AbstractProcessingTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingRestClientTest.class);

    private IProcessingRestClient client;

    @Test
    public void download() {
        List<PProcessDTO> ps = client.listAll().getBody();

        ps.forEach(p -> LOGGER.info("Found process: {}", p));

        assertThat(ps).isEqualTo(Values.processes);
    }

    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================

    @Before
    public void init() throws IOException, ModuleException {
        runtimeTenantResolver.forceTenant(TENANT_PROJECTA);
        client = FeignClientBuilder.build(
                new TokenClientProvider<>(IProcessingRestClient.class,
                                          "http://" + serverAddress + ":" + port, feignSecurityManager),
                gson);
        FeignSecurityManager.asSystem();
    }

    interface Values {
        List<PProcessDTO> processes = randomList(PProcessDTO.class, 20);
        PBatch batch = randomInstance(PBatch.class);
    }

    @Configuration
    static class Config {

        @Bean public IProcessService processService() {
            return new IProcessService() {
                @Override public Flux<PProcessDTO> findByTenant(String tenant) {
                    return Flux.fromIterable(Values.processes);
                }
            };
        }
        @Bean public IBatchService batchService() {
            return new IBatchService() {
                @Override public Mono<PBatch> checkAndCreateBatch(PUserAuth auth, PBatchRequest data) {
                    return Mono.just(Values.batch);
                }
            };
        }
        @Bean public IWorkloadEngineRepository engineRepo() {
            return Mockito.mock(IWorkloadEngineRepository.class);
        }
    }

}
