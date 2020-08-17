package fr.cnes.regards.modules.processing.controller;

import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static fr.cnes.regards.modules.processing.ProcessingConstants.ContentType.APPLICATION_JSON;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.PROCESS_CONFIG_INSTANCES_PATH;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.PROCESS_CONFIG_METADATA_PATH;

@RestController
public class ProcessPluginConfigController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPluginConfigController.class);
    
    private final IPluginConfigurationRepository pluginConfigRepo;
    private final IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    public ProcessPluginConfigController(
            IPluginConfigurationRepository pluginConfigRepo,
            IRuntimeTenantResolver runtimeTenantResolver
    ) {
        this.pluginConfigRepo = pluginConfigRepo;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @GetMapping(
            path = PROCESS_CONFIG_METADATA_PATH,
            produces = APPLICATION_JSON
    )
    public Flux<PluginMetaData> listAllDetectedPlugins() {
        setUselessTenant();
        return Flux.fromIterable(PluginUtils.getPlugins().values())
                .doOnError(t -> LOGGER.error(t.getMessage(), t));
    }

    @GetMapping(
            path = PROCESS_CONFIG_INSTANCES_PATH,
            produces = APPLICATION_JSON
    )
    public Flux<PluginConfiguration> listAllPluginConfigurations() {
        setUselessTenant();
        return Flux.fromIterable(pluginConfigRepo.findAll())
                .doOnError(t -> LOGGER.error(t.getMessage(), t));
    }

    @PostMapping(
            path = PROCESS_CONFIG_INSTANCES_PATH,
            produces = APPLICATION_JSON,
            consumes = APPLICATION_JSON
    )
    public Mono<PluginConfiguration> create(@RequestBody PluginConfiguration config) {
        setUselessTenant();
        return Mono.fromCallable(() -> pluginConfigRepo.save(config))
                .doOnError(t -> LOGGER.error(t.getMessage(), t));
    }

    @PutMapping(
            path = PROCESS_CONFIG_INSTANCES_PATH,
            produces = APPLICATION_JSON,
            consumes = APPLICATION_JSON
    )
    public Mono<PluginConfiguration> update(@RequestBody PluginConfiguration config) {
        setUselessTenant();
        return Mono.fromCallable(() -> pluginConfigRepo.save(config))
                .doOnError(t -> LOGGER.error(t.getMessage(), t));
    }

    @DeleteMapping(
            path = PROCESS_CONFIG_INSTANCES_PATH + "/{id}",
            produces = APPLICATION_JSON,
            consumes = APPLICATION_JSON
    )
    public Mono<Void> delete(@PathVariable("id") Long id) {
        setUselessTenant();
        return Mono.<Void>fromCallable(() -> { pluginConfigRepo.deleteById(id); return null; })
                .doOnError(t -> LOGGER.error(t.getMessage(), t));
    }

    @PostMapping(value = "*")
    @ResponseBody
    public String getFallback(@RequestBody String content) {
        LOGGER.info("Received: {}", content);
        return "Fallback for POST Requests";
    }

    private void setUselessTenant() {
        runtimeTenantResolver.forceTenant("default");
    }

}
