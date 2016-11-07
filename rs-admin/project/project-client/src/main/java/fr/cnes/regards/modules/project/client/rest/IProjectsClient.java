/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.client.rest;

import fr.cnes.regards.client.core.annotation.RestClient;
import fr.cnes.regards.modules.project.client.rest.fallback.ProjectsFallback;
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
@RestClient(name = "rs-admin", fallback = ProjectsFallback.class)
public interface IProjectsClient extends IProjectsSignature {

}
