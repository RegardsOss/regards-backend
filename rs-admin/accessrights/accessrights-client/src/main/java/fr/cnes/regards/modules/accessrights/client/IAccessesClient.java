/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.client;

import fr.cnes.regards.client.core.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.fallback.AccessesFallback;
import fr.cnes.regards.modules.accessrights.signature.IAccessesSignature;

/**
 *
 * Class IAccessesClient
 *
 * Feign client for rs-admin Accesses Rest controller
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RestClient(name = "rs-admin", fallback = AccessesFallback.class)
public interface IAccessesClient extends IAccessesSignature {

}