package fr.cnes.regards.framework.security.autoconfigure.endpoint;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;

import fr.cnes.regards.framework.security.utils.endpoint.annotation.ResourceAccess;

/**
 *
 * Class IPluginResourceManager
 *
 * Interface to define a resource manager for plugins endpoints
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public interface IPluginResourceManager {

    /**
     *
     * Identify the endpoint(s) associated to the specified plugin endpoint.
     *
     * @param pResourceRootPath
     *            Endpoint resource root path
     * @param pResourceAccess
     *            ResourceAccess of the endpoint
     * @param pRequestMapping
     *            RequestMapping of the endpoint
     * @return List<ResourceMapping>
     * @since 1.0-SNAPSHOT
     */
    List<ResourceMapping> manageMethodResource(String pResourceRootPath, ResourceAccess pResourceAccess,
            RequestMapping pRequestMapping);

}
