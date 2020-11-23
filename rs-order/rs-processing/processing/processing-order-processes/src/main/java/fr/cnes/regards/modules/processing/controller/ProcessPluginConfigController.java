package fr.cnes.regards.modules.processing.controller;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.BY_DATASETS_SUFFIX;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.CONFIG_BID_SUFFIX;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.CONFIG_BID_USERROLE_SUFFIX;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.CONFIG_SUFFIX;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.LINKDATASET_SUFFIX;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.METADATA_SUFFIX;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.PROCESSPLUGIN_PATH;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.DATASET_PARAM;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.PROCESS_BUSINESS_ID_PARAM;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.USER_ROLE_PARAM;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.processing.dto.ProcessLabelDTO;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;
import fr.cnes.regards.modules.processing.dto.ProcessesByDatasetsDTO;
import fr.cnes.regards.modules.processing.plugins.IProcessDefinition;
import fr.cnes.regards.modules.processing.service.IProcessPluginConfigService;
import io.vavr.collection.Map;
import io.vavr.collection.Stream;

// TODO: void return types, exception management, empty mono management

@RestController
@RequestMapping(path = PROCESSPLUGIN_PATH)
public class ProcessPluginConfigController {

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IProcessPluginConfigService rightsConfigService;

    @Autowired
    public ProcessPluginConfigController(IRuntimeTenantResolver runtimeTenantResolver,
            IProcessPluginConfigService rightsConfigService) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.rightsConfigService = rightsConfigService;
    }

    @GetMapping(path = CONFIG_SUFFIX)
    @ResourceAccess(description = "Find all registered configured processes", role = DefaultRole.REGISTERED_USER)
    public Collection<ProcessPluginConfigurationRightsDTO> findAll() {
        return rightsConfigService.findAllRightsPluginConfigs().collectList().block();
    }

    @GetMapping(path = CONFIG_BID_SUFFIX)
    @ResourceAccess(description = "Find a configured process by its business uuid", role = DefaultRole.REGISTERED_USER)
    public ProcessPluginConfigurationRightsDTO findByBusinessId(
            @PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId) {
        return rightsConfigService.findByBusinessId(processBusinessId).block();
    }

    @PostMapping(path = CONFIG_SUFFIX,
            consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
    @ResourceAccess(description = "Create a process configuration from a plugin", role = DefaultRole.ADMIN)
    public ProcessPluginConfigurationRightsDTO create(@RequestBody ProcessPluginConfigurationRightsDTO rightsDto) {
        String tenant = runtimeTenantResolver.getTenant();
        return rightsConfigService.create(tenant, rightsDto).block();
    }

    @PutMapping(path = CONFIG_BID_SUFFIX)
    @ResourceAccess(description = "Update the given process with the given rights configuration",
            role = DefaultRole.ADMIN)
    public ProcessPluginConfigurationRightsDTO update(@PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId,
            @RequestBody ProcessPluginConfigurationRightsDTO rightsDto) {
        String tenant = runtimeTenantResolver.getTenant();
        return rightsConfigService.update(tenant, processBusinessId, rightsDto).block();
    }

    @DeleteMapping(path = CONFIG_BID_SUFFIX)
    @ResourceAccess(description = "Delete the given process", role = DefaultRole.ADMIN)
    public ProcessPluginConfigurationRightsDTO delete(@PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId) {
        return rightsConfigService.delete(processBusinessId).block();
    }

    @GetMapping(path = METADATA_SUFFIX, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "List all detected process plugins", role = DefaultRole.ADMIN)
    public Collection<PluginMetaData> listAllDetectedPlugins() {
        return Stream.ofAll(PluginUtils.getPlugins().values()).filter(md -> md.getInterfaceNames().stream()
                .anyMatch(i -> i.endsWith(IProcessDefinition.class.getName()))).collect(Collectors.toList());
    }

    @GetMapping(path = LINKDATASET_SUFFIX)
    @ResourceAccess(description = "Find processes attached to any of the given dataset",
            role = DefaultRole.REGISTERED_USER)
    public Collection<ProcessLabelDTO> findProcessesByDataset(@PathVariable(DATASET_PARAM) String dataset) {
        return rightsConfigService.getDatasetLinkedProcesses(dataset).collectList().block();
    }

    @PostMapping(path = BY_DATASETS_SUFFIX)
    @ResourceAccess(description = "Find processes attached to any of the given datasets",
            role = DefaultRole.REGISTERED_USER)
    public Map<String, List<ProcessLabelDTO>> findProcessesByDatasets(@RequestBody List<String> datasets) {
        return rightsConfigService.findProcessesByDatasets(datasets).map(ProcessesByDatasetsDTO::getMap).block()
                .mapValues(io.vavr.collection.List::asJava);
    }

    @PutMapping(path = LINKDATASET_SUFFIX)
    @ResourceAccess(description = "Attach the given dataset to all the given processes", role = DefaultRole.ADMIN)
    public void attachDatasetToProcesses(@RequestBody List<UUID> processBusinessIds,
            @PathVariable(DATASET_PARAM) String dataset) {
        rightsConfigService.putDatasetLinkedProcesses(processBusinessIds, dataset).block();
    }

    @PutMapping(path = CONFIG_BID_USERROLE_SUFFIX)
    @ResourceAccess(description = "Attache the given role to the given process", role = DefaultRole.ADMIN)
    public void attachRoleToProcess(@PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId,
            @RequestParam(USER_ROLE_PARAM) String userRole) {
        rightsConfigService.attachRoleToProcess(processBusinessId, userRole).block();
    }

}
