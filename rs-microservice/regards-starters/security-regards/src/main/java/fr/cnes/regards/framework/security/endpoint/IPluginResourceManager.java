package fr.cnes.regards.framework.security.endpoint;

import fr.cnes.regards.framework.security.domain.ResourceMapping;

import java.util.List;

/**
 * Class IPluginResourceManager
 * <p>
 * Interface to define a resource manager for plugins endpoints
 *
 * @author CS
 */
public interface IPluginResourceManager {

    List<ResourceMapping> manageMethodResource(ResourceMapping pResourceMapping);

}
