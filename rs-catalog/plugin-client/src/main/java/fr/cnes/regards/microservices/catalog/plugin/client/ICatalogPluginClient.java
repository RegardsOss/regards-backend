/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.catalog.plugin.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.modules.plugins.client.rest.IPluginClient;

/**
 *
 * This client feign exposes access to the plugin rest interface for the microservice catalog.
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@RestClient(name = "rs-catalog")
public interface ICatalogPluginClient extends IPluginClient {

}
