/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.client;

import javax.validation.Valid;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;

@RestClient(name = "rs-admin")
@RequestMapping(value = IResourcesClient.TYPE_MAPPING, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IResourcesClient {

    /**
     * Root mapping for requests of this rest controller
     */
    public static final String TYPE_MAPPING = "/resources";

    /**
     * Single resource mapping
     */
    public static final String RESOURCE_MAPPING = "/{resource_id}";

    /**
     * Retrieve resource accesses available to the user
     *
     * @param pPageable
     *            pagination information
     * @param pPagedResourcesAssembler
     *            page assembler
     * @return list of user resource accesses
     */
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<PagedResources<Resource<ResourcesAccess>>> getAllResourceAccesses(
            @RequestParam("page") int pPage, @RequestParam("size") int pSize);

    /**
     *
     * Retrieve the ResourceAccess with given id {@link Long} exists.
     *
     * @param pResourceId
     *            resource id
     * @return {@link ResourcesAccess}
     */
    @RequestMapping(method = RequestMethod.GET, value = RESOURCE_MAPPING)
    public ResponseEntity<Resource<ResourcesAccess>> getResourceAccess(
            @PathVariable("resource_id") final Long pResourceId);

    /**
     *
     * Update given resource access informations
     *
     * @param pResourceId
     *            Resource access identifier
     * @param pResourceAccessToUpdate
     *            Resource access to update
     * @return updated ResourcesAccess
     */
    @RequestMapping(method = RequestMethod.PUT, value = RESOURCE_MAPPING)
    public ResponseEntity<Resource<ResourcesAccess>> updateResourceAccess(
            @PathVariable("resource_id") final Long pResourceId,
            @Valid @RequestBody final ResourcesAccess pResourceAccessToUpdate);

}
