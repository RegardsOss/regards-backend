/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.signature;

import java.util.List;

import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;

/**
 * Define the common interface of REST clients for resources.
 *
 * @author CS SI
 */
@RequestMapping("/resources")
public interface IResourcesSignature {

    /**
     *
     * Collect all the resources from each microservice connected.
     *
     * @return List<ResourceMapping>
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(value = "collect", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<List<ResourceMapping>> collectResources();

    /**
     *
     * Retrieve the ResourceAccess list
     *
     * @return List<ResourceAccess>
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<List<Resource<ResourcesAccess>>> getResourceAccessList();

}
