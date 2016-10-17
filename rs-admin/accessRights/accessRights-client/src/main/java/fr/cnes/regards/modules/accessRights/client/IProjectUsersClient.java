/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.client;

import org.springframework.cloud.netflix.feign.FeignClient;

import feign.Headers;
import fr.cnes.regards.modules.accessRights.fallback.ProjectUsersFallback;
import fr.cnes.regards.modules.accessRights.signature.IProjectUsersSignature;

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
