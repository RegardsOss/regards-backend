/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.client;

import org.springframework.cloud.netflix.feign.FeignClient;

import feign.Headers;
import fr.cnes.regards.modules.accessrights.signature.IRolesSignature;
import fr.cnes.regards.modules.accessrights.fallback.RolesFallback;

/**
 *
 * Class IRolesClient
 *
 * Feign client for rs-admin Roles controller.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@FeignClient(value = "rs-admin", fallback = RolesFallback.class)
@Headers({ "Accept: application/json", "Content-Type: application/json" })
public interface IRolesClient extends IRolesSignature {

}