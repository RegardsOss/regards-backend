package fr.cnes.regards.modules.processing.controller;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.processing.dto.ProcessLabelDTO;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;
import fr.cnes.regards.modules.processing.dto.ProcessesByDatasetsDTO;
import fr.cnes.regards.modules.processing.service.IProcessPluginConfigService;
import io.vavr.collection.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.*;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.*;

// TODO: void return types, exception management, empty mono management

@RestController
@RequestMapping(
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE }
)
public class ProcessPluginConfigController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPluginConfigController.class);

    private final IRuntimeTenantResolver runtimeTenantResolver;
    private final IAuthenticationResolver authResolver;
    private final IProcessPluginConfigService rightsConfigService;

    @Autowired
    public ProcessPluginConfigController(
            IRuntimeTenantResolver runtimeTenantResolver,
            IAuthenticationResolver authResolver,
            IProcessPluginConfigService rightsConfigService
    ) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.authResolver = authResolver;
        this.rightsConfigService = rightsConfigService;
    }

    @GetMapping(path = PROCESS_CONFIG_PATH, consumes = MediaType.ALL_VALUE)
    public Collection<ProcessPluginConfigurationRightsDTO> findAll() {
        return rightsConfigService.findAllRightsPluginConfigs().collectList().block();
    }


    @GetMapping(path = PROCESS_CONFIG_BID_PATH, consumes = MediaType.ALL_VALUE)
    public ProcessPluginConfigurationRightsDTO findByBusinessId(
            @PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId
    ) {
        return rightsConfigService.findByBusinessId(processBusinessId).block();
    }

    @PostMapping(path = PROCESS_CONFIG_PATH)
    public ProcessPluginConfigurationRightsDTO create(
            @RequestBody ProcessPluginConfigurationRightsDTO rightsDto
    ) {
        String tenant = runtimeTenantResolver.getTenant();
        return rightsConfigService.create(tenant, rightsDto).block();
    }

    @PutMapping(path = PROCESS_CONFIG_BID_PATH)
    public ProcessPluginConfigurationRightsDTO update(
            @PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId,
            @RequestBody ProcessPluginConfigurationRightsDTO rightsDto
    ) {
        String tenant = runtimeTenantResolver.getTenant();
        return rightsConfigService.update(tenant, processBusinessId, rightsDto).block();
    }

    @DeleteMapping(path = PROCESS_CONFIG_BID_PATH, consumes = MediaType.ALL_VALUE)
    public ProcessPluginConfigurationRightsDTO save(
            @PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId
    ) {
        return rightsConfigService.delete(processBusinessId).block();
    }

    @GetMapping(path = PROCESS_METADATA_PATH, consumes = MediaType.ALL_VALUE)
    public Collection<PluginMetaData> listAllDetectedPlugins() {
        return PluginUtils.getPlugins().values();
    }

    @GetMapping(path = PROCESS_LINKDATASET_PATH)
    public Collection<ProcessLabelDTO> findProcessesByDataset(
            @PathVariable(DATASET_PARAM) String dataset
    ) {
        return rightsConfigService.getDatasetLinkedProcesses(dataset).collectList().block();
    }

    @PostMapping(path = PROCESS_BY_DATASETS_PATH)
    public Map<String, List<ProcessLabelDTO>> findProcessesByDatasets(
            @RequestBody List<String> datasets
    ) {
        return rightsConfigService.findProcessesByDatasets(datasets).map(ProcessesByDatasetsDTO::getMap)
                .block()
                .mapValues(io.vavr.collection.List::asJava);
    }

    @PutMapping(path = PROCESS_LINKDATASET_PATH)
    public void attachDatasetToProcesses (
            @RequestBody List<UUID> processBusinessIds,
            @PathVariable(DATASET_PARAM) String dataset
    ) {
        rightsConfigService.putDatasetLinkedProcesses(processBusinessIds, dataset).block();
    }

    @PutMapping(path = PROCESS_CONFIG_BID_USERROLE_PATH, consumes = MediaType.ALL_VALUE)
    public void attachRoleToProcess (
            @PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId,
            @RequestParam(USER_ROLE_PARAM) String userRole
    ) {
        rightsConfigService.attachRoleToProcess(processBusinessId, userRole).block();
    }

}
