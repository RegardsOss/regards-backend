package fr.cnes.regards.modules.processing.service;

import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.processing.controller.ProcessPluginConfigController;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import fr.cnes.regards.modules.processing.plugins.IProcessDefinition;
import fr.cnes.regards.modules.processing.repository.IRightsPluginConfigurationRepository;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class ProcessPluginConfigService implements IProcessPluginConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPluginConfigController.class);

    private final IPluginConfigurationRepository pluginConfigRepo;
    private final IRightsPluginConfigurationRepository rightsPluginConfigRepo;

    public ProcessPluginConfigService(
            IPluginConfigurationRepository pluginConfigRepo,
            IRightsPluginConfigurationRepository rightsPluginConfigRepo
    ) {
        this.pluginConfigRepo = pluginConfigRepo;
        this.rightsPluginConfigRepo = rightsPluginConfigRepo;
    }

    @Override
    public Flux<ProcessPluginConfigurationRightsDTO> findAllRightsPluginConfigs() {
        return Flux.fromIterable(rightsPluginConfigRepo.findAll())
                .flatMap(rights -> {
                    ProcessPluginConfigurationRightsDTO rightsDto = ProcessPluginConfigurationRightsDTO
                            .fromRightsPluginConfiguration(rights);
                    return Mono.just(rightsDto);
                });
    }

    @Override public Mono<ProcessPluginConfigurationRightsDTO> findByBusinessId(UUID processBusinessId) {
        return Mono.fromCallable(() -> {
            RightsPluginConfiguration rights = findEntityByBusinessId(processBusinessId);
            ProcessPluginConfigurationRightsDTO rightsDto = ProcessPluginConfigurationRightsDTO
                    .fromRightsPluginConfiguration(rights);
            return rightsDto;
        });
    }

    @Override public Mono<ProcessPluginConfigurationRightsDTO> update(
            String tenant,
            UUID processBusinessId,
            ProcessPluginConfigurationRightsDTO rightsDto
    ) {
        return Mono.fromCallable(() -> {
            PluginConfiguration updatedPc = pluginConfigRepo.save(rightsDto.getPluginConfiguration());

            RightsPluginConfiguration rights = findEntityByBusinessId(processBusinessId);
            rights.setPluginConfiguration(updatedPc);
            rights.setDatasets(rightsDto.getRights().getDatasets().toJavaList());
            rights.setRole(rightsDto.getRights().getRole());

            RightsPluginConfiguration persistedRights = rightsPluginConfigRepo.save(rights);
            return ProcessPluginConfigurationRightsDTO.fromRightsPluginConfiguration(persistedRights);
        });
    }

    @Override public Mono<ProcessPluginConfigurationRightsDTO> create(
            String tenant,
            ProcessPluginConfigurationRightsDTO rightsDto
    ) {
        return Mono.fromCallable(() -> {
            UUID processBusinessId = UUID.randomUUID();
            // Beware, mutation
            rightsDto.getPluginConfiguration().setBusinessId(processBusinessId.toString());
            return ProcessPluginConfigurationRightsDTO
                    .fromRightsPluginConfiguration(rightsPluginConfigRepo.save(rightsDto.toRightsPluginConfiguration(tenant)));
        });
    }

    @Override public Mono<ProcessPluginConfigurationRightsDTO> delete(UUID processBusinessId) {
        return Mono.fromCallable(() -> {
            RightsPluginConfiguration rights = findEntityByBusinessId(processBusinessId);
            rightsPluginConfigRepo.delete(rights);
            return ProcessPluginConfigurationRightsDTO.fromRightsPluginConfiguration(rights);
         });
    }

    private RightsPluginConfiguration findEntityByBusinessId(UUID processBusinessId) {
        PluginConfiguration pc = Option.of(pluginConfigRepo
                                                   .findCompleteByBusinessId(processBusinessId.toString()))
                .getOrElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plugin with UUID " + processBusinessId + " not found"));
        return rightsPluginConfigRepo.findByPluginConfiguration(pc)
                .getOrElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rights for plugin with UUID " + processBusinessId + " not found"));
    }

    private boolean eligibleClass(PluginConfiguration pc) {
        try {
            String pluginClassName = PluginUtils.getPluginMetadata(pc.getPluginId()).getPluginClassName();
            return Class.forName(pluginClassName).isAssignableFrom(IProcessDefinition.class); }
        catch(ClassNotFoundException e) { return false; }
    }

}
