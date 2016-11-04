/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.client;

import fr.cnes.regards.client.core.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.signature.IAccountsSignature;

/**
 *
 * Class IAccountsClient
 *
 * Feign client for rs-admin Accounts controller.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RestClient(name = "rs-admin")
public interface IAccountsClient extends IAccountsSignature {

}
