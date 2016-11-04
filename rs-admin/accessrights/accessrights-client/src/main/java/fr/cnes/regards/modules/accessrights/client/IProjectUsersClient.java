/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.client;

import fr.cnes.regards.client.core.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.signature.IProjectUsersSignature;

/**
 *
 * Class IProjectUsersClient
 *
 * Feign client for rs-admin ProjectUsers controller.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RestClient(name = "rs-admin")
public interface IProjectUsersClient extends IProjectUsersSignature {

}
