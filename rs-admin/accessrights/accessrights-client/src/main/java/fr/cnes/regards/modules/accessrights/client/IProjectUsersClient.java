/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.client;

import org.springframework.cloud.netflix.feign.FeignClient;

import feign.Headers;
import fr.cnes.regards.modules.accessrights.signature.IProjectUsersSignature;
import fr.cnes.regards.modules.accessrights.fallback.ProjectUsersFallback;

/**
 *
 * Class IProjectUsersClient
 *
 * Feign client for rs-admin ProjectUsers controller.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@FeignClient(value = "rs-admin", fallback = ProjectUsersFallback.class)
@Headers({ "Accept: application/json", "Content-Type: application/json" })
public interface IProjectUsersClient extends IProjectUsersSignature {

}
