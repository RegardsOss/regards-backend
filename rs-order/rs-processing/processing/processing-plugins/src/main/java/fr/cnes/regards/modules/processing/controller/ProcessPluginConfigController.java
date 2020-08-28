package fr.cnes.regards.modules.processing.controller;

import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.processing.dto.ProcessRightsDTO;
import fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration;
import fr.cnes.regards.modules.processing.repository.IRightsPluginConfigurationRepository;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.websocket.server.PathParam;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.ProcessingConstants.ContentType.APPLICATION_JSON;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.*;

@RestController
public class ProcessPluginConfigController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPluginConfigController.class);

    private final IPluginConfigurationRepository pluginConfigRepo;
    private final IRightsPluginConfigurationRepository rightsPluginConfigRepo;
    private final IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    public ProcessPluginConfigController(
            IPluginConfigurationRepository pluginConfigRepo,
            IRightsPluginConfigurationRepository rightsPluginConfigRepo,
            IRuntimeTenantResolver runtimeTenantResolver
    ) {
        this.pluginConfigRepo = pluginConfigRepo;
        this.rightsPluginConfigRepo = rightsPluginConfigRepo;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @GetMapping(
            path = PROCESS_PATH + "/{processUuid}/rights",
            produces = APPLICATION_JSON,
            consumes = APPLICATION_JSON
    )
    public Mono<ProcessRightsDTO> getRights(
            @PathParam ("processUUid") UUID processBusinessId
    ) {
        return Mono.fromCallable(() -> {
            String tenant = runtimeTenantResolver.getTenant();
            PluginConfiguration pc = Option.of(pluginConfigRepo
                                                       .findCompleteByBusinessId(processBusinessId.toString()))
                    .getOrElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plugin with UUID " + processBusinessId + " not found"));
            RightsPluginConfiguration rights = rightsPluginConfigRepo.findByPluginConfiguration(pc)
                    .getOrElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rights for plugin with UUID " + processBusinessId + " not found"));
            ProcessRightsDTO rightsDto = ProcessRightsDTO.fromRightsPluginConfiguration(rights);
            return rightsDto;
        });
    }

    @PostMapping(
            path = PROCESS_PATH + "/{processUuid}/rights",
            produces = APPLICATION_JSON,
            consumes = APPLICATION_JSON
    )
    public Mono<ProcessRightsDTO> setRights(
            @PathParam ("processUUid") UUID processBusinessId,
            @RequestBody ProcessRightsDTO rightsDto
    ) {
        return Mono.fromCallable(() -> {
            String tenant = runtimeTenantResolver.getTenant();
            PluginConfiguration pc = Option
                    .of(pluginConfigRepo.findCompleteByBusinessId(processBusinessId.toString()))
                    .getOrElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                                                                                                                                         "Plugin with UUID " + processBusinessId + " not found"));
            rightsPluginConfigRepo.findByPluginConfiguration(pc).peek(rightsPluginConfigRepo::delete);
            ProcessRightsDTO saved = ProcessRightsDTO.fromRightsPluginConfiguration(rightsPluginConfigRepo.save(rightsDto.toRightsPluginConfiguration(tenant, pc)));
            return saved;
        });
    }

    @GetMapping(
            path = PROCESS_CONFIG_METADATA_PATH,
            produces = APPLICATION_JSON
    )
    public Flux<PluginMetaData> listAllDetectedPlugins() {
        return ReactiveSecurityContextHolder.getContext()
            .flatMapMany(ctx -> Flux.fromIterable(PluginUtils.getPlugins().values())
                .doOnError(t -> LOGGER.error(t.getMessage(), t)));
    }

    @GetMapping(
            path = PROCESS_CONFIG_INSTANCES_PATH,
            produces = APPLICATION_JSON
    )
    public Flux<PluginConfiguration> listAllPluginConfigurations() {
        return ReactiveSecurityContextHolder.getContext().flatMapMany(ctx ->
                Flux.fromIterable(pluginConfigRepo.findAll())
                .doOnError(t -> LOGGER.error(t.getMessage(), t)));
    }

    @PostMapping(
            path = PROCESS_CONFIG_INSTANCES_PATH,
            produces = APPLICATION_JSON,
            consumes = APPLICATION_JSON
    )
    public Mono<PluginConfiguration> create(@RequestBody PluginConfiguration config) {
        return Mono.fromCallable(() -> pluginConfigRepo.save(config))
                .doOnError(t -> LOGGER.error(t.getMessage(), t));
    }

    @PutMapping(
            path = PROCESS_CONFIG_INSTANCES_PATH,
            produces = APPLICATION_JSON,
            consumes = APPLICATION_JSON
    )
    public Mono<PluginConfiguration> update(@RequestBody PluginConfiguration config) {
        return Mono.fromCallable(() -> pluginConfigRepo.save(config))
                .doOnError(t -> LOGGER.error(t.getMessage(), t));
    }

    @DeleteMapping(
            path = PROCESS_CONFIG_INSTANCES_PATH + "/{id}",
            produces = APPLICATION_JSON,
            consumes = APPLICATION_JSON
    )
    public Mono<Void> delete(@PathVariable("id") Long id) {
        return Mono.<Void>fromCallable(() -> { pluginConfigRepo.deleteById(id); return null; })
                .doOnError(t -> LOGGER.error(t.getMessage(), t));
    }

    @PostMapping(value = "*")
    @ResponseBody
    public String getFallback(@RequestBody String content) {
        LOGGER.info("Received: {}", content);
        return "Fallback for POST Requests";
    }

}
