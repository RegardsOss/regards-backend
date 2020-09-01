package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IProcessPluginConfigService {

    Flux<ProcessPluginConfigurationRightsDTO> findAllRightsPluginConfigs();

    Mono<ProcessPluginConfigurationRightsDTO> findByBusinessId(UUID processBusinessId);

    Mono<ProcessPluginConfigurationRightsDTO> update(String tenant, UUID processBusinessId, ProcessPluginConfigurationRightsDTO rightsDto);

    Mono<ProcessPluginConfigurationRightsDTO> create(String tenant, ProcessPluginConfigurationRightsDTO rightsDto);

    Mono<ProcessPluginConfigurationRightsDTO> delete(UUID processBusinessId);
}
