/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.client;

import org.springframework.cloud.netflix.feign.FeignClient;

import feign.Headers;
import fr.cnes.regards.modules.accessRights.fallback.AccountsFallback;
import fr.cnes.regards.modules.accessRights.signature.IAccountsSignature;

/**
 *
 * Class IAccountsClient
 *
 * Feign client for rs-admin Accounts controller.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@FeignClient(name = "rs-admin", fallback = AccountsFallback.class)
@Headers({ "Accept: application/json", "Content-Type: application/json" })
public interface IAccountsClient extends IAccountsSignature {

}
