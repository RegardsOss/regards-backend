package fr.cnes.regards.modules.processing.controller;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.processing.dto.ProcessLabelDTO;
import fr.cnes.regards.modules.processing.dto.ProcessesByDatasetsDTO;
import fr.cnes.regards.modules.processing.service.IProcessPluginConfigService;
import io.vavr.collection.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.*;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.DATASET_PARAM;

@RestController
@RequestMapping(path = PROCESSPLUGIN_PATH)
public class ProcessPluginDatasetController {

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IProcessPluginConfigService rightsConfigService;

    @Autowired
    public ProcessPluginDatasetController(IRuntimeTenantResolver runtimeTenantResolver,
                                          IProcessPluginConfigService rightsConfigService) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.rightsConfigService = rightsConfigService;
    }

    @GetMapping(path = LINKDATASET_SUFFIX)
    @ResourceAccess(description = "Find processes attached to any of the given dataset",
            role = DefaultRole.REGISTERED_USER)
    public Collection<ProcessLabelDTO> findProcessesByDataset(@PathVariable(DATASET_PARAM) String dataset) {
        return rightsConfigService.getDatasetLinkedProcesses(dataset).collectList().block();
    }

    @PutMapping(path = LINKDATASET_SUFFIX)
    @ResourceAccess(description = "Attach the given dataset to all the given processes", role = DefaultRole.ADMIN)
    public void attachDatasetToProcesses(@RequestBody List<UUID> processBusinessIds,
                                         @PathVariable(DATASET_PARAM) String dataset) {
        rightsConfigService.putDatasetLinkedProcesses(processBusinessIds, dataset).block();
    }

    @PostMapping(path = BY_DATASETS_SUFFIX)
    @ResourceAccess(description = "Find processes attached to any of the given datasets",
            role = DefaultRole.REGISTERED_USER)
    public Map<String, List<ProcessLabelDTO>> findProcessesByDatasets(@RequestBody List<String> datasets) {
        return rightsConfigService.findProcessesByDatasets(datasets).map(ProcessesByDatasetsDTO::getMap).block()
                .mapValues(io.vavr.collection.List::asJava);
    }

}
