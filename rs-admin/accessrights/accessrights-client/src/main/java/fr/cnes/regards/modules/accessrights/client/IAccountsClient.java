/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.client;

import org.springframework.cloud.netflix.feign.FeignClient;

import feign.Headers;
import fr.cnes.regards.modules.accessrights.signature.IAccountsSignature;
import fr.cnes.regards.modules.accessrights.fallback.AccountsFallback;

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
