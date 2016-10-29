/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.client;

import org.springframework.cloud.netflix.feign.FeignClient;

import feign.Headers;
import fr.cnes.regards.modules.plugins.fallback.PluginsFallback;
import fr.cnes.regards.modules.plugins.signature.IPluginsSignature;

/**
 * Feign client allowing access to the module with REST requests.
 * 
 * @author cmertz
 *
 */
@FeignClient(name = "rs-microservice", fallback = PluginsFallback.class)
@Headers({ "Accept: application/json", "Content-Type: application/json" })
public interface IPluginsClient extends IPluginsSignature {

}
