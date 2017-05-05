/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.client;

import java.util.List;

import javax.validation.Valid;

import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;

/**
 * Role resource management API client
 *
 * @author Marc Sordi
 *
 */
@RestClient(name = "rs-admin")
@RequestMapping(value = IRoleResourceClient.TYPE_MAPPING, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IRoleResourceClient {

    /**
     * Controller base mapping
     */
    public static final String TYPE_MAPPING = "/roles/{role_name}/resources";

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<Resource<ResourcesAccess>>> getRoleResources(
            @PathVariable("role_name") final String pRoleName);

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Resource<ResourcesAccess>> addRoleResource(@PathVariable("role_name") final String pRoleName,
            @RequestBody @Valid final ResourcesAccess pNewResourcesAccess);

    @RequestMapping(method = RequestMethod.DELETE, value = "/{resources_access_id}")
    public ResponseEntity<Void> deleteRoleResource(@PathVariable("role_name") final String pRoleName,
            @PathVariable("resources_access_id") final Long pResourcesAccessId);

}
