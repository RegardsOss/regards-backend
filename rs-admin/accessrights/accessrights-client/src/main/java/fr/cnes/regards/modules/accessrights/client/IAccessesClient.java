/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.client;

import org.springframework.cloud.netflix.feign.FeignClient;

import feign.Headers;
import fr.cnes.regards.modules.accessrights.signature.IAccessesSignature;
import fr.cnes.regards.modules.accessrights.fallback.AccessesFallback;

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