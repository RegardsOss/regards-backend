/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.client;

import org.springframework.cloud.netflix.feign.FeignClient;

import feign.Headers;
import fr.cnes.regards.modules.accessrights.fallback.ResourcesFallback;
import fr.cnes.regards.modules.accessrights.signature.IResourcesSignature;

@FeignClient(value = "rs-admin", fallback = ResourcesFallback.class)
@Headers({ "Accept: application/json", "Content-Type: application/json" })
public interface IResourcesClient extends IResourcesSignature {

}
