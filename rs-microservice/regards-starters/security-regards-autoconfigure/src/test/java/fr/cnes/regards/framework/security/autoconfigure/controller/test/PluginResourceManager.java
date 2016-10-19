/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure.controller.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import fr.cnes.regards.framework.security.autoconfigure.endpoint.IPluginResourceManager;
import fr.cnes.regards.framework.security.autoconfigure.endpoint.ResourceMapping;
import fr.cnes.regards.framework.security.utils.endpoint.annotation.ResourceAccess;

/**
 *
 * Class PluginResourceManager
 *
 * Plugin resources manager for tests
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Component
public class PluginResourceManager implements IPluginResourceManager {

    @Override
    public List<ResourceMapping> manageMethodResource(final String pResourceRootPath,
            final ResourceAccess pResourceAccess, final RequestMapping pRequestMapping) {
        final List<ResourceMapping> mappings = new ArrayList<>();

        final String path = pRequestMapping.value()[0];
        mappings.add(new ResourceMapping(pResourceAccess, Optional.ofNullable(pResourceRootPath + path + "/plugin_1"),
                pRequestMapping.method()[0]));
        mappings.add(new ResourceMapping(pResourceAccess, Optional.ofNullable(pResourceRootPath + path + "/plugin_2"),
                pRequestMapping.method()[0]));

        return mappings;
    }

}
