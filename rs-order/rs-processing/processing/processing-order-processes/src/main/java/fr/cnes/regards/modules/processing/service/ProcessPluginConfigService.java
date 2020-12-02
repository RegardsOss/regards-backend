/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.repository.IPExecutionRepository;
import fr.cnes.regards.modules.processing.dto.ProcessLabelDTO;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;
import fr.cnes.regards.modules.processing.dto.ProcessesByDatasetsDTO;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import fr.cnes.regards.modules.processing.event.RightsPluginConfigurationEvent;
import fr.cnes.regards.modules.processing.plugins.IProcessDefinition;
import fr.cnes.regards.modules.processing.repository.IRightsPluginConfigurationRepository;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static fr.cnes.regards.modules.processing.event.RightsPluginConfigurationEvent.Type.DELETE;

/**
 * This class is the implementation for {@link IProcessPluginConfigService}.
 *
 * @author gandrieu
 */
@Service
public class ProcessPluginConfigService implements IProcessPluginConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPluginConfigService.class);

    private final IPluginConfigurationRepository pluginConfigRepo;

    private final IRightsPluginConfigurationRepository rightsPluginConfigRepo;

    private final IPExecutionRepository executionRepository;

    private final IPublisher publisher;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public ProcessPluginConfigService(
            IPluginConfigurationRepository pluginConfigRepo,
            IRightsPluginConfigurationRepository rightsPluginConfigRepo,
            IPExecutionRepository executionRepository,
            IPublisher publisher,
            IRuntimeTenantResolver runtimeTenantResolver
    ) {
        this.pluginConfigRepo = pluginConfigRepo;
        this.rightsPluginConfigRepo = rightsPluginConfigRepo;
        this.executionRepository = executionRepository;
        this.publisher = publisher;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public Flux<ProcessPluginConfigurationRightsDTO> findAllRightsPluginConfigs() {
        return Flux.fromIterable(rightsPluginConfigRepo.findAll()).map(RightsPluginConfiguration::toDto);
    }

    @Override
    public Mono<ProcessPluginConfigurationRightsDTO> findByBusinessId(UUID processBusinessId) {
        return Mono.fromCallable(() -> {
            RightsPluginConfiguration rights = findEntityByBusinessId(processBusinessId);
            return RightsPluginConfiguration.toDto(rights);
        });
    }

    @Override
    public Mono<ProcessPluginConfigurationRightsDTO> update(String tenant, UUID processBusinessId,
            ProcessPluginConfigurationRightsDTO rightsDto) {
        return Mono.fromCallable(() -> {
            PluginConfiguration updatedPc = pluginConfigRepo.save(rightsDto.getPluginConfiguration());

            RightsPluginConfiguration rights = findEntityByBusinessId(processBusinessId);
            rights.setPluginConfiguration(updatedPc);
            rights.setDatasets(rightsDto.getRights().getDatasets().toJavaArray(String[]::new));
            rights.setRole(rightsDto.getRights().getRole());
            rights.setLinkedToAllDatasets(rightsDto.getRights().isLinkedToAllDatasets());

            RightsPluginConfiguration persistedRights = rightsPluginConfigRepo.save(rights);
            return RightsPluginConfiguration.toDto(persistedRights);
        }).doOnNext(dto -> publisher.publish(new RightsPluginConfigurationEvent(
                RightsPluginConfigurationEvent.Type.UPDATE, rightsDto, dto)));
    }

    @Override
    public Mono<ProcessPluginConfigurationRightsDTO> create(String tenant,
            ProcessPluginConfigurationRightsDTO rightsDto) {
        return Mono.fromCallable(() -> {
            UUID processBusinessId = UUID.randomUUID();
            // Beware, mutation
            rightsDto.getPluginConfiguration().setBusinessId(processBusinessId.toString());
            RightsPluginConfiguration rights = RightsPluginConfiguration.fromDto(tenant, rightsDto);
            return RightsPluginConfiguration.toDto(rightsPluginConfigRepo.save(rights));
        }).doOnNext(dto -> publisher
                .publish(new RightsPluginConfigurationEvent(RightsPluginConfigurationEvent.Type.CREATE, null, dto)));
    }

    @Override
    public Mono<Boolean> canDelete(UUID processBusinessId) {
        return executionRepository
            .countByProcessBusinessIdAndStatusIn(processBusinessId, ExecutionStatus.nonFinalStatusList())
            .map(count -> count == 0);
    }

    @Override
    public Mono<ProcessPluginConfigurationRightsDTO> delete(UUID processBusinessId, String tenant) {
        return Mono.defer(() -> {
            RightsPluginConfiguration rights = findEntityByBusinessId(processBusinessId);
            return canDelete(processBusinessId).flatMap(canDelete -> {
                if (canDelete) {
                    runtimeTenantResolver.forceTenant(tenant); // We're in some reactor thread somewhere...
                    rightsPluginConfigRepo.delete(rights);
                    runtimeTenantResolver.clearTenant();
                    return Mono.just(RightsPluginConfiguration.toDto(rights));
                }
                else {
                    return Mono.error(new DeleteAttemptOnUsedProcessException(processBusinessId));
                }
            })
            .doOnNext(dto ->
                    publisher.publish(new RightsPluginConfigurationEvent(DELETE, dto, null))
            );
        });
    }

    @Override
    public Flux<ProcessLabelDTO> getDatasetLinkedProcesses(String dataset) {
        java.util.List<RightsPluginConfiguration> fetched = rightsPluginConfigRepo.findByReferencedDataset(dataset);
        return Flux.fromIterable(fetched).map(RightsPluginConfiguration::getPluginConfiguration)
                .map(ProcessLabelDTO::fromPluginConfiguration);
    }

    @MultitenantTransactional
    @Override
    public Mono<Void> putDatasetLinkedProcesses(java.util.List<UUID> processBusinessIds, String dataset) {
        rightsPluginConfigRepo.updateAllAddDatasetOnlyForIds(processBusinessIds, dataset);
        return Mono.empty();
    }

    @Override
    public Mono<ProcessesByDatasetsDTO> findProcessesByDatasets(java.util.List<String> datasets) {
        return Mono.fromCallable(() -> {
            List<RightsPluginConfiguration> rpcs = List.ofAll(rightsPluginConfigRepo.findAll());
            Map<String, List<ProcessLabelDTO>> map = Stream.ofAll(datasets)
                    .collect(HashMap.collector(d -> d, d -> rpcs.filter(rpc -> rpc.getDatasets().contains(d))
                            .map(rpc -> ProcessLabelDTO.fromPluginConfiguration(rpc.getPluginConfiguration()))));
            return new ProcessesByDatasetsDTO(map);
        });
    }

    @Override
    public Mono<Void> attachRoleToProcess(UUID processBusinessId, String userRole) {
        return Mono.fromCallable(() -> {
            rightsPluginConfigRepo.updateRoleToForProcessBusinessId(userRole, processBusinessId);
            return processBusinessId;
        }).then();
    }

    private RightsPluginConfiguration findEntityByBusinessId(UUID processBusinessId) {
        PluginConfiguration pc = Option.of(pluginConfigRepo.findCompleteByBusinessId(processBusinessId.toString()))
                .getOrElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Plugin with UUID " + processBusinessId + " not found"));
        return rightsPluginConfigRepo.findByPluginConfiguration(pc)
                .getOrElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Rights for plugin with UUID " + processBusinessId + " not found"));
    }

    private boolean eligibleClass(PluginConfiguration pc) {
        try {
            String pluginClassName = PluginUtils.getPluginMetadata(pc.getPluginId()).getPluginClassName();
            return Class.forName(pluginClassName).isAssignableFrom(IProcessDefinition.class);
        } catch (ClassNotFoundException|RuntimeException e) {
            LOGGER.debug("Unable to find class matching class name for plugin configuration pc={}", pc, e);
            return false;
        }
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
