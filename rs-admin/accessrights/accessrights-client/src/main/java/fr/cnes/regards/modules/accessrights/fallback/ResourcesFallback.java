/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.fallback;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.modules.accessrights.client.IResourcesClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;

/**
 *
 * Class ResourcesFallback
 *
 * Administration microservice Resources client
 *
 * @author sbinda
 * @since 1.0-SNAPSHOT
 */
@Component
public class ResourcesFallback implements IResourcesClient {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ResourcesFallback.class);

    /**
     * Common error message to log
     */
    private static final String fallBackErrorMessage = "RS-ADMIN /users request error. Fallback.";

    @Override
    public ResponseEntity<List<ResourceMapping>> collectResources() {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public ResponseEntity<List<Resource<ResourcesAccess>>> getResourceAccessList() {
        LOG.error(fallBackErrorMessage);
        return null;
    }

}
