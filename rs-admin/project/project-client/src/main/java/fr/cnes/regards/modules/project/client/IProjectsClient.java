/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.client;

import org.springframework.cloud.netflix.feign.FeignClient;

import feign.Headers;
import fr.cnes.regards.modules.project.fallback.ProjectsFallback;
import fr.cnes.regards.modules.project.signature.IProjectsSignature;

/**
 *
 * Class ProjectsClient
 *
 * Feign client allowing access to the module with REST requests.
 *
 * @author sbinda
 * @since 1.0-SNAPSHOT
 */
@FeignClient(name = "rs-admin", fallback = ProjectsFallback.class)
@Headers({ "Accept: application/json", "Content-Type: application/json" })
public interface IProjectsClient extends IProjectsSignature {

}
