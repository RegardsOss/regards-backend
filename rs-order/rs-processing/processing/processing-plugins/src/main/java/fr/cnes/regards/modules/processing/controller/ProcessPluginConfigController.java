package fr.cnes.regards.modules.processing.controller;

import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static fr.cnes.regards.modules.processing.ProcessingConstants.ContentType.APPLICATION_JSON;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.*;

@Controller
public class ProcessPluginConfigController {

    private final IPluginConfigurationRepository pluginConfigRepo;

    @Autowired
    public ProcessPluginConfigController(IPluginConfigurationRepository pluginConfigRepo) {
        this.pluginConfigRepo = pluginConfigRepo;
    }

    @RequestMapping(path = PROCESS_CONFIG_METADATA_PATH, method = RequestMethod.GET, produces = APPLICATION_JSON)
    public Flux<PluginMetaData> listAllDetectedPlugins() {
        return Flux.fromIterable(PluginUtils.getPlugins().values());
    }

    @RequestMapping(path = PROCESS_CONFIG_INSTANCES_PATH, method = RequestMethod.GET, produces = APPLICATION_JSON)
    public Flux<PluginConfiguration> listAllPluginConfigurations() {
        return Flux.fromIterable(pluginConfigRepo.findAll());
    }

    @RequestMapping(path = PROCESS_CONFIG_INSTANCES_PATH, method = RequestMethod.POST, produces = APPLICATION_JSON)
    public Mono<PluginConfiguration> create(@RequestBody PluginConfiguration config) {
        return Mono.fromCallable(() -> pluginConfigRepo.save(config));
    }

    @RequestMapping(path = PROCESS_CONFIG_INSTANCES_PATH, method = RequestMethod.PUT, produces = APPLICATION_JSON)
    public Mono<PluginConfiguration> update(@RequestBody PluginConfiguration config) {
        return Mono.fromCallable(() -> pluginConfigRepo.save(config));
    }

}
