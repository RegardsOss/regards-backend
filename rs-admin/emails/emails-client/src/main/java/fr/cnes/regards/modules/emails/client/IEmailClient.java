package fr.cnes.regards.modules.emails.client;

import fr.cnes.regards.client.core.annotation.RestClient;
import fr.cnes.regards.modules.emails.signature.IEmailSignature;

/**
 * Feign client exposing the emails module endpoints to other microservices plugged through Eureka.
 *
 * @author CS SI
 */
@RestClient(name = "rs-admin")
public interface IEmailClient extends IEmailSignature {

}