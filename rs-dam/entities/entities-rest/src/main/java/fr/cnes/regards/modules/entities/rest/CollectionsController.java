/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.entities.rest;

import java.util.List;
import java.util.Set;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.service.ICollectionService;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;

/**
 * @author lmieulet
 *
 */
@RestController
// CHECKSTYLE:OFF
@ModuleInfo(name = "collections", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
// CHECKSTYLE:ON
@RequestMapping(value = "/collections")
public class CollectionsController implements IResourceController<Collection> {

    /**
     * Service
     */
    @Autowired
    private ICollectionService collectionsRequestService;

    /**
     * HATEOAS service
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Entry point to retrieve {@link Collection}s
     *
     * @return all {@link Collection}s
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "endpoint to retrieve the list fo all collections")
    public HttpEntity<List<Resource<Collection>>> retrieveCollectionList() {

        final List<Collection> collections = collectionsRequestService.retrieveCollectionList();
        final List<Resource<Collection>> resources = toResources(collections);
        return new ResponseEntity<>(resources, HttpStatus.OK);

    }

    /**
     * Entry point to retrieve a collection using its id
     *
     * @param pCollectionId
     *            {@link Collection} id
     * @return {@link Collection} as a {@link Resource}
     */
    @RequestMapping(method = RequestMethod.GET, value = "/{collection_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Retrieve a collection")
    public HttpEntity<Resource<Collection>> retrieveCollection(@PathVariable("collection_id") Long pCollectionId) {
        final Collection collection = collectionsRequestService.retrieveCollectionById(pCollectionId);
        final Resource<Collection> resource = toResource(collection);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to update a collection using its id
     *
     * @param pCollectionId
     *            {@link Collection} id
     * @param pCollection
     *            {@link Collection}
     * @return update {@link Collection} as a {@link Resource}
     * @throws ModuleException
     *             if error occurs!
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{collection_id}")
    @ResponseBody
    @ResourceAccess(description = "Update a collection")
    public HttpEntity<Resource<Collection>> updateCollection(@PathVariable("collection_id") Long pCollectionId,
            @Valid @RequestBody Collection pCollection, BindingResult pResult) throws ModuleException {

        // Validate dynamic model
        collectionsRequestService.validate(pCollection, pResult, false);

        final Collection collection = collectionsRequestService.update(pCollectionId, pCollection);
        final Resource<Collection> resource = toResource(collection);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     *
     * Entry point to delete a collection using its id
     *
     * @param pCollectionId
     *            {@link Collection} id
     * @return nothing
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{collection_id}")
    @ResponseBody
    @ResourceAccess(description = "delete the collection of collection_id")
    public HttpEntity<Void> deleteCollection(@PathVariable("collection_id") Long pCollectionId)
            throws EntityNotFoundException {
        collectionsRequestService.delete(pCollectionId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Entry point to create a collection
     *
     * @param pCollection
     *            {@link Collection} to create
     * @param pResult
     *            validation errors
     * @return {@link Collection} as a {@link Resource}
     * @throws ModuleException
     *             if validation fails
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    @ResourceAccess(description = "create a new collection according to what is passed as parameter")
    public HttpEntity<Resource<Collection>> createCollection(@Valid @RequestBody Collection pCollection,
            BindingResult pResult) throws ModuleException {

        // Validate dynamic model
        collectionsRequestService.validate(pCollection, pResult, false);

        final Collection collection = collectionsRequestService.create(pCollection);
        final Resource<Collection> resource = toResource(collection);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    /**
     * Entry point to handle dissociation of {@link Collection} specified by its id to other entities
     *
     * @param pCollectionId
     *            {@link Collection} id
     * @param pToBeDissociated
     *            entity to dissociate
     * @return {@link Collection} as a {@link Resource}
     * @throws ModuleException
     *             if error occurs
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{collection_id}/dissociate")
    @ResponseBody
    @ResourceAccess(description = "Dissociate a collection from  a list of entities")
    public HttpEntity<Resource<Collection>> dissociateCollection(@PathVariable("collection_id") Long pCollectionId,
            @Valid @RequestBody Set<UniformResourceName> pToBeDissociated) throws ModuleException {
        final Collection collection = (Collection) collectionsRequestService.dissociate(pCollectionId,
                                                                                        pToBeDissociated);
        final Resource<Collection> resource = toResource(collection);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to handle association of {@link Collection} specified by its id to other entities
     *
     * @param pCollectionId
     *            {@link Collection} id
     * @param pToBeAssociatedWith
     *            entities to be associated
     * @return {@link Collection} as a {@link Resource}
     * @throws ModuleException
     *             if error occurs
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{collection_id}/associate")
    @ResponseBody
    @ResourceAccess(description = "associate the collection of id collection_id to the list of entities in parameter")
    public HttpEntity<Resource<Collection>> associateCollections(@PathVariable("collection_id") Long pCollectionId,
            @Valid @RequestBody Set<UniformResourceName> pToBeAssociatedWith) throws ModuleException {
        final Collection collection = (Collection) collectionsRequestService.associate(pCollectionId,
                                                                                       pToBeAssociatedWith);
        final Resource<Collection> resource = toResource(collection);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    public Resource<Collection> toResource(Collection pElement, Object... pExtras) {
        final Resource<Collection> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource, this.getClass(), "retrieveCollection", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource, this.getClass(), "retrieveCollectionList", LinkRels.LIST);
        resourceService.addLink(resource, this.getClass(), "deleteCollection", LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource, this.getClass(), "updateCollection", LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, pElement.getId()),
                                MethodParamFactory.build(Collection.class));
        return resource;
    }
}
