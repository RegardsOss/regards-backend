/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.search.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.validation.Valid;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.search.service.IFileEntityDescriptionHelper;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.SearchException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.Document;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.indexer.domain.JoinEntitySearchKey;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.service.ISearchService;
import fr.cnes.regards.modules.indexer.service.Searches;
import fr.cnes.regards.modules.models.domain.EntityType;
import fr.cnes.regards.modules.opensearch.service.description.OpenSearchDescriptionBuilder;
import fr.cnes.regards.modules.search.rest.assembler.DatasetResourcesAssembler;
import fr.cnes.regards.modules.search.rest.assembler.FacettedPagedResourcesAssembler;
import fr.cnes.regards.modules.search.rest.assembler.PagedDatasetResourcesAssembler;
import fr.cnes.regards.modules.search.rest.assembler.resource.FacettedPagedResources;
import fr.cnes.regards.modules.search.rest.representation.IRepresentation;
import fr.cnes.regards.modules.search.schema.OpenSearchDescription;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;
import fr.cnes.regards.modules.search.service.accessright.IAccessRightFilter;

/**
 * REST controller managing the research of REGARDS entities ({@link Collection}s, {@link Dataset}s, {@link DataObject}s
 * and {@link Document}s).
 * <p>
 * It :
 * <ol>
 * <li>Receives an OpenSearch format request, for example
 * <code>q=(tags=urn://laCollection)&type=collection&modele=ModelDeCollection</code>.
 * <li>Applies project filters by interpreting the OpenSearch query string and transforming them in ElasticSearch
 * criterion request. This is done with a plugin of type {@link IFilter}.
 * <li>Adds user group and data access filters. This is done with {@link IAccessRightFilter} service.
 * <li>Performs the ElasticSearch request on the project index. This is done with {@link IIndexService}.
 * <li>Applies {@link IRepresentation} type plugins to the response.
 * <ol>
 *
 * @author Xavier-Alexandre Brochard
 */
