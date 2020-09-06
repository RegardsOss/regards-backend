package fr.cnes.regards.modules.processing.controller;

import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;
import fr.cnes.regards.modules.processing.plugins.impl.UselessProcessPlugin;
import fr.cnes.regards.modules.processing.testutils.AbstractProcessingTest;
import fr.cnes.regards.modules.processing.utils.gson.GsonLoggingDecoder;
import fr.cnes.regards.modules.processing.utils.gson.GsonLoggingEncoder;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.*;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.PROCESS_BUSINESS_ID_PARAM;
import static org.assertj.core.api.Assertions.assertThat;

public class ProcessPluginConfigControllerTest  extends AbstractProcessingTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPluginConfigControllerTest.class);

    private Client client;

    @Test
    public void test_list_metadata() {
        runtimeTenantResolver.forceTenant(TENANT_PROJECTA);

        // LIST AVAILABLE PLUGINS
        List<PluginMetaData> pluginMetaData = client.findAllMetadata();

        pluginMetaData
                .forEach(md -> LOGGER.info("Found md {}: {}", md.getPluginId(), md));

        assertThat(pluginMetaData).hasSize(2);
        assertThat(pluginMetaData).anyMatch(md -> md.getPluginClassName().equals(UselessProcessPlugin.class.getName()));

        // LIST AVAILABLE CONFIGURATIONS: NOTHING YET...
        List<ProcessPluginConfigurationRightsDTO> pluginConfigs = client.findAll();
        int initSize = pluginConfigs.size();

        // CREATE A CONFIG
        PluginConfiguration useless1Config = new PluginConfiguration("useless1 label", UselessProcessPlugin.class.getSimpleName());
        useless1Config.setVersion("1.0");
        useless1Config.setPriorityOrder(1);
        useless1Config.setParameters(IPluginParam.set(
                IPluginParam.build("processName", "useless-processName-1")
        ));
        ProcessPluginConfigurationRightsDTO created = client.create(new ProcessPluginConfigurationRightsDTO(
                useless1Config,
                new ProcessPluginConfigurationRightsDTO.Rights("EXPLOIT", io.vavr.collection.List.of(1L, 2L))
        ));

        String tenant = runtimeTenantResolver.getTenant();

        // THERE IS THE CONFIG IN THE DATABASE!
        ProcessPluginConfigurationRightsDTO fetched = client.findByUUID(UUID.fromString(created.getPluginConfiguration().getBusinessId()));
        assertThat(fetched.getPluginConfiguration().getParameter("processName").getValue()).isEqualTo("useless-processName-1");

        // UPDATE THE CONFIG
        fetched.getPluginConfiguration().setParameters(IPluginParam.set(
                IPluginParam.build("processName", "useless-processName-2")
        ));
        ProcessPluginConfigurationRightsDTO toBeUpdated = new ProcessPluginConfigurationRightsDTO(
                fetched.getPluginConfiguration(),
                new ProcessPluginConfigurationRightsDTO.Rights("ADMIN", io.vavr.collection.List.of(1L, 2L, 3L))
        );
        ProcessPluginConfigurationRightsDTO updated = client
                .update(UUID.fromString(toBeUpdated.getPluginConfiguration().getBusinessId()), toBeUpdated);
        assertThat(updated.getRights().getRole()).isEqualTo("ADMIN");
        assertThat(updated.getRights().getDatasets()).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(updated.getPluginConfiguration().getParameter("processName").getValue()).isEqualTo("useless-processName-2");

        // LIST AGAIN: THERE IS ONE CONFIG!
        List<ProcessPluginConfigurationRightsDTO> pluginConfigsWithUseless2 = client.findAll();
        pluginConfigsWithUseless2.forEach(pc -> LOGGER.info("Found pc {}: {}", pc.getPluginConfiguration().getPluginId(), pc));
        assertThat(pluginConfigsWithUseless2).hasSize(initSize + 1);
        assertThat(pluginConfigsWithUseless2).anyMatch(pc -> Try.of(() -> pc.getPluginConfiguration().getParameter("processName").getValue().equals("useless-processName-2")).getOrElse(false));

        // NOW DELETE IT
        ProcessPluginConfigurationRightsDTO toBeDeleted = pluginConfigsWithUseless2.get(0);
        client.delete(UUID.fromString(toBeDeleted.getPluginConfiguration().getBusinessId()));

        // LIST AVAILABLE CONFIGURATIONS: NOTHING ANYMORE...
        List<ProcessPluginConfigurationRightsDTO> pluginConfigsFinal = client.findAll();
        pluginConfigsFinal.forEach(pc -> LOGGER.info("Found pc {}: {}", pc.getPluginConfiguration().getPluginId(), pc));
        assertThat(pluginConfigsFinal).hasSize(initSize);

    }

    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================

    @Before
    public void init() throws IOException, ModuleException {
        client = Feign.builder()
            .decoder(new GsonLoggingDecoder(gson))
            .encoder(new GsonLoggingEncoder(gson))
            .target(new TokenClientProvider<>(Client.class, "http://" + serverAddress + ":" + port, feignSecurityManager));
        runtimeTenantResolver.forceTenant(TENANT_PROJECTA);
        FeignSecurityManager.asSystem();
    }

    @RestClient(name = "rs-processing-config", contextId = "rs-processing.rest.plugin-conf.client")
    @Headers({ "Accept: application/json", "Content-Type: application/json" })
    public interface Client {
        @RequestLine("GET " + PROCESS_METADATA_PATH)
        List<PluginMetaData> findAllMetadata();

        @RequestLine("GET " + PROCESS_CONFIG_PATH)
        List<ProcessPluginConfigurationRightsDTO> findAll();

        @RequestLine("GET " + PROCESS_CONFIG_BID_PATH)
        ProcessPluginConfigurationRightsDTO findByUUID(@Param(PROCESS_BUSINESS_ID_PARAM) UUID id);

        @RequestLine("POST " + PROCESS_CONFIG_PATH)
        ProcessPluginConfigurationRightsDTO create(ProcessPluginConfigurationRightsDTO config);

        @RequestLine("PUT " + PROCESS_CONFIG_BID_PATH)
        ProcessPluginConfigurationRightsDTO update(@Param(PROCESS_BUSINESS_ID_PARAM) UUID id, ProcessPluginConfigurationRightsDTO config);

        @RequestLine("DELETE " + PROCESS_CONFIG_BID_PATH)
        ProcessPluginConfigurationRightsDTO delete(@Param(PROCESS_BUSINESS_ID_PARAM) UUID id);
    }

}