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

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.processing.dto.ProcessLabelDTO;
import fr.cnes.regards.modules.processing.service.IProcessPluginConfigService;
import io.vavr.collection.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.*;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.DATASET_PARAM;

/**
 * This class is the controller for manipulating {@link fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration}
 * links to datasets.
 *
 * @author gandrieu
 */
@RestController
@RequestMapping(path = PROCESSPLUGIN_PATH)
public class ProcessPluginDatasetController {

    private final IProcessPluginConfigService rightsConfigService;

    @Autowired
    public ProcessPluginDatasetController(IRuntimeTenantResolver runtimeTenantResolver,
                                          IProcessPluginConfigService rightsConfigService) {
        this.rightsConfigService = rightsConfigService;
    }

    @GetMapping(path = LINKDATASET_SUFFIX)
    @ResourceAccess(description = "Find processes attached to any of the given dataset",
                    role = DefaultRole.REGISTERED_USER)
    public Collection<ProcessLabelDTO> findProcessesByDataset(@PathVariable(DATASET_PARAM) String dataset) {
        return rightsConfigService.getDatasetLinkedProcesses(dataset);
    }

    @PutMapping(path = LINKDATASET_SUFFIX)
    @ResourceAccess(description = "Attach the given dataset to all the given processes", role = DefaultRole.ADMIN)
    public void attachDatasetToProcesses(@RequestBody(required = false) List<UUID> processBusinessIds,
                                         @PathVariable(DATASET_PARAM) String dataset) {
        if (processBusinessIds != null) {
            rightsConfigService.putDatasetLinkedProcesses(processBusinessIds, dataset);
        } else {
            rightsConfigService.putDatasetLinkedProcesses(Lists.newArrayList(), dataset);
        }
    }

    @PostMapping(path = BY_DATASETS_SUFFIX)
    @ResourceAccess(description = "Find processes attached to any of the given datasets",
                    role = DefaultRole.REGISTERED_USER)
    public Map<String, List<ProcessLabelDTO>> findProcessesByDatasets(@RequestBody List<String> datasets) {
        return rightsConfigService.findProcessesByDatasets(datasets)
                                  .getMap()
                                  .mapValues(io.vavr.collection.List::asJava);
    }

}
