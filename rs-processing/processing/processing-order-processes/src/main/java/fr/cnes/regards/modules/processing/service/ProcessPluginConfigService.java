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
package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.repository.IPBatchRepository;
import fr.cnes.regards.modules.processing.domain.repository.IPExecutionRepository;
import fr.cnes.regards.modules.processing.dto.ProcessLabelDTO;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;
import fr.cnes.regards.modules.processing.dto.ProcessesByDatasetsDTO;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import fr.cnes.regards.modules.processing.event.RightsPluginConfigurationEvent;
import fr.cnes.regards.modules.processing.event.RightsPluginConfigurationEvent.Type;
import fr.cnes.regards.modules.processing.repository.IRightsPluginConfigurationRepository;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * This class is the implementation for {@link IProcessPluginConfigService}.
 *
 * @author gandrieu
 */
@Service
@MultitenantTransactional
public class ProcessPluginConfigService implements IProcessPluginConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPluginConfigService.class);

    private final IPluginService pluginService;

    private final IRightsPluginConfigurationRepository rightsPluginConfigRepo;

    private final IPExecutionRepository executionRepository;
    private final IPBatchRepository batchRepo;

    private final IPublisher publisher;

    public ProcessPluginConfigService(
            IRightsPluginConfigurationRepository rightsPluginConfigRepo,
            IPExecutionRepository executionRepository,
            IPublisher publisher,
            IPluginService pluginService,
            IPBatchRepository batchRepo
    ) {
        this.rightsPluginConfigRepo = rightsPluginConfigRepo;
        this.executionRepository = executionRepository;
        this.publisher = publisher;
        this.pluginService = pluginService;
        this.batchRepo = batchRepo;
    }

    @Override
    public Collection<ProcessPluginConfigurationRightsDTO> findAllRightsPluginConfigs() {
        return rightsPluginConfigRepo.findAll().stream().map(RightsPluginConfiguration::toDto)
                .collect(Collectors.toSet());
    }

    @Override
    public ProcessPluginConfigurationRightsDTO findByBusinessId(UUID processBusinessId) {
        RightsPluginConfiguration rights = findEntityByBusinessId(processBusinessId);
        return RightsPluginConfiguration.toDto(rights);
    }

    @Override
    public ProcessPluginConfigurationRightsDTO update(UUID processBusinessId,
            ProcessPluginConfigurationRightsDTO rightsDto) throws ModuleException {
        PluginConfiguration updatedPc = pluginService.updatePluginConfiguration(rightsDto.getPluginConfiguration());
        RightsPluginConfiguration rights = findEntityByBusinessId(processBusinessId);
        rights.setPluginConfiguration(updatedPc);
        rights.setDatasets(rightsDto.getRights().getDatasets().toJavaArray(String[]::new));
        rights.setRole(rightsDto.getRights().getRole());
        rights.setLinkedToAllDatasets(rightsDto.getRights().isLinkedToAllDatasets());
        RightsPluginConfiguration persistedRights = rightsPluginConfigRepo.save(rights);
        ProcessPluginConfigurationRightsDTO dto = RightsPluginConfiguration.toDto(persistedRights);
        publisher.publish(new RightsPluginConfigurationEvent(RightsPluginConfigurationEvent.Type.UPDATE, rightsDto,
                dto));
        return dto;
    }

    @Override
    public ProcessPluginConfigurationRightsDTO create(ProcessPluginConfigurationRightsDTO rightsDto)
            throws EntityNotFoundException {
        ProcessPluginConfigurationRightsDTO toSave;
        String businessId = rightsDto.getPluginConfiguration().getBusinessId();
        if (businessId == null) {
            LOGGER.debug("rights plugin before adding UUID: {}", rightsDto);
            UUID processBusinessId = UUID.randomUUID();
            rightsDto.getPluginConfiguration().setBusinessId(processBusinessId.toString());
            toSave = rightsDto;
            LOGGER.debug("rights plugin after adding UUID: {}", rightsDto);
        } else {
            PluginConfiguration pc = pluginService.getPluginConfiguration(businessId);
            LOGGER.debug("found plugin with UUID={}: {}", businessId, rightsDto);
            toSave = new ProcessPluginConfigurationRightsDTO(pc, rightsDto.getRights());
        }
        RightsPluginConfiguration rights = RightsPluginConfiguration.fromDto(toSave);
        ProcessPluginConfigurationRightsDTO dto = RightsPluginConfiguration.toDto(rightsPluginConfigRepo.save(rights));
        publisher.publish(new RightsPluginConfigurationEvent(RightsPluginConfigurationEvent.Type.CREATE, null, dto));
        return dto;
    }

    @Override
    public Boolean canDelete(UUID processBusinessId) {
        return executionRepository
                .countByProcessBusinessIdAndStatusIn(processBusinessId, ExecutionStatus.nonFinalStatusList())
                .map(count -> count == 0)
                .block();
    }

    @Override
    public void delete(UUID processBusinessId) throws DeleteAttemptOnUsedProcessException, ModuleException {
        RightsPluginConfiguration rights = findEntityByBusinessId(processBusinessId);
        if (canDelete(processBusinessId)) {
            PluginConfiguration plgConf = rights.getPluginConfiguration();
            rights.setPluginConfiguration(null);
            rightsPluginConfigRepo.delete(rights);
            batchRepo.deleteByProcessBusinessId(processBusinessId).block();
            pluginService.deletePluginConfiguration(plgConf.getBusinessId());
            rights.setPluginConfiguration(plgConf);
            ProcessPluginConfigurationRightsDTO dto = RightsPluginConfiguration.toDto(rights);
            publisher.publish(new RightsPluginConfigurationEvent(Type.DELETE, dto, null));
        } else {
            throw new DeleteAttemptOnUsedProcessException(processBusinessId);
        }
    }

    @Override
    public Collection<ProcessLabelDTO> getDatasetLinkedProcesses(String dataset) {
        java.util.List<RightsPluginConfiguration> fetched = rightsPluginConfigRepo.findByReferencedDataset(dataset);
        return fetched.stream().map(RightsPluginConfiguration::getPluginConfiguration)
                .map(ProcessLabelDTO::fromPluginConfiguration).collect(Collectors.toSet());
    }

    @Override
    public void putDatasetLinkedProcesses(java.util.List<UUID> processBusinessIds, String dataset) {
        rightsPluginConfigRepo.updateAllAddDatasetOnlyForIds(processBusinessIds, dataset);
    }

    @Override
    public ProcessesByDatasetsDTO findProcessesByDatasets(java.util.List<String> datasets) {
        List<RightsPluginConfiguration> rpcs = List.ofAll(rightsPluginConfigRepo.findAll());
        Map<String, List<ProcessLabelDTO>> map = Stream.ofAll(datasets)
                .collect(HashMap.collector(d -> d, d -> rpcs.filter(rpc -> rpc.getDatasets().contains(d))
                        .map(rpc -> ProcessLabelDTO.fromPluginConfiguration(rpc.getPluginConfiguration()))));
        return new ProcessesByDatasetsDTO(map);
    }

    @Override
    public void attachRoleToProcess(UUID processBusinessId, String userRole) {
        rightsPluginConfigRepo.updateRoleToForProcessBusinessId(userRole, processBusinessId);
    }

    private RightsPluginConfiguration findEntityByBusinessId(UUID processBusinessId) {
        return rightsPluginConfigRepo.findByPluginConfigurationBusinessId(processBusinessId.toString())
                .getOrElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Rights for plugin with UUID " + processBusinessId + " not found"));
    }

    @SuppressWarnings("serial")
    public static class DeleteAttemptOnUsedProcessException extends Exception {

        private final UUID processBusinessID;

        public DeleteAttemptOnUsedProcessException(UUID processBusinessID) {
            super(String.format("Can not delete process %s because still in use by executions.", processBusinessID));
            this.processBusinessID = processBusinessID;
        }

        public UUID getProcessBusinessID() {
            return processBusinessID;
        }
    }

}
