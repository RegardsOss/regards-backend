package fr.cnes.regards.modules.jobs.client;

import org.springframework.cloud.netflix.feign.FeignClient;

import feign.Headers;
import fr.cnes.regards.modules.jobs.signature.IJobInfoSignature;

/**
 * Feign client exposing the jobs module endpoints to other microservices plugged through Eureka.
 *
 */
@FeignClient("#{'${spring.application.name}'}")
@Headers({ "Accept: application/json", "Content-Type: application/json" })
public interface JobInfoClient extends IJobInfoSignature {

}