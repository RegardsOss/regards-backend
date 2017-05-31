/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.entities.rest;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.DescriptionFile;
import fr.cnes.regards.modules.entities.service.ICollectionService;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;

/**
 * @author lmieulet
 */
@RestController
// CHECKSTYLE:OFF
@ModuleInfo(name = "collections", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
// CHECKSTYLE:ON
@RequestMapping(path = CollectionController.ROOT_MAPPING, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class CollectionController implements IResourceController<Collection> {

    public static final String ROOT_MAPPING = "/collections";

    public static final String COLLECTION_ID_PATH_FILE = "/{collection_id}/file";

    /**
     * Service
     */
    @Autowired
    private ICollectionService collectionService;

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
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "endpoint to retrieve the list fo all collections")
    public HttpEntity<List<Resource<Collection>>> retrieveCollections() {

        final List<Collection> collections = collectionService.findAll();
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
    @RequestMapping(method = RequestMethod.GET, value = "/{collection_id}")
    @ResponseBody
    @ResourceAccess(description = "Retrieve a collection")
    public HttpEntity<Resource<Collection>> retrieveCollection(
            @PathVariable("collection_id") final Long pCollectionId) {
        final Collection collection = collectionService.load(pCollectionId);
        final Resource<Collection> resource = toResource(collection);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = COLLECTION_ID_PATH_FILE)
    @ResourceAccess(description = "Retrieves a collection description file content")
    public void retrieveCollectionDescription(@PathVariable("collection_id") Long pCollectionId,
            HttpServletResponse response) throws EntityNotFoundException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DescriptionFile file = collectionService.retrieveDescription(pCollectionId);
        if (file != null) {
            out.write(file.getContent());
            response.setContentType(file.getType().toString());
            response.setContentLength(out.size());
            response.getOutputStream().write(out.toByteArray());
            response.getOutputStream().flush();
            response.setStatus(HttpStatus.OK.value());
        } else {
            response.setStatus(HttpStatus.NO_CONTENT.value());
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, value = COLLECTION_ID_PATH_FILE)
    @ResourceAccess(description = "remove a dataset description file content")
    public ResponseEntity<Void> removeDatasetDescription(@PathVariable("collection_id") Long pDatasetId)
            throws EntityNotFoundException {
        collectionService.removeDescription(pDatasetId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Entry point to update a collection using its id
     *
     * @param pCollectionId
     *            {@link Collection} id
     * @param pCollection
     *            {@link Collection}
     * @param pResult
     *            for validation of entites' properties
     * @return update {@link Collection} as a {@link Resource}
     * @throws ModuleException
     *             if error occurs! @
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{collection_id}")
    @ResponseBody
    @ResourceAccess(description = "Update a collection")
    public HttpEntity<Resource<Collection>> updateCollection(@PathVariable("collection_id") final Long pCollectionId,
            @Valid @RequestPart(name = "collection") final Collection pCollection,
            @RequestPart(name = "description", required = false) final MultipartFile descriptionFile,
            final BindingResult pResult) throws ModuleException, IOException {

        // Validate dynamic model
        collectionService.validate(pCollection, pResult, false);

        final Collection collection = collectionService.update(pCollectionId, pCollection, descriptionFile);
        final Resource<Collection> resource = toResource(collection);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to delete a collection using its id
     *
     * @param pCollectionId
     *            {@link Collection} id
     * @return nothing
     * @throws EntityNotFoundException
     * @
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/{collection_id}")
    @ResponseBody
    @ResourceAccess(description = "delete the collection of collection_id")
    public HttpEntity<Void> deleteCollection(@PathVariable("collection_id") final Long pCollectionId)
            throws EntityNotFoundException {
        collectionService.delete(pCollectionId);
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
     * @throws IOException
     * @
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "create a new collection according to what is passed as parameter")
    public ResponseEntity<Resource<Collection>> createCollection(
            @Valid @RequestPart(name = "collection") final Collection pCollection,
            @RequestPart(name = "description", required = false) final MultipartFile descriptionFile,
            final BindingResult pResult) throws ModuleException, IOException {

        // Validate dynamic model
        collectionService.validate(pCollection, pResult, false);

        final Collection collection = collectionService.create(pCollection, descriptionFile);
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
    public HttpEntity<Resource<Collection>> dissociateCollection(
            @PathVariable("collection_id") final Long pCollectionId,
            @Valid @RequestBody final Set<UniformResourceName> pToBeDissociated) throws ModuleException {
        final Collection collection = collectionService.dissociate(pCollectionId, pToBeDissociated);
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
    @ResourceAccess(description = "Associate the collection of id collection_id to the list of entities in parameter")
    public HttpEntity<Resource<Collection>> associateCollection(@PathVariable("collection_id") final Long pCollectionId,
            @Valid @RequestBody final Set<UniformResourceName> pToBeAssociatedWith) throws ModuleException {
        final Collection collection = collectionService.associate(pCollectionId, pToBeAssociatedWith);
        final Resource<Collection> resource = toResource(collection);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    public Resource<Collection> toResource(final Collection pElement, final Object... pExtras) {
        final Resource<Collection> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource, this.getClass(), "retrieveCollection", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource, this.getClass(), "retrieveCollections", LinkRels.LIST);
        resourceService.addLink(resource, this.getClass(), "deleteCollection", LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource, this.getClass(), "updateCollection", LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, pElement.getId()),
                                MethodParamFactory.build(Collection.class),
                                MethodParamFactory.build(MultipartFile.class),
                                MethodParamFactory.build(BindingResult.class));
        resourceService.addLink(resource, this.getClass(), "dissociateCollection", "dissociate",
                                MethodParamFactory.build(Long.class, pElement.getId()),
                                MethodParamFactory.build(Set.class));
        resourceService.addLink(resource, this.getClass(), "associateCollection", "associate",
                                MethodParamFactory.build(Long.class, pElement.getId()),
                                MethodParamFactory.build(Set.class));
        return resource;
    }
}
