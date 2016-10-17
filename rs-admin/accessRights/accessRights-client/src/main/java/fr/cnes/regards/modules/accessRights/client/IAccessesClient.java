/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.client;

import org.springframework.cloud.netflix.feign.FeignClient;

import feign.Headers;
import fr.cnes.regards.modules.accessRights.fallback.AccessesFallback;
import fr.cnes.regards.modules.accessRights.signature.IAccessesSignature;

/**
 *
 * Class IAccessesClient
 *
 * Feign client for rs-admin Accesses Rest controller
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@FeignClient(value = "rs-admin", fallback = AccessesFallback.class)
@Headers({ "Accept: application/json", "Content-Type: application/json" })
public interface IAccessesClient extends IAccessesSignature {

}