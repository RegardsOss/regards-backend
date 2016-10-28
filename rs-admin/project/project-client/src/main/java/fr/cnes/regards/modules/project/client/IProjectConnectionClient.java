/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.client;

import org.springframework.cloud.netflix.feign.FeignClient;

import feign.Headers;
import fr.cnes.regards.modules.project.fallback.ProjectConnectionFallback;
import fr.cnes.regards.modules.project.signature.IProjectConnectionSignature;

/**
 *
 * Class ProjectsClient
 *
 * Feign client allowing access to the module with REST requests.
 *
 * @author sbinda
 * @since 1.0-SNAPSHOT
 */
@FeignClient(name = "rs-admin", fallback = ProjectConnectionFallback.class)
@Headers({ "Accept: application/json", "Content-Type: application/json" })
public interface IProjectConnectionClient extends IProjectConnectionSignature {

}
