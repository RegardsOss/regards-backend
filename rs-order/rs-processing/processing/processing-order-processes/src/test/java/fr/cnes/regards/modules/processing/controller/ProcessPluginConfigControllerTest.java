package fr.cnes.regards.modules.processing.controller;

import feign.Feign;
import fr.cnes.regards.framework.feign.FeignContractSupplier;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.processing.dto.ProcessLabelDTO;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;
import fr.cnes.regards.modules.processing.plugins.impl.UselessProcessPlugin;
import fr.cnes.regards.modules.processing.testutils.servlet.AbstractProcessingTest;
import fr.cnes.regards.modules.processing.utils.gson.GsonLoggingDecoder;
import fr.cnes.regards.modules.processing.utils.gson.GsonLoggingEncoder;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.*;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ProcessPluginConfigControllerTest  extends AbstractProcessingTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPluginConfigControllerTest.class);

    private Client client;

    @Test
    public void test_list_metadata() {
        runtimeTenantResolver.forceTenant(TENANT_PROJECTA);
        FeignSecurityManager.asUser("regards@cnes.fr", DefaultRole.ADMIN.name());

        // LIST AVAILABLE PLUGINS
        List<PluginMetaData> pluginMetaData = client.listAllDetectedMetadata();

        pluginMetaData
                .forEach(md -> LOGGER.info("Found md {}: {}", md.getPluginId(), md));

        assertThat(pluginMetaData).hasSize(2);
        assertThat(pluginMetaData).anyMatch(md -> md.getPluginClassName().equals(UselessProcessPlugin.class.getName()));

        // LIST AVAILABLE CONFIGURATIONS: NOTHING YET...
        FeignSecurityManager.asUser("regards@cnes.fr", DefaultRole.ADMIN.name());
        List<ProcessPluginConfigurationRightsDTO> pluginConfigs = client.findAll();
        int initSize = pluginConfigs.size();

        // CREATE A CONFIG
        PluginConfiguration useless1Config = new PluginConfiguration("useless1 label", UselessProcessPlugin.class.getSimpleName());
        useless1Config.setVersion("1.0");
        useless1Config.setPriorityOrder(1);
        useless1Config.setParameters(IPluginParam.set(
                IPluginParam.build("processName", "useless-processName-1")
        ));
        io.vavr.collection.List<String> datasets = list(randomDataset(), randomDataset());
        ProcessPluginConfigurationRightsDTO created = client.create(new ProcessPluginConfigurationRightsDTO(
                useless1Config,
                new ProcessPluginConfigurationRightsDTO.Rights("EXPLOIT", datasets)
        ));

        // THERE IS THE CONFIG IN THE DATABASE!
        ProcessPluginConfigurationRightsDTO fetched = client.findByBusinessId(UUID.fromString(created.getPluginConfiguration().getBusinessId()));
        assertThat(fetched.getPluginConfiguration().getParameter("processName").getValue()).isEqualTo("useless-processName-1");

        // UPDATE THE CONFIG
        fetched.getPluginConfiguration().setParameters(IPluginParam.set(
                IPluginParam.build("processName", "useless-processName-2")
        ));
        ProcessPluginConfigurationRightsDTO toBeUpdated = new ProcessPluginConfigurationRightsDTO(
                fetched.getPluginConfiguration(),
                new ProcessPluginConfigurationRightsDTO.Rights("ADMIN", datasets)
        );
        ProcessPluginConfigurationRightsDTO updated = client
                .update(UUID.fromString(toBeUpdated.getPluginConfiguration().getBusinessId()), toBeUpdated);
        assertThat(updated.getRights().getRole()).isEqualTo("ADMIN");
        assertThat(updated.getRights().getDatasets()).hasSameElementsAs(datasets);
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

    @Test
    public void test_manage_datasets() {
        runtimeTenantResolver.forceTenant(TENANT_PROJECTB);

        String initDataset = randomDataset();

        // Create 3 configs, all referencing initDataset
        ProcessPluginConfigurationRightsDTO rpc1 = createConfig(list(initDataset), "useless1 label", "useless-processName-1");
        ProcessPluginConfigurationRightsDTO rpc2 = createConfig(list(initDataset), "useless2 label", "useless-processName-2");
        ProcessPluginConfigurationRightsDTO rpc3 = createConfig(list(initDataset), "useless3 label", "useless-processName-3");

        // All processes should reference initDataset
        assertThat(extractIds(client.findProcessesByDataset(initDataset)))
                .containsExactlyInAnyOrder(idFor(rpc1), idFor(rpc2), idFor(rpc3));

        // Using a new dataset
        String newDataset = randomDataset();

        // Nobody knows the init dataset yet
        assertThat(extractIds(client.findProcessesByDataset(newDataset))).isEmpty();

        Map<String, Collection<ProcessLabelDTO>> map = client.findProcessesByDatasets(list(initDataset, newDataset).toJavaList());
        Option<Collection<ProcessLabelDTO>> lists = map.get(initDataset);
        Collection<ProcessLabelDTO> processLabelDTOS = lists.get();
        assertThat(extractIds(processLabelDTOS))
                .containsExactlyInAnyOrder(idFor(rpc1), idFor(rpc2), idFor(rpc3));
        assertThat(map.get(newDataset).get()).isEmpty();

        // Set the dataset to be referenced by rpc 1 and 2
        client.attachDatasetToProcesses(extractIds(rpc1, rpc2), newDataset);
        assertThat(extractIds(client.findProcessesByDataset(newDataset)))
                .containsExactlyInAnyOrder(idFor(rpc1), idFor(rpc2));

        Map<String, Collection<ProcessLabelDTO>> processesByDatasetsB = client.findProcessesByDatasets(list(initDataset, newDataset).toJavaList());
        assertThat(extractIds(processesByDatasetsB.get(initDataset).get()))
                .containsExactlyInAnyOrder(idFor(rpc1), idFor(rpc2), idFor(rpc3));
        assertThat(extractIds(processesByDatasetsB.get(newDataset).get()))
                .containsExactlyInAnyOrder(idFor(rpc1), idFor(rpc2));

        // Reset the dataset to be referenced by rpc 2 and 3
        client.attachDatasetToProcesses(extractIds(rpc2, rpc3), newDataset);
        assertThat(extractIds(client.findProcessesByDataset(newDataset)))
                .containsExactlyInAnyOrder(idFor(rpc2), idFor(rpc3));

        Map<String, Collection<ProcessLabelDTO>> processesByDatasetsC = client.findProcessesByDatasets(list(initDataset, newDataset).toJavaList());
        assertThat(extractIds(processesByDatasetsC.get(initDataset).get()))
                .containsExactlyInAnyOrder(idFor(rpc1), idFor(rpc2), idFor(rpc3));
        assertThat(extractIds(processesByDatasetsC.get(newDataset).get()))
                .containsExactlyInAnyOrder(idFor(rpc2), idFor(rpc3));
    }


    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================
    //==================================================================================================================

    public String randomDataset() {
        return "URN:DATASET:tenant:" + UUID.randomUUID();
    }

    private static UUID idFor(ProcessPluginConfigurationRightsDTO rpc1) {
        return UUID.fromString(rpc1.getPluginConfiguration().getBusinessId());
    }

    private static List<UUID> extractIds(Collection<ProcessLabelDTO> datasetAssociatedProcesses) {
        return datasetAssociatedProcesses.stream().map(dto -> dto.getProcessBusinessId()).collect(Collectors.toList());
    }

    private static List<UUID> extractIds(ProcessPluginConfigurationRightsDTO... dtos) {
        return list(dtos).map(rpc -> rpc.getPluginConfiguration().getBusinessId()).map(UUID::fromString).toJavaList();
    }

    private static <T> io.vavr.collection.List<T> list(T... ts) {
        return io.vavr.collection.List.of(ts);
    }

    private ProcessPluginConfigurationRightsDTO createConfig(io.vavr.collection.List<String> initDatasets, String s, String s2) {
        PluginConfiguration useless1Config = new PluginConfiguration(s, UselessProcessPlugin.class.getSimpleName());
        useless1Config.setVersion("1.0");
        useless1Config.setPriorityOrder(1);
        useless1Config.setParameters(IPluginParam.set(IPluginParam.build("processName", s2)));
        runtimeTenantResolver.forceTenant(TENANT_PROJECTA);
        return client.create(
                new ProcessPluginConfigurationRightsDTO(
                        useless1Config,
                        new ProcessPluginConfigurationRightsDTO.Rights("EXPLOIT", initDatasets)
                )
        );
    }

    @Before
    public void init() throws IOException, ModuleException {
        client = Feign.builder()
            .contract(new FeignContractSupplier().get())
            .decoder(new GsonLoggingDecoder(gson))
            .encoder(new GsonLoggingEncoder(gson))
            .logLevel(feign.Logger.Level.FULL)
            .target(new TokenClientProvider<>(Client.class, "http://" + serverAddress + ":" + port, feignSecurityManager));
        runtimeTenantResolver.forceTenant(TENANT_PROJECTA);
        FeignSecurityManager.asUser("regards@cnes.fr", DefaultRole.ADMIN.name());
    }

    @RestClient(name = "rs-processing-config", contextId = "rs-processing.rest.plugin-conf.client")
    public interface Client {

        @RequestMapping(
                method = RequestMethod.GET,
                path = PROCESS_CONFIG_PATH,
                consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE },
                produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
        List<ProcessPluginConfigurationRightsDTO> findAll();

        @RequestMapping(
                method = RequestMethod.GET,
                path = PROCESS_METADATA_PATH,
                consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE },
                produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
        List<PluginMetaData> listAllDetectedMetadata();

        @RequestMapping(
                method = RequestMethod.GET,
                path = PROCESS_CONFIG_BID_PATH,
                consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE },
                produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
        ProcessPluginConfigurationRightsDTO findByBusinessId(@PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId);

        @RequestMapping(
                method = RequestMethod.POST,
                path = PROCESS_CONFIG_PATH,
                consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE },
                produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
        ProcessPluginConfigurationRightsDTO create(
                @RequestBody ProcessPluginConfigurationRightsDTO rightsDto
        );

        @RequestMapping(
                method = RequestMethod.PUT,
                path = PROCESS_CONFIG_BID_PATH,
                produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE },
                consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
        ProcessPluginConfigurationRightsDTO update(
                @PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId,
                @RequestBody ProcessPluginConfigurationRightsDTO rightsDto
        );

        @RequestMapping(
                method = RequestMethod.DELETE,
                path = PROCESS_CONFIG_BID_PATH,
                consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE },
                produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
        ProcessPluginConfigurationRightsDTO delete(
                @PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId
        );

        @RequestMapping(
                method = RequestMethod.GET,
                path = PROCESS_METADATA_PATH,
                consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE },
                produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
        Collection<PluginMetaData> listAllDetectedPlugins();

        @RequestMapping(
                method = RequestMethod.GET,
                path = PROCESS_LINKDATASET_PATH,
                consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE },
                produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
        Collection<ProcessLabelDTO> findProcessesByDataset(
                @PathVariable(DATASET_PARAM) String dataset
        );

        @RequestMapping(
                method = RequestMethod.POST,
                path = PROCESS_BY_DATASETS_PATH,
                consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE },
                produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
        Map<String, Collection<ProcessLabelDTO>> findProcessesByDatasets(
                @RequestBody List<String> datasets
        );

        @RequestMapping(
                method = RequestMethod.PUT,
                path = PROCESS_LINKDATASET_PATH,
                consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
        void attachDatasetToProcesses (
                @RequestBody List<UUID> processBusinessIds,
                @PathVariable(DATASET_PARAM) String dataset
        );

        @RequestMapping(
                method = RequestMethod.PUT,
                path = PROCESS_CONFIG_BID_USERROLE_PATH,
                consumes = MediaType.ALL_VALUE)
        void attachRoleToProcess (
                @PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId,
                @RequestParam(USER_ROLE_PARAM) String userRole
        );
    }

}