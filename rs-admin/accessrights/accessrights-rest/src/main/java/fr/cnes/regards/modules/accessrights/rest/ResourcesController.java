/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.resources.IResourcesService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;

/**
 *
 * Class ResourcesController
 *
 * Rest controller to access ResourcesAccess entities. ResourceAccess are the security configuration to allow access for
 * given roles to microservices endpoints. This configuration is made for each project of the regards instance.
 *
 * @author Sébastien Binda
 * @since 1.0-SNASHOT
 */
@RestController
@ModuleInfo(name = "accessrights", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping(value = ResourcesController.REQUEST_MAPPING_ROOT)
public class ResourcesController implements IResourceController<ResourcesAccess> {

    /**
     * Root mapping for requests of this rest controller
     */
    public static final String REQUEST_MAPPING_ROOT = "/resources";

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ResourcesController.class);

    /**
     * Business service
     */
    private final IResourcesService service;

    /**
     * Service handling project users
     */
    @Autowired
    private IProjectUserService projectUserService;

    /**
     * Service handling roles.
     */
    @Autowired
    private IRoleService roleService;

    /**
     * Resource service to manage visibles hateoas links
     */
    private final IResourceService hateoasService;

    public ResourcesController(final IResourcesService pService, final IResourceService pHateoasService) {
        super();
        service = pService;
        hateoasService = pHateoasService;
    }

    /**
     *
     * Retrieve the resource accesses available to the user
     *
     * @param pPageable
     *            pagination informations
     *
     * @return {@link Page} of {@link ResourceAccess}
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve accessible resource accesses of the user among the system",
            role = DefaultRole.PUBLIC)
    @ResponseBody
    public ResponseEntity<PagedResources<Resource<ResourcesAccess>>> retrieveResourcesAccesses(final Pageable pPageable,
            final PagedResourcesAssembler<ResourcesAccess> pPagedResourcesAssembler) {
        return new ResponseEntity<>(toPagedResources(service.retrieveRessources(pPageable), pPagedResourcesAssembler),
                HttpStatus.OK);
    }

    /**
     *
     * Retrieve the resource accesses available to the user of the given microservice
     *
     * @param pPageable
     *            pagination informations
     *
     * @return {@link Page} of {@link ResourceAccess}
     * @throws EntityNotFoundException
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(value = "/microservices/{microservice}", method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve accessible resource accesses of the user among the given microservice",
            role = DefaultRole.PUBLIC)
    @ResponseBody
    public ResponseEntity<PagedResources<Resource<ResourcesAccess>>> retrieveResourcesAccesses(
            @PathVariable("microservice") final String pMicroserviceName, final Pageable pPageable,
            final PagedResourcesAssembler<ResourcesAccess> pPagedResourcesAssembler) throws EntityNotFoundException {
        return new ResponseEntity<>(
                toPagedResources(service.retrieveMicroserviceRessources(pMicroserviceName, pPageable),
                                 pPagedResourcesAssembler),
                HttpStatus.OK);
    }

    /**
     *
     * Retrieve the ResourceAccess with given id {@link Long} exists.
     *
     * @return List of {@link ResourcesAccess}
     * @throws EntityNotFoundException
     *             Exception if resource with given id does not exists
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(value = "/{resource_id}", method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve all resource accesses of the REGARDS system", role = DefaultRole.PUBLIC)
    @ResponseBody
    public ResponseEntity<Resource<ResourcesAccess>> retrieveResourceAccesses(
            @PathVariable("resource_id") final Long pResourceId) throws EntityNotFoundException {
        return new ResponseEntity<>(toResource(service.retrieveRessource(pResourceId)), HttpStatus.OK);
    }

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
    @ResourceAccess(description = "Update access to a given resource", role = DefaultRole.ADMIN)
    @ResponseBody
    public ResponseEntity<Resource<ResourcesAccess>> updateResourceAccess(
            @PathVariable("resource_id") final Long pResourceId,
            @Valid @RequestBody final ResourcesAccess pResourceAccessToUpdate) throws ModuleException {
        if ((pResourceAccessToUpdate.getId() == null) || !pResourceAccessToUpdate.getId().equals(pResourceId)) {
            throw new EntityInvalidException(
                    String.format("Resource to update with id %d do not match the required resource id %d",
                                  pResourceAccessToUpdate.getId(), pResourceId));
        }
        return new ResponseEntity<>(toResource(service.updateResource(pResourceAccessToUpdate)), HttpStatus.OK);
    }

    /**
     * Retrieve the {@link List} of {@link ResourcesAccess} for the {@link Account} of passed <code>id</code>.
     *
     * @param pUserLogin
     *            The {@link Account}'s <code>id</code>
     * @param pBorrowedRoleName
     *            The borrowed {@link Role} <code>name</code> if the user is connected with a borrowed role. Optional.
     * @return the {@link List} list of resources access
     * @throws EntityException
     *             <br>
     *             {@link EntityOperationForbiddenException} Thrown when the passed {@link Role} is not hierarchically
     *             inferior to the true {@link ProjectUser}'s <code>role</code>.<br>
     *             {@link EntityNotFoundException} Thrown when no {@link ProjectUser} with passed <code>id</code> could
     *             be found<br>
     */
    @ResponseBody
    @RequestMapping(value = "/users/{user_email}", method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the list of specific access rights and the role of the project user",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<Resource<ResourcesAccess>>> retrieveProjectUserResources(
            @PathVariable("user_email") final String pUserLogin,
            @RequestParam(value = "borrowedRoleName", required = false) final String pBorrowedRoleName)
            throws EntityException {
        final List<ResourcesAccess> permissions = projectUserService.retrieveProjectUserAccessRights(pUserLogin,
                                                                                                     pBorrowedRoleName);
        final List<Resource<ResourcesAccess>> result = new ArrayList<>();
        for (final ResourcesAccess item : permissions) {
            result.add(new Resource<>(item));
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * Update the the {@link List} of <code>permissions</code>.
     *
     * @param pLogin
     *            The {@link ProjectUser}'s <code>login</code>
     * @param pUpdatedUserAccessRights
     *            The {@link List} of {@link ResourcesAccess} to set
     * @return void
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @ResponseBody
    @RequestMapping(value = "/users/{user_email}", method = RequestMethod.PUT)
    @ResourceAccess(description = "update the list of specific user access rights", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> updateProjectUserResources(@PathVariable("user_email") final String pLogin,
            @Valid @RequestBody final List<ResourcesAccess> pUpdatedUserAccessRights) throws EntityNotFoundException {
        projectUserService.updateUserAccessRights(pLogin, pUpdatedUserAccessRights);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Clear the {@link List} of {@link ResourcesAccess} of the {@link ProjectUser} with passed <code>login</code>.
     *
     * @param pUserLogin
     *            The {@link ProjectUser} <code>login</code>
     * @return void
     * @throws EntityNotFoundException
     *             Thrown when no {@link ProjectUser} with passed <code>id</code> could be found
     */
    @ResponseBody
    @RequestMapping(value = "/users/{user_email}", method = RequestMethod.DELETE)
    @ResourceAccess(description = "remove all the specific access rights", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> removeProjectUserResources(@PathVariable("user_email") final String pUserLogin)
            throws EntityNotFoundException {
        projectUserService.removeUserAccessRights(pUserLogin);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Define the endpoint for returning the {@link List} of {@link ResourcesAccess} on the {@link Role} of passed
     * <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @return The {@link List} of permissions as {@link ResourcesAccess} wrapped in an {@link ResponseEntity}
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    @ResponseBody
    @RequestMapping(value = "/roles/{role_id}", method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve the list of permissions of the role with role_id",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<Resource<ResourcesAccess>>> retrieveRoleResourcesAccessList(
            @PathVariable("role_id") final Long pRoleId) throws EntityNotFoundException {
        final Set<ResourcesAccess> resources = roleService.retrieveRoleResourcesAccesses(pRoleId);
        return new ResponseEntity<>(toResources(resources), HttpStatus.OK);
    }

    /**
     * Define the endpoint for setting the passed {@link List} of {@link ResourcesAccess} onto the {@link role} of
     * passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role}'s <code>id</code>
     * @param pResourcesAccessList
     *            The {@link List} of {@link ResourcesAccess} to set
     * @return {@link Void} wrapped in an {@link ResponseEntity}
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     * @throws EntityOperationForbiddenException
     */
    @ResponseBody
    @RequestMapping(value = "/roles/{role_id}", method = RequestMethod.PUT)
    @ResourceAccess(description = "Totally update the list of permissions of the role with role_id",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> updateRoleResourcesAccess(@PathVariable("role_id") final Long pRoleId,
            @Valid @RequestBody final Set<ResourcesAccess> pResourcesAccessList)
            throws EntityNotFoundException, EntityOperationForbiddenException {
        roleService.updateRoleResourcesAccess(pRoleId, pResourcesAccessList);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Define the endpoint for clearing the {@link List} of {@link ResourcesAccess} of the {@link Role} with passed
     * <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role} <code>id</code>
     * @return {@link Void} wrapped in an {@link ResponseEntity}
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    @ResponseBody
    @RequestMapping(value = "/roles/{role_id}", method = RequestMethod.DELETE)
    @ResourceAccess(description = "Clear the list of permissions of the given role", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> clearRoleResourcesAccess(@PathVariable("role_id") final Long pRoleId)
            throws EntityNotFoundException {
        roleService.clearRoleResourcesAccess(pRoleId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Define the endpoint allowing the addition of a given resource access to the role of given role_id
     *
     * @param pRoleId
     *            The {@link Role} <code>id</code>
     * @return {@link Void} wrapped in an {@link ResponseEntity}
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     */
    @ResponseBody
    @RequestMapping(value = "/roles/{role_id}", method = RequestMethod.POST)
    @ResourceAccess(description = "Add the given resource access to the role of given role_id",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> addRoleResourcesAccess(@PathVariable("role_id") final Long pRoleId,
            @RequestBody @Valid final ResourcesAccess pNewResourcesAccess) throws EntityNotFoundException {
        roleService.addResourceAccesses(pRoleId, pNewResourcesAccess);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Define the endpoint removing the given {@link ResourcesAccess} of the {@link Role} with passed <code>id</code>.
     *
     * @param pRoleId
     *            The {@link Role} <code>id</code>
     * @return {@link Void} wrapped in an {@link ResponseEntity}
     * @throws EntityNotFoundException
     *             Thrown when no {@link Role} with passed <code>id</code> could be found
     * @throws EntityOperationForbiddenException
     */
    @ResponseBody
    @RequestMapping(value = "/roles/{role_id}/{resources_access_id}", method = RequestMethod.DELETE)
    @ResourceAccess(description = "Remove resource access of given resources_access_id from the role of given role_id",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> removeRoleResourcesAccess(@PathVariable("role_id") final Long pRoleId,
            @PathVariable("resources_access_id_id") final Long pResourcesAccessId)
            throws EntityNotFoundException, EntityOperationForbiddenException {
        service.removeRoleResourcesAccess(pRoleId, pResourcesAccessId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     *
     * Register given resources for the given microservice.
     *
     * @return List<ResourceAccess>
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(value = "/register/microservices/{microservicename}", method = RequestMethod.POST)
    @ResourceAccess(description = "Endpoint to register all endpoints of a microservice to the administration service.",
            role = DefaultRole.INSTANCE_ADMIN)
    @ResponseBody
    public ResponseEntity<Void> registerMicroserviceEndpoints(
            @PathVariable("microservicename") final String pMicroserviceName,
            @RequestBody @Valid final List<ResourceMapping> pResourcesToRegister) {
        service.registerResources(pResourcesToRegister, pMicroserviceName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public Resource<ResourcesAccess> toResource(final ResourcesAccess pElement, final Object... pExtras) {
        Resource<ResourcesAccess> resource = null;
        if ((pElement != null) && (pElement.getId() != null)) {
            resource = hateoasService.toResource(pElement);
            hateoasService.addLink(resource, this.getClass(), "retrieveResourceAccesses", LinkRels.SELF,
                                   MethodParamFactory.build(Long.class, pElement.getId()));
            hateoasService.addLink(resource, this.getClass(), "updateResourceAccess", LinkRels.UPDATE,
                                   MethodParamFactory.build(Long.class, pElement.getId()),
                                   MethodParamFactory.build(ResourcesAccess.class, pElement));
        } else {
            LOG.warn(String.format("Invalid %s entity. Cannot create hateoas resources", this.getClass().getName()));
        }
        return resource;
    }

}
