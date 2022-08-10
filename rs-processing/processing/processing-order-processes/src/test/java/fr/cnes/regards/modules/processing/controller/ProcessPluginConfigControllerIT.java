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
package fr.cnes.regards.modules.processing.controller;

import feign.Feign;
import fr.cnes.regards.framework.feign.FeignContractSupplier;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.processing.ProcessingConstants;
import fr.cnes.regards.modules.processing.dto.ProcessLabelDTO;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;
import fr.cnes.regards.modules.processing.plugins.impl.sample.UselessProcessPlugin;
import fr.cnes.regards.modules.processing.testutils.servlet.AbstractProcessingIT;
import fr.cnes.regards.modules.processing.utils.gson.GsonLoggingDecoder;
import fr.cnes.regards.modules.processing.utils.gson.GsonLoggingEncoder;
import io.vavr.collection.Array;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ProcessPluginConfigControllerIT extends AbstractProcessingIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPluginConfigControllerIT.class);

    private Client client;

    @Test
    public void test_list_metadata() {
        runtimeTenantResolver.forceTenant(TENANT_PROJECTA);
        FeignSecurityManager.asUser("regards@cnes.fr", DefaultRole.ADMIN.name());

        // LIST AVAILABLE PLUGINS
        List<PluginMetaData> pluginMetaData = client.listAllDetectedMetadata();

        pluginMetaData.forEach(md -> LOGGER.info("Found md {}: {}", md.getPluginId(), md));

        assertThat(pluginMetaData).hasSize(1)
                                  .anyMatch(md -> md.getPluginClassName().equals(UselessProcessPlugin.class.getName()));

        // LIST AVAILABLE CONFIGURATIONS: NOTHING YET...
        FeignSecurityManager.asUser("regards@cnes.fr", DefaultRole.ADMIN.name());
        List<ProcessPluginConfigurationRightsDTO> pluginConfigs = Array.ofAll(client.findAll())
                                                                       .map(EntityModel::getContent)
                                                                       .asJava();
        int initSize = pluginConfigs.size();

        // CREATE A CONFIG
        PluginConfiguration useless1Config = new PluginConfiguration("useless1 label",
                                                                     UselessProcessPlugin.class.getSimpleName());
        useless1Config.setVersion("1.0.0-SNAPSHOT");
        useless1Config.setPriorityOrder(1);
        useless1Config.setParameters(IPluginParam.set(IPluginParam.build("processName", "useless-processName-1")));
        useless1Config.setBusinessId(null);
        io.vavr.collection.List<String> datasets = list(randomDataset(), randomDataset());
        ProcessPluginConfigurationRightsDTO created = Objects.requireNonNull(client.create(new ProcessPluginConfigurationRightsDTO(
            useless1Config,
            new ProcessPluginConfigurationRightsDTO.Rights("EXPLOIT", datasets, false))).getContent());

        // THERE IS THE CONFIG IN THE DATABASE!
        ProcessPluginConfigurationRightsDTO fetched =  Objects.requireNonNull(client.findByBusinessId(UUID.fromString(created.getPluginConfiguration()
                                                                                                     .getBusinessId()))
                                                            .getContent());
        assertThat(fetched.getPluginConfiguration().getParameter("processName").getValue()).isEqualTo(
            "useless-processName-1");

        // UPDATE THE CONFIG
        fetched.getPluginConfiguration()
               .setParameters(IPluginParam.set(IPluginParam.build("processName", "useless-processName-2")));
        ProcessPluginConfigurationRightsDTO toBeUpdated = new ProcessPluginConfigurationRightsDTO(fetched.getPluginConfiguration(),
                                                                                                  new ProcessPluginConfigurationRightsDTO.Rights(
                                                                                                      "ADMIN",
                                                                                                      datasets,
                                                                                                      false));
        ProcessPluginConfigurationRightsDTO updated =  Objects.requireNonNull(client.update(UUID.fromString(toBeUpdated.getPluginConfiguration()
                                                                                               .getBusinessId()),
                                                                    toBeUpdated).getContent());
        assertThat(updated.getRights().getRole()).isEqualTo("ADMIN");
        assertThat(updated.getRights().getDatasets()).hasSameElementsAs(datasets);
        assertThat(updated.getPluginConfiguration().getParameter("processName").getValue()).isEqualTo(
            "useless-processName-2");

        // LIST AGAIN: THERE IS ONE CONFIG!
        List<ProcessPluginConfigurationRightsDTO> pluginConfigsWithUseless2 = Array.ofAll(client.findAll())
                                                                                   .map(EntityModel::getContent)
                                                                                   .asJava();
        pluginConfigsWithUseless2.forEach(pc -> LOGGER.info("Found pc {}: {}",
                                                            pc.getPluginConfiguration().getPluginId(),
                                                            pc));
        assertThat(pluginConfigsWithUseless2).hasSize(initSize + 1);
        assertThat(pluginConfigsWithUseless2).anyMatch(pc -> Try.of(() -> pc.getPluginConfiguration()
                                                                            .getParameter("processName")
                                                                            .getValue()
                                                                            .equals("useless-processName-2"))
                                                                .getOrElse(false));

        // NOW DELETE IT
        ProcessPluginConfigurationRightsDTO toBeDeleted = pluginConfigsWithUseless2.get(0);
        client.delete(UUID.fromString(toBeDeleted.getPluginConfiguration().getBusinessId()));

        // LIST AVAILABLE CONFIGURATIONS: NOTHING ANYMORE...
        List<ProcessPluginConfigurationRightsDTO> pluginConfigsFinal = Array.ofAll(client.findAll())
                                                                            .map(EntityModel::getContent)
                                                                            .asJava();
        pluginConfigsFinal.forEach(pc -> LOGGER.info("Found pc {}: {}", pc.getPluginConfiguration().getPluginId(), pc));
        assertThat(pluginConfigsFinal).hasSize(initSize);

    }

    @Test
    public void test_manage_datasets() {
        runtimeTenantResolver.forceTenant(TENANT_PROJECTB);

        String initDataset = randomDataset();

        // Create 3 configs, all referencing initDataset
        ProcessPluginConfigurationRightsDTO rpc1 = createConfig(list(initDataset),
                                                                "useless1 label",
                                                                "useless-processName-1");
        ProcessPluginConfigurationRightsDTO rpc2 = createConfig(list(initDataset),
                                                                "useless2 label",
                                                                "useless-processName-2");
        ProcessPluginConfigurationRightsDTO rpc3 = createConfig(list(initDataset),
                                                                "useless3 label",
                                                                "useless-processName-3");

        // All processes should reference initDataset
        assertThat(extractIds(client.findProcessesByDataset(initDataset))).containsExactlyInAnyOrder(idFor(rpc1),
                                                                                                     idFor(rpc2),
                                                                                                     idFor(rpc3));

        // Using a new dataset
        String newDataset = randomDataset();

        // Nobody knows the init dataset yet
        assertThat(extractIds(client.findProcessesByDataset(newDataset))).isEmpty();

        Map<String, Collection<ProcessLabelDTO>> map = client.findProcessesByDatasets(list(initDataset,
                                                                                           newDataset).toJavaList());
        Option<Collection<ProcessLabelDTO>> lists = map.get(initDataset);
        Collection<ProcessLabelDTO> processLabelDTOS = lists.get();
        assertThat(extractIds(processLabelDTOS)).containsExactlyInAnyOrder(idFor(rpc1), idFor(rpc2), idFor(rpc3));
        assertThat(map.get(newDataset).get()).isEmpty();

        // Set the dataset to be referenced by rpc 1 and 2
        client.attachDatasetToProcesses(extractIds(rpc1, rpc2), newDataset);
        assertThat(extractIds(client.findProcessesByDataset(newDataset))).containsExactlyInAnyOrder(idFor(rpc1),
                                                                                                    idFor(rpc2));

        Map<String, Collection<ProcessLabelDTO>> processesByDatasetsB = client.findProcessesByDatasets(list(initDataset,
                                                                                                            newDataset).toJavaList());
        assertThat(extractIds(processesByDatasetsB.get(initDataset).get())).containsExactlyInAnyOrder(idFor(rpc1),
                                                                                                      idFor(rpc2),
                                                                                                      idFor(rpc3));
        assertThat(extractIds(processesByDatasetsB.get(newDataset).get())).containsExactlyInAnyOrder(idFor(rpc1),
                                                                                                     idFor(rpc2));

        // Reset the dataset to be referenced by rpc 2 and 3
        client.attachDatasetToProcesses(extractIds(rpc2, rpc3), newDataset);
        assertThat(extractIds(client.findProcessesByDataset(newDataset))).containsExactlyInAnyOrder(idFor(rpc2),
                                                                                                    idFor(rpc3));

        Map<String, Collection<ProcessLabelDTO>> processesByDatasetsC = client.findProcessesByDatasets(list(initDataset,
                                                                                                            newDataset).toJavaList());
        assertThat(extractIds(processesByDatasetsC.get(initDataset).get())).containsExactlyInAnyOrder(idFor(rpc1),
                                                                                                      idFor(rpc2),
                                                                                                      idFor(rpc3));
        assertThat(extractIds(processesByDatasetsC.get(newDataset).get())).containsExactlyInAnyOrder(idFor(rpc2),
                                                                                                     idFor(rpc3));
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
        return datasetAssociatedProcesses.stream().map(ProcessLabelDTO::getProcessBusinessId).collect(Collectors.toList());
    }

    private static List<UUID> extractIds(ProcessPluginConfigurationRightsDTO... dtos) {
        return list(dtos).map(rpc -> rpc.getPluginConfiguration().getBusinessId()).map(UUID::fromString).toJavaList();
    }

    @SafeVarargs
    private static <T> io.vavr.collection.List<T> list(T... ts) {
        return io.vavr.collection.List.of(ts);
    }

    private ProcessPluginConfigurationRightsDTO createConfig(io.vavr.collection.List<String> initDatasets,
                                                             String s,
                                                             String s2) {
        PluginConfiguration useless1Config = new PluginConfiguration(s, UselessProcessPlugin.class.getSimpleName());
        useless1Config.setVersion("1.0.0-SNAPSHOT");
        useless1Config.setPriorityOrder(1);
        useless1Config.setParameters(IPluginParam.set(IPluginParam.build("processName", s2)));
        useless1Config.setBusinessId(null);
        runtimeTenantResolver.forceTenant(TENANT_PROJECTA);
        return client.create(new ProcessPluginConfigurationRightsDTO(useless1Config,
                                                                     new ProcessPluginConfigurationRightsDTO.Rights(
                                                                         "EXPLOIT",
                                                                         initDatasets,
                                                                         false))).getContent();
    }

    @Before
    public void init() {
        client = Feign.builder()
                      .contract(new FeignContractSupplier().get())
                      .decoder(new GsonLoggingDecoder(gson))
                      .encoder(new GsonLoggingEncoder(gson))
                      .logLevel(feign.Logger.Level.FULL)
                      .target(new TokenClientProvider<>(Client.class,
                                                        "http://" + serverAddress + ":" + port,
                                                        feignSecurityManager));
        runtimeTenantResolver.forceTenant(TENANT_PROJECTA);
        FeignSecurityManager.asUser("regards@cnes.fr", DefaultRole.ADMIN.name());
    }

    @RestClient(name = "rs-processing-config", contextId = "rs-processing.rest.plugin-conf.client")
    public interface Client {

        @RequestMapping(method = RequestMethod.GET,
            path = ProcessingConstants.Path.PROCESSPLUGIN_PATH + ProcessingConstants.Path.CONFIG_SUFFIX,
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
        List<EntityModel<ProcessPluginConfigurationRightsDTO>> findAll();

        @RequestMapping(method = RequestMethod.GET,
            path = ProcessingConstants.Path.PROCESSPLUGIN_PATH + ProcessingConstants.Path.METADATA_SUFFIX,
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
        List<PluginMetaData> listAllDetectedMetadata();

        @RequestMapping(method = RequestMethod.GET,
            path = ProcessingConstants.Path.PROCESSPLUGIN_PATH + ProcessingConstants.Path.CONFIG_BID_SUFFIX,
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
        EntityModel<ProcessPluginConfigurationRightsDTO> findByBusinessId(
            @PathVariable(ProcessingConstants.Path.Param.PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId);

        @RequestMapping(method = RequestMethod.POST,
            path = ProcessingConstants.Path.PROCESSPLUGIN_PATH + ProcessingConstants.Path.CONFIG_SUFFIX,
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
        EntityModel<ProcessPluginConfigurationRightsDTO> create(
            @RequestBody ProcessPluginConfigurationRightsDTO rightsDto);

        @RequestMapping(method = RequestMethod.PUT,
            path = ProcessingConstants.Path.PROCESSPLUGIN_PATH + ProcessingConstants.Path.CONFIG_BID_SUFFIX,
            produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE },
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
        EntityModel<ProcessPluginConfigurationRightsDTO> update(
            @PathVariable(ProcessingConstants.Path.Param.PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId,
            @RequestBody ProcessPluginConfigurationRightsDTO rightsDto);

        @RequestMapping(method = RequestMethod.DELETE,
            path = ProcessingConstants.Path.PROCESSPLUGIN_PATH + ProcessingConstants.Path.CONFIG_BID_SUFFIX,
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
        EntityModel<ProcessPluginConfigurationRightsDTO> delete(
            @PathVariable(ProcessingConstants.Path.Param.PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId);

        @RequestMapping(method = RequestMethod.GET,
            path = ProcessingConstants.Path.PROCESSPLUGIN_PATH + ProcessingConstants.Path.LINKDATASET_SUFFIX,
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
        Collection<ProcessLabelDTO> findProcessesByDataset(
            @PathVariable(ProcessingConstants.Path.Param.DATASET_PARAM) String dataset);

        @RequestMapping(method = RequestMethod.POST,
            path = ProcessingConstants.Path.PROCESSPLUGIN_PATH + ProcessingConstants.Path.BY_DATASETS_SUFFIX,
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
        Map<String, Collection<ProcessLabelDTO>> findProcessesByDatasets(@RequestBody List<String> datasets);

        @RequestMapping(method = RequestMethod.PUT,
            path = ProcessingConstants.Path.PROCESSPLUGIN_PATH + ProcessingConstants.Path.LINKDATASET_SUFFIX,
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
        void attachDatasetToProcesses(@RequestBody List<UUID> processBusinessIds,
                                      @PathVariable(ProcessingConstants.Path.Param.DATASET_PARAM) String dataset);

        @RequestMapping(method = RequestMethod.PUT,
            path = ProcessingConstants.Path.PROCESSPLUGIN_PATH + ProcessingConstants.Path.CONFIG_BID_USERROLE_SUFFIX,
            consumes = MediaType.ALL_VALUE)
        void attachRoleToProcess(
            @PathVariable(ProcessingConstants.Path.Param.PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId,
            @RequestParam(ProcessingConstants.Path.Param.USER_ROLE_PARAM) String userRole);
    }

}