package fr.cnes.regards.modules.emails.client;

import org.springframework.cloud.netflix.feign.FeignClient;

import feign.Headers;
import fr.cnes.regards.modules.emails.signature.IEmailSignature;

/**
 * Feign client exposing the emails module endpoints to other microservices plugged through Eureka.
 *
 * @author CS SI
 */
@FeignClient(value = "rs-admin")
@Headers({ "Accept: application/json", "Content-Type: application/json" })
public interface IEmailClient extends IEmailSignature {

}