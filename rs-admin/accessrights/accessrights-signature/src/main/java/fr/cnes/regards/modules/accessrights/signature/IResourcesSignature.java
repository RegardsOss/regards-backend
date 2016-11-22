/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.signature;

import java.util.List;

import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;

/**
 * Define the common interface of REST clients for resources.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RequestMapping(value = "/resources", consumes = MediaType.APPLICATION_JSON_VALUE)
public interface IResourcesSignature {

    /**
     *
     * Retrieve the ResourceAccess list of all microservices
     *
     * @return List<ResourceAccess>
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    ResponseEntity<List<Resource<ResourcesAccess>>> retrieveResourcesAccesses();

    /**
     *
     * Update given resource access informations
     *
     * @param pResourceId
     *            Resource access identifier
     * @param pResourceAccessToUpdate
     *            Resource access to update
     * @return updated ResourcesAccess
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(value = "/{resource_id}", method = RequestMethod.PUT)
    @ResponseBody
    ResponseEntity<Resource<ResourcesAccess>> updateResourceAccess(@PathVariable("resource_id") final Long pResourceId,
            final ResourcesAccess pResourceAccessToUpdate);

    /**
     *
     * Register given resources for the given microservice.
     *
     * @return List<ResourceAccess>
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(value = "/register/{microservicename}", method = RequestMethod.POST)
    @ResponseBody
    ResponseEntity<List<Resource<ResourcesAccess>>> registerMicroserviceEndpoints(
            @PathVariable("microservicename") final String pMicroserviceName,
            @RequestBody List<ResourceMapping> pResourcesToRegister);

}
