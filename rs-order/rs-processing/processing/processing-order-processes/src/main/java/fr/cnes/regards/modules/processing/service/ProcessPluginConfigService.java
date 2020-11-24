package fr.cnes.regards.modules.processing.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.processing.controller.ProcessPluginConfigController;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProcessPluginConfigService implements IProcessPluginConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPluginConfigController.class);

    private final IPluginConfigurationRepository pluginConfigRepo;

    private final IRightsPluginConfigurationRepository rightsPluginConfigRepo;

    private final IPExecutionRepository executionRepository;

    private final IPublisher publisher;

    public ProcessPluginConfigService(IPluginConfigurationRepository pluginConfigRepo,
            IRightsPluginConfigurationRepository rightsPluginConfigRepo, IPExecutionRepository executionRepository,
            IPublisher publisher) {
        this.pluginConfigRepo = pluginConfigRepo;
        this.rightsPluginConfigRepo = rightsPluginConfigRepo;
        this.executionRepository = executionRepository;
        this.publisher = publisher;
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
    public Mono<ProcessPluginConfigurationRightsDTO> delete(UUID processBusinessId) {
        return Mono.fromCallable(() -> {
            RightsPluginConfiguration rights = findEntityByBusinessId(processBusinessId);
            executionRepository.findByProcessBusinessIdAndStatusIn(processBusinessId,
                                                                   ExecutionStatus.nonFinalStatusList());
            rightsPluginConfigRepo.delete(rights);
            return RightsPluginConfiguration.toDto(rights);
        }).doOnNext(dto -> publisher
                .publish(new RightsPluginConfigurationEvent(RightsPluginConfigurationEvent.Type.DELETE, dto, null)));
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
        return null; // TODO
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
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
