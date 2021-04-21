package fr.cnes.regards.framework.security.endpoint;

import java.util.List;

import fr.cnes.regards.framework.security.domain.ResourceMapping;

/**
 * Class IPluginResourceManager
 *
 * Interface to define a resource manager for plugins endpoints
 * @author CS
 */
public interface IPluginResourceManager {

    List<ResourceMapping> manageMethodResource(ResourceMapping pResourceMapping);

}
