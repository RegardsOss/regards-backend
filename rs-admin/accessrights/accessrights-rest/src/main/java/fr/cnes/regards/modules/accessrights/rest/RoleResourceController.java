/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.List;
import java.util.Set;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.resources.IResourcesService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;

/**
 * Role resource management API
 *
 * @author Marc Sordi
 *
 */
@RestController
@RequestMapping(RoleResourceController.TYPE_MAPPING)
public class RoleResourceController implements IResourceController<ResourcesAccess> {

    /**
     * Controller base mapping
     */
    public static final String TYPE_MAPPING = "/roles/{role_name}/resources";

    /**
     * Single resource mapping
     */
    public static final String SINGLE_RESOURCE_MAPPING = "/{resources_access_id}";

    /**
     * Service handling roles.
     */
    @Autowired
    private IRoleService roleService;

    /**
     * Service handling resources.
     */
    @Autowired
    private IResourcesService resourceService;

    /**
     * Resource service to manage visible hateoas links
     */
    @Autowired
    private IResourceService hateoasService;

    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Get all resource accesses of a role", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<Resource<ResourcesAccess>>> getRoleResources(
            @PathVariable("role_name") final String pRoleName) throws ModuleException {
        final Role role = roleService.retrieveRole(pRoleName);
        final Set<ResourcesAccess> resources = roleService.retrieveRoleResourcesAccesses(role.getId());
        return new ResponseEntity<>(toResources(resources, pRoleName), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Add access to one resource for a role", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<ResourcesAccess>> addRoleResource(@PathVariable("role_name") final String pRoleName,
            @RequestBody @Valid final ResourcesAccess pNewResourcesAccess) throws ModuleException {
        final Role role = roleService.retrieveRole(pRoleName);
        roleService.addResourceAccesses(role.getId(), pNewResourcesAccess);
        return new ResponseEntity<>(toResource(pNewResourcesAccess, pRoleName), HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = SINGLE_RESOURCE_MAPPING)
    @ResourceAccess(description = "Remove one resource access from a role", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> deleteRoleResource(@PathVariable("role_name") final String pRoleName,
            @PathVariable("resources_access_id") final Long pResourcesAccessId) throws ModuleException {
        resourceService.removeRoleResourcesAccess(pRoleName, pResourcesAccessId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public Resource<ResourcesAccess> toResource(ResourcesAccess pElement, Object... pExtras) {

        Assert.notNull(pExtras);
        String roleName = (String) pExtras[0];

        Resource<ResourcesAccess> resource = hateoasService.toResource(pElement);
        hateoasService.addLink(resource, this.getClass(), "getRoleResources", LinkRels.LIST,
                               MethodParamFactory.build(String.class, roleName));
        hateoasService.addLink(resource, this.getClass(), "addRoleResource", LinkRels.CREATE,
                               MethodParamFactory.build(String.class, roleName),
                               MethodParamFactory.build(ResourcesAccess.class));
        hateoasService.addLink(resource, this.getClass(), "deleteRoleResource", LinkRels.DELETE,
                               MethodParamFactory.build(String.class, roleName),
                               MethodParamFactory.build(Long.class, pElement.getId()));
        return resource;
    }

}
