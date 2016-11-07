/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.client;

import fr.cnes.regards.client.core.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.signature.IResourcesSignature;

@RestClient(name = "rs-admin")
public interface IResourcesClient extends IResourcesSignature {

}
