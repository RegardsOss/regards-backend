package fr.cnes.regards.modules.processing.controller;

import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.processing.plugins.IProcessDefinition;
import fr.cnes.regards.modules.processing.service.IProcessPluginConfigService;
import io.vavr.collection.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.stream.Collectors;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.METADATA_SUFFIX;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.PROCESSPLUGIN_PATH;

@RestController
@RequestMapping(path = PROCESSPLUGIN_PATH + METADATA_SUFFIX)
public class ProcessPluginMetadataController {

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IProcessPluginConfigService rightsConfigService;

    @Autowired
    public ProcessPluginMetadataController(IRuntimeTenantResolver runtimeTenantResolver,
                                           IProcessPluginConfigService rightsConfigService) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.rightsConfigService = rightsConfigService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "List all detected process plugins", role = DefaultRole.ADMIN)
    public Collection<PluginMetaData> listAllDetectedPlugins() {
        return Stream.ofAll(PluginUtils.getPlugins().values()).filter(md -> md.getInterfaceNames().stream()
                .anyMatch(i -> i.endsWith(IProcessDefinition.class.getName()))).collect(Collectors.toList());
    }

}