@RestController
@ModuleInfo(name = "search", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping(path = CatalogController.PATH)
public class CatalogController {

    public static final String DATAOBJECTS_DATASETS_SEARCH = "/dataobjects/datasets/search";

    public static final String DOCUMENTS_SEARCH = "/documents/search";

    public static final String DATAOBJECTS_SEARCH = "/dataobjects/search";

    public static final String DATASETS_SEARCH = "/datasets/search";

    public static final String DATASET_FILE = "/datasets/search";

    public static final String COLLECTIONS_SEARCH = "/collections/search";

    public static final String SEARCH_WITH_FACETS = "/searchwithfacets";

    public static final String SEARCH = "/search";

    public static final String DESCRIPTOR = "/descriptor.xml";

    public static final String ENTITY_GET_MAPPING = "/entities/{urn}";

    /**
     * The main path
     */
    static final String PATH = "";

    /**
     * Service performing the search from the query string. Autowired by Spring.
     */
    private final ICatalogSearchService catalogSearchService;

    /**
     * Service perfoming the ElasticSearch search directly. Autowired by Spring.
     */
    private final ISearchService searchService;

    /**
     * Service perfoming the ElasticSearch search directly. Autowired by Spring.
     */
    private final IFileEntityDescriptionHelper fileEntityDescriptionHelper;

    /**
     * The resource service. Autowired by Spring.
     */
    private final IResourceService resourceService;

    /**
     * Get current tenant at runtime and allows tenant forcing. Autowired.
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final OpenSearchDescriptionBuilder osDescriptorBuilder;

    /**
     * @param pCatalogSearchService
     *            Service performing the search from the query string. Autowired by Spring.
     * @param pSearchService
     *            Service perfoming the ElasticSearch search directly. Autowired by Spring.
     * @param pResourceService
     *            The resource service. Autowired by Spring.
     * @param pRuntimeTenantResolver
     *            Get current tenant at runtime and allows tenant forcing. Autowired.
     */
    public CatalogController(final ICatalogSearchService pCatalogSearchService, final ISearchService pSearchService,
            final IResourceService pResourceService, final IRuntimeTenantResolver pRuntimeTenantResolver,
            final OpenSearchDescriptionBuilder osDescriptorBuilder, IFileEntityDescriptionHelper fileEntityDescriptionHelper) {
        super();
        catalogSearchService = pCatalogSearchService;
        searchService = pSearchService;
        resourceService = pResourceService;
        runtimeTenantResolver = pRuntimeTenantResolver;
        this.osDescriptorBuilder = osDescriptorBuilder;
        this.fileEntityDescriptionHelper = fileEntityDescriptionHelper;
    }

    /**
     * Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of
     * collection, dataset, dataobject and document.
     *
     * @param allParams
     *            all query parameters
     * @param pPageable
     *            the page
     * @return the page of entities matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = SEARCH, method = RequestMethod.GET)
    @ResourceAccess(
            description = "Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of collection, dataset, dataobject and document.",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<PagedResources<Resource<AbstractEntity>>> searchAll(
            @RequestParam final Map<String, String> allParams, final Pageable pPageable,
            FacettedPagedResourcesAssembler<AbstractEntity> pAssembler) throws SearchException {
        final SimpleSearchKey<AbstractEntity> searchKey = Searches.onAllEntities(runtimeTenantResolver.getTenant());
        final FacetPage<AbstractEntity> result = catalogSearchService.search(allParams, searchKey, null, pPageable);
        return new ResponseEntity<>(pAssembler.toResource(result), HttpStatus.OK);
    }

    @RequestMapping(path = SEARCH + DESCRIPTOR, method = RequestMethod.GET, produces = MediaType.APPLICATION_XML_VALUE)
    @ResourceAccess(
            description = "endpoint allowing to get the OpenSearch descriptor for searches on every type of entities",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<OpenSearchDescription> searchAllDescriptor() throws UnsupportedEncodingException {
        return new ResponseEntity<>(osDescriptorBuilder.build(null, CatalogController.PATH + CatalogController.SEARCH),
                HttpStatus.OK);
    }

    /**
     * Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of
     * collection, dataset, dataobject and document. Allows usage of facets.
     *
     * @param allParams
     *            all query parameters
     * @param pFacets
     *            the facets to apply
     * @param pPageable
     *            the page
     * @return the page of entities matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = SEARCH_WITH_FACETS, method = RequestMethod.GET)
    @ResourceAccess(role = DefaultRole.PUBLIC,
            description = "Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of collection, dataset, dataobject and document.")
    public ResponseEntity<FacettedPagedResources<Resource<AbstractEntity>>> searchAll(
            @RequestParam final Map<String, String> allParams,
            @RequestParam(value = "facets", required = false) final String[] pFacets, final Pageable pPageable,
            FacettedPagedResourcesAssembler<AbstractEntity> pAssembler) throws SearchException {
        final SimpleSearchKey<AbstractEntity> searchKey = Searches.onAllEntities(runtimeTenantResolver.getTenant());
        final FacetPage<AbstractEntity> result = catalogSearchService.search(allParams, searchKey, pFacets, pPageable);
        return new ResponseEntity<>(pAssembler.toResource(result), HttpStatus.OK);
    }

    /**
     * Return the collection of passed URN_COLLECTION.
     *
     * @param pUrn
     *            the Uniform Resource Name of the collection
     * @return the collection
     * @throws SearchException
     */
    @RequestMapping(path = "/collections/{urn}", method = RequestMethod.GET)
    @ResourceAccess(description = "Return the collection of passed URN_COLLECTION.", role = DefaultRole.PUBLIC)
    public ResponseEntity<Resource<Collection>> getCollection(
            @Valid @PathVariable("urn") final UniformResourceName pUrn) throws SearchException {
        final Collection collection = searchService.get(pUrn);
        final Resource<Collection> resource = toResource(collection);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Perform an OpenSearch request on collections.
     *
     * @param allParams
     *            all query parameters
     * @param pPageable
     *            the page
     * @param pAssembler
     *            injected by Spring
     * @return the page of collections matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = COLLECTIONS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(description = "Perform an OpenSearch request on collection.", role = DefaultRole.PUBLIC)
    public ResponseEntity<PagedResources<Resource<Collection>>> searchCollections(
            @RequestParam final Map<String, String> allParams, final Pageable pPageable,
            final PagedResourcesAssembler<Collection> pAssembler) throws SearchException {
        final SimpleSearchKey<Collection> searchKey = Searches.onSingleEntity(runtimeTenantResolver.getTenant(),
                                                                              EntityType.COLLECTION);
        final FacetPage<Collection> result = catalogSearchService.search(allParams, searchKey, null, pPageable);
        return new ResponseEntity<>(toPagedResources(result, pAssembler), HttpStatus.OK);
    }

    @RequestMapping(path = COLLECTIONS_SEARCH + DESCRIPTOR, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_XML_VALUE)
    @ResourceAccess(description = "endpoint allowing to get the OpenSearch descriptor for searches on collections",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<OpenSearchDescription> searchCollectionsDescriptor() throws UnsupportedEncodingException {
        return new ResponseEntity<>(osDescriptorBuilder.build(EntityType.COLLECTION, PATH + COLLECTIONS_SEARCH),
                HttpStatus.OK);
    }

    /**
     * Return the dataset of passed dataset URN.
     *
     * @param pUrn
     *            the Uniform Resource Name of the dataset
     * @return the dataset
     * @throws SearchException
     */
    @RequestMapping(path = "/datasets/{urn}", method = RequestMethod.GET)
    @ResourceAccess(description = "Return the dataset of passed URN_COLLECTION.", role = DefaultRole.PUBLIC)
    public ResponseEntity<Resource<Dataset>> getDataset(@Valid @PathVariable("urn") final UniformResourceName pUrn,
                                                        DatasetResourcesAssembler assembler) throws SearchException {
        final Dataset dataset = searchService.get(pUrn);
        return new ResponseEntity<>(assembler.toResource(dataset), HttpStatus.OK);
    }

    /**
     * Return the dataset file of passed dataset URN.
     *
     * @param pUrn
     *            the Uniform Resource Name of the dataset
     * @return the dataset file
     * @throws SearchException
     */
    @RequestMapping(path = "/datasets/{urn}/file", method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Return the dataset of passed URN_COLLECTION.", role = DefaultRole.PUBLIC)
    public ResponseEntity<InputStreamResource> retrieveDatasetDescription(@RequestParam(name = "origin", required = false) String origin,
                                                                @PathVariable("urn") String pUrn) throws SearchException, EntityNotFoundException, EntityOperationForbiddenException, IOException {
        final ResponseEntity<InputStreamResource> fileStream = fileEntityDescriptionHelper.getFile(UniformResourceName.fromString(pUrn));


        InputStreamResource isr = new InputStreamResource(fileStream.getBody().getInputStream());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(fileStream.getBody().contentLength());
        headers.setContentType(fileStream.getHeaders().getContentType());
        // set the X-Frame-Options header value to ALLOW-FROM origin
        if (origin != null) {
            headers.add(com.google.common.net.HttpHeaders.X_FRAME_OPTIONS, "ALLOW-FROM " + origin);
        }
        return new ResponseEntity<>(isr, headers, HttpStatus.OK);
    }

    /**
     * Perform an OpenSearch request on datasets.
     *
     * @param allParams
     *            all query parameters
     * @param pPageable
     *            the page
     * @return the page of datasets matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = DATASETS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(description = "Perform an OpenSearch request on dataset.", role = DefaultRole.PUBLIC)
    public ResponseEntity<PagedResources<Resource<Dataset>>> searchDatasets(
            @RequestParam final Map<String, String> allParams, final Pageable pPageable,
            PagedDatasetResourcesAssembler pAssembler) throws SearchException {
        final SimpleSearchKey<Dataset> searchKey = Searches.onSingleEntity(runtimeTenantResolver.getTenant(),
                                                                           EntityType.DATASET);
        final FacetPage<Dataset> result = catalogSearchService.search(allParams, searchKey, null, pPageable);
        return new ResponseEntity<>(pAssembler.toResource(result), HttpStatus.OK);
    }

    @RequestMapping(path = DATASETS_SEARCH + DESCRIPTOR, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_XML_VALUE)
    @ResourceAccess(description = "endpoint allowing to get the OpenSearch descriptor for searches on datasets",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<OpenSearchDescription> searchDatasetsDescriptor() throws UnsupportedEncodingException {
        return new ResponseEntity<>(osDescriptorBuilder.build(EntityType.DATASET, PATH + DATASETS_SEARCH),
                HttpStatus.OK);
    }

    /**
     * Return the dataobject of passed dataobject URN.
     *
     * @param pUrn
     *            the Uniform Resource Name of the dataobject
     * @return the dataobject
     * @throws SearchException
     */
    @RequestMapping(path = "/dataobjects/{urn}", method = RequestMethod.GET)
    @ResourceAccess(description = "Return the dataobject of passed URN_COLLECTION.", role = DefaultRole.PUBLIC)
    public ResponseEntity<Resource<DataObject>> getDataobject(
            @Valid @PathVariable("urn") final UniformResourceName pUrn) throws SearchException {
        final DataObject dataobject = searchService.get(pUrn);
        final Resource<DataObject> resource = toResource(dataobject);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Perform an OpenSearch request on dataobjects. Only return required facets.
     *
     * @param allParams
     *            all query parameters
     * @param pFacets
     *            the facets to apply
     * @param pPageable
     *            the page
     * @return the page of dataobjects matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = DATAOBJECTS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(description = "Perform an OpenSearch request on dataobject. Only return required facets.",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<FacettedPagedResources<Resource<DataObject>>> searchDataobjects(
            @RequestParam final Map<String, String> allParams,
            @RequestParam(value = "facets", required = false) String[] pFacets, final Pageable pPageable,
            FacettedPagedResourcesAssembler<DataObject> pAssembler) throws SearchException {
        final SimpleSearchKey<DataObject> searchKey = Searches.onSingleEntity(runtimeTenantResolver.getTenant(),
                                                                              EntityType.DATA);
        final FacetPage<DataObject> result = catalogSearchService.search(allParams, searchKey, pFacets, pPageable);
        return new ResponseEntity<>(pAssembler.toResource(result), HttpStatus.OK);
    }

    @RequestMapping(path = DATAOBJECTS_SEARCH + DESCRIPTOR, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_XML_VALUE)
    @ResourceAccess(description = "endpoint allowing to get the OpenSearch descriptor for searches on data",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<OpenSearchDescription> searchDataobjectsDescriptor() throws UnsupportedEncodingException {
        return new ResponseEntity<>(osDescriptorBuilder.build(EntityType.DATA, PATH + DATAOBJECTS_SEARCH),
                HttpStatus.OK);
    }

    /**
     * Perform an joined OpenSearch request. The search will be performed on dataobjects attributes, but will return the
     * associated datasets.
     *
     * @param allParams
     *            all query parameters
     * @param pFacets
     *            the facets to apply
     * @param pPageable
     *            the page
     * @param pAssembler
     *            injected by Spring
     * @return the page of datasets matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = DATAOBJECTS_DATASETS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(
            description = "Perform an joined OpenSearch request. The search will be performed on dataobjects attributes, but will return the associated datasets.",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<PagedResources<Resource<Dataset>>> searchDataobjectsReturnDatasets(
            @RequestParam final Map<String, String> allParams,
            @RequestParam(value = "facets", required = false) final String[] pFacets, final Pageable pPageable,
            final PagedResourcesAssembler<Dataset> pAssembler) throws SearchException {
        final JoinEntitySearchKey<DataObject, Dataset> searchKey = Searches
                .onSingleEntityReturningJoinEntity(runtimeTenantResolver.getTenant(), EntityType.DATA,
                                                   EntityType.DATASET);
        final FacetPage<Dataset> result = catalogSearchService.search(allParams, searchKey, pFacets, pPageable);
        return new ResponseEntity<>(toPagedResources(result, pAssembler), HttpStatus.OK);
    }

    @RequestMapping(path = DATAOBJECTS_DATASETS_SEARCH + DESCRIPTOR, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_XML_VALUE)
    @ResourceAccess(
            description = "endpoint allowing to get the OpenSearch descriptor for searches on data but result returned are datasets",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<OpenSearchDescription> searchDataobjectsReturnDatasetsDescriptor()
            throws UnsupportedEncodingException {
        return new ResponseEntity<>(osDescriptorBuilder.build(EntityType.DATA, PATH + DATAOBJECTS_DATASETS_SEARCH),
                HttpStatus.OK);
    }

    /**
     * Return the document of passed URN_COLLECTION.
     *
     * @param pUrn
     *            the Uniform Resource Name of the document
     * @return the document
     * @throws SearchException
     */
    @RequestMapping(path = "/documents/{urn}", method = RequestMethod.GET)
    @ResourceAccess(description = "Return the document of passed URN_COLLECTION.", role = DefaultRole.PUBLIC)
    public ResponseEntity<Resource<Document>> getDocument(@Valid @PathVariable("urn") final UniformResourceName pUrn)
            throws SearchException {
        final Document document = searchService.get(pUrn);
        final Resource<Document> resource = toResource(document);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Unified entity retrieval endpoint
     * @param pUrn the entity URN
     * @return an entity
     * @throws SearchException if error occurs.
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(path = ENTITY_GET_MAPPING, method = RequestMethod.GET)
    @ResourceAccess(description = "Return the entity of passed URN_COLLECTION.", role = DefaultRole.PUBLIC)
    public <E extends AbstractEntity> ResponseEntity<Resource<E>> getEntity(
            @Valid @PathVariable("urn") final UniformResourceName pUrn, DatasetResourcesAssembler assembler)
            throws SearchException {
        // Retrieve entity
        E indexable = searchService.get(pUrn);
        // Prepare resource according to its type
        Resource<E> resource;
        if (EntityType.DATASET.name().equals(indexable.getType())) {
            resource = (Resource<E>) assembler.toResource((Dataset) indexable);
        } else {
            resource = toResource(indexable);
        }
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Perform an OpenSearch request on documents.
     *
     * @param allParams
     *            all query parameters
     * @param pPageable
     *            the page
     * @param pAssembler
     *            injected by Spring
     * @return the page of documents matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = DOCUMENTS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(description = "Perform an OpenSearch request on document.", role = DefaultRole.PUBLIC)
    public ResponseEntity<PagedResources<Resource<Document>>> searchDocuments(
            @RequestParam final Map<String, String> allParams, final Pageable pPageable,
            final PagedResourcesAssembler<Document> pAssembler) throws SearchException {
        final SimpleSearchKey<Document> searchKey = Searches.onSingleEntity(runtimeTenantResolver.getTenant(),
                                                                            EntityType.DOCUMENT);
        final FacetPage<Document> result = catalogSearchService.search(allParams, searchKey, null, pPageable);
        return new ResponseEntity<>(toPagedResources(result, pAssembler), HttpStatus.OK);
    }

    @RequestMapping(path = DOCUMENTS_SEARCH + DESCRIPTOR, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_XML_VALUE)
    @ResourceAccess(
            description = "endpoint allowing to get the OpenSearch descriptor for searches on data but result returned are datasets",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<OpenSearchDescription> searchDocumentsDescriptor() throws UnsupportedEncodingException {
        return new ResponseEntity<>(osDescriptorBuilder.build(EntityType.DOCUMENT, PATH + DOCUMENTS_SEARCH),
                HttpStatus.OK);
    }

    /**
     * Convert a list of elements to a list of {@link Resource}
     *
     * @param pElements
     *            list of elements to convert
     * @param pAssembler
     *            page resources assembler
     * @return a list of {@link Resource}
     */
    private <T> PagedResources<Resource<T>> toPagedResources(final Page<T> pElements,
            final PagedResourcesAssembler<T> pAssembler) {
        Assert.notNull(pElements);
        final PagedResources<Resource<T>> pageResources = pAssembler.toResource(pElements);
        pageResources.forEach(resource -> resource.add(toResource(resource.getContent()).getLinks()));
        return pageResources;
    }

    private <T> Resource<T> toResource(final T pElement) {
        return resourceService.toResource(pElement);
    }

}
