/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.endpoint;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.domain.ResourceMapping;

/**
 *
 * Class DefaultPluginResourceManager
 *
 * Default implementation for Plugin resource endpoints management
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class DefaultPluginResourceManager implements IPluginResourceManager {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DefaultPluginResourceManager.class);

    @Override
    public List<ResourceMapping> manageMethodResource(final String pResourceRootPath,
            final ResourceAccess pResourceAccess, final RequestMapping pRequestMapping) {
        final List<ResourceMapping> mappings = new ArrayList<>();
        LOG.warn("There is no implementation fo plugin endpoints resource management");
        return mappings;
    }

}
