package fr.cnes.regards.modules.notification.client;

import org.springframework.cloud.netflix.feign.FeignClient;

import feign.Headers;
import fr.cnes.regards.modules.notification.signature.INotificationSignature;

/**
 * Feign client exposing the notification module endpoints to other microservices plugged through Eureka.
 *
 * @author Xavier-Alexandre Brochard
 */
@FeignClient(value = "rs-admin")
@Headers({ "Accept: application/json", "Content-Type: application/json" })
public interface INotificationClient extends INotificationSignature {

}