package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.dto.ProcessLabelDTO;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;
import fr.cnes.regards.modules.processing.dto.ProcessesByDatasetsDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface IProcessPluginConfigService {

    Flux<ProcessPluginConfigurationRightsDTO> findAllRightsPluginConfigs();

    Mono<ProcessPluginConfigurationRightsDTO> findByBusinessId(UUID processBusinessId);

    Mono<ProcessPluginConfigurationRightsDTO> update(String tenant, UUID processBusinessId, ProcessPluginConfigurationRightsDTO rightsDto);

    Mono<ProcessPluginConfigurationRightsDTO> create(String tenant, ProcessPluginConfigurationRightsDTO rightsDto);

    Mono<Boolean> canDelete(UUID processBusinessId);

    Mono<ProcessPluginConfigurationRightsDTO> delete(UUID processBusinessId, String tenant);

    Mono<Void> putDatasetLinkedProcesses(List<UUID> processBusinessIds, String dataset);

    Flux<ProcessLabelDTO> getDatasetLinkedProcesses(String dataset);

    Mono<ProcessesByDatasetsDTO> findProcessesByDatasets(List<String> datasets);

    Mono<Void> attachRoleToProcess(UUID processBusinessId, String userRole);
}
