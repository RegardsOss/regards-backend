package fr.cnes.regards.modules.processing.rest;

import feign.*;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.dto.PProcessDTO;
import fr.cnes.regards.modules.processing.repository.IWorkloadEngineRepository;
import fr.cnes.regards.modules.processing.testutils.AbstractProcessingTest;
import fr.cnes.regards.modules.processing.testutils.GsonLoggingDecoder;
import fr.cnes.regards.modules.processing.testutils.GsonLoggingEncoder;
import fr.cnes.regards.modules.processing.testutils.TestSpringConfiguration;
import fr.cnes.regards.modules.processing.utils.TimeUtils;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.reactive.config.EnableWebFlux;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.MONITORING_EXECUTIONS_PATH;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.*;
import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.PREPARE;
import static fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus.RUNNING;
import static fr.cnes.regards.modules.processing.testutils.RandomUtils.randomList;
import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;
import static java.util.Arrays.asList;

@ContextConfiguration(
        classes = { TestSpringConfiguration.class, PMonitoringControllerTest.Config.class }
)
public class PMonitoringControllerTest extends AbstractProcessingTest {

    @Test public void executions() {

        List<PExecution> response = client
            .executions(TENANT_PROJECTA, asList(RUNNING, PREPARE),
                toMap(
                    PageRequest.of(0, 10)
                        //, Tuple.of(USER_EMAIL_PARAM, "a@a.a")
                        //, Tuple.of(DATE_FROM_PARAM, nowUtc().withNano(0).minusHours(2).toString())
                        //, Tuple.of(DATE_TO_PARAM, nowUtc().withNano(0).minusHours(1).toString())
                )
            );

        // TODO: the actual test, now that doing nothin runs...

        LOGGER.info("Resp: {}", response);

    }

    private Map<String, String> toMap(Pageable page, Tuple2<String,String>... rest) {
        return Stream.of(rest).foldLeft(HashMap.of(
            PAGE_PARAM, "" + page.getPageNumber(),
            SIZE_PARAM, "" + page.getPageSize()
        ), (acc, t) -> acc.put(t)).toJavaMap();
    }

    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================

    private static final Logger LOGGER = LoggerFactory.getLogger(PMonitoringControllerTest.class);

    private Client client;

    @Before
    public void init() throws IOException, ModuleException {
        client = Feign.builder()
                .decoder(new GsonLoggingDecoder(gson))
                .encoder(new GsonLoggingEncoder(gson))
                .target(new TokenClientProvider<>(Client.class, "http://" + serverAddress + ":" + port, feignSecurityManager));
        runtimeTenantResolver.forceTenant(TENANT_PROJECTA);
        FeignSecurityManager.asSystem();
    }

    interface Values {
        io.vavr.collection.List<PProcessDTO> processes = randomList(PProcessDTO.class, 20);
    }

    @RestClient(name = "rs-processing-config", contextId = "rs-processing.rest.plugin-conf.client")
    @Headers({ "Accept: application/json", "Content-Type: application/json" })
    public interface Client {
        @RequestLine("GET " + MONITORING_EXECUTIONS_PATH + "?tenant={tenant}&status={status}")
        List<PExecution> executions(
                @Param("tenant") String tenant,
                @Param("status") java.util.List<ExecutionStatus> status,
                @QueryMap Map<String,String> params
        );
    }


    @Configuration
    static class Config {
        @Bean
        public IWorkloadEngineRepository workloadEngineRepository() {
            return Mockito.mock(IWorkloadEngineRepository.class);
        }
    }


}