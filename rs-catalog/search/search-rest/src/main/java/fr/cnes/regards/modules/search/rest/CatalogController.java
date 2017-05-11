/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import java.util.Map;

import javax.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.indexer.service.ISearchService;
import fr.cnes.regards.modules.indexer.service.Searches;
import fr.cnes.regards.modules.models.domain.EntityType;
import fr.cnes.regards.modules.opensearch.service.descriptor.OpenSearchDescriptorBuilder;
import fr.cnes.regards.modules.search.domain.IRepresentation;
import fr.cnes.regards.modules.search.domain.assembler.resource.FacettedPagedResources;
import fr.cnes.regards.modules.search.rest.assembler.DatasetResourcesAssembler;
import fr.cnes.regards.modules.search.rest.assembler.FacettedPagedResourcesAssembler;
import fr.cnes.regards.modules.search.rest.assembler.PagedDatasetResourcesAssembler;
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

    /**
     *
     */
    private static final String DATAOBJECTS_DATASETS_SEARCH = "/dataobjects/datasets/search";

    /**
     *
     */
    private static final String DOCUMENTS_SEARCH = "/documents/search";

    /**
     *
     */
    private static final String DATAOBJECTS_SEARCH = "/dataobjects/search";

    /**
     *
     */
    private static final String DATASETS_SEARCH = "/datasets/search";

    /**
     *
     */
    private static final String COLLECTIONS_SEARCH = "/collections/search";

    /**
     *
     */
    private static final String SEARCH_WITH_FACETS = "/searchwithfacets";

    /**
     *
     */
    private static final String SEARCH = "/search";

    private static final String DESCRIPTOR = "/descriptor.xml";

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
     * The resource service. Autowired by Spring.
     */
    private final IResourceService resourceService;

    /**
     * The resource assembler to use for abstract entities in order to add facets. Autowired by Spring.
     */
    private final FacettedPagedResourcesAssembler<AbstractEntity> abstractEntityResourcesAssembler;

    /**
     * The resource assembler to use for dataobject in order to add facets. Autowired by Spring.
     */
    private final FacettedPagedResourcesAssembler<DataObject> dataobjectResourcesAssembler;

    /**
     * The resource assembler to use for paged datasets. Autowired by Spring.
     */
    private final DatasetResourcesAssembler datasetResourcesAssembler;

    /**
     * The resource assembler to use for datasets. Autowired by Spring.
     */
    private final PagedDatasetResourcesAssembler pagedDatasetResourcesAssembler;

    /**
     * Get current tenant at runtime and allows tenant forcing. Autowired.
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final OpenSearchDescriptorBuilder osDescriptorBuilder;

    /**
     * @param pCatalogSearchService Service performing the search from the query string. Autowired by Spring.
     * @param pSearchService Service perfoming the ElasticSearch search directly. Autowired by Spring.
     * @param pResourceService The resource service. Autowired by Spring.
     * @param pAbstractEntityResourcesAssembler The resource assembler to use for abstract entities in order to add
     * facets. Autowired by Spring.
     * @param pDataobjectResourcesAssembler The resource assembler to use for dataobject in order to add facets.
     * Autowired by Spring.
     * @param pDatasetResourcesAssembler The resource assembler to use for paged datasets. Autowired by Spring.
     * @param pPagedDatasetResourcesAssembler The resource assembler to use for datasets. Autowired by Spring.
     * @param pRuntimeTenantResolver Get current tenant at runtime and allows tenant forcing. Autowired.
     */
    public CatalogController(ICatalogSearchService pCatalogSearchService, ISearchService pSearchService, // NOSONAR
            IResourceService pResourceService,
            FacettedPagedResourcesAssembler<AbstractEntity> pAbstractEntityResourcesAssembler,
            FacettedPagedResourcesAssembler<DataObject> pDataobjectResourcesAssembler,
            DatasetResourcesAssembler pDatasetResourcesAssembler,
            PagedDatasetResourcesAssembler pPagedDatasetResourcesAssembler,
            IRuntimeTenantResolver pRuntimeTenantResolver, OpenSearchDescriptorBuilder osDescriptorBuilder) {
        super();
        catalogSearchService = pCatalogSearchService;
        searchService = pSearchService;
        resourceService = pResourceService;
        abstractEntityResourcesAssembler = pAbstractEntityResourcesAssembler;
        dataobjectResourcesAssembler = pDataobjectResourcesAssembler;
        datasetResourcesAssembler = pDatasetResourcesAssembler;
        pagedDatasetResourcesAssembler = pPagedDatasetResourcesAssembler;
        runtimeTenantResolver = pRuntimeTenantResolver;
        this.osDescriptorBuilder = osDescriptorBuilder;
    }

    /**
     * Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of
     * collection, dataset, dataobject and document.
     *
     * @param pQ the OpenSearch-format query
     * @param pPageable the page
     * @return the page of entities matching the query
     * @throws SearchException when an error occurs while parsing the query
     */
    @RequestMapping(path = SEARCH, method = RequestMethod.GET)
    @ResourceAccess(
            description = "Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of collection, dataset, dataobject and document.")
    @ResponseBody
    public ResponseEntity<PagedResources<Resource<AbstractEntity>>> searchAll(@RequestParam("q") String pQ,
            final Pageable pPageable) throws SearchException {
        SimpleSearchKey<AbstractEntity> searchKey = Searches.onAllEntities(runtimeTenantResolver.getTenant());
        Page<AbstractEntity> result = catalogSearchService.search(pQ, searchKey, null, pPageable);
        return new ResponseEntity<>(abstractEntityResourcesAssembler.toResource(result), HttpStatus.OK);
    }

    @RequestMapping(path = SEARCH + DESCRIPTOR, method = RequestMethod.GET, produces = MediaType.APPLICATION_XML_VALUE)
    @ResourceAccess(
            description = "endpoint allowing to get the OpenSearch descriptor for searches on every type of entities",
            role = DefaultRole.PUBLIC)
    @ResponseBody
    public ResponseEntity<OpenSearchDescription> searchAllDescriptor() {
        return new ResponseEntity<>(osDescriptorBuilder.build(null, CatalogController.PATH + CatalogController.SEARCH),
                HttpStatus.OK);
    }

    /**
     * Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of
     * collection, dataset, dataobject and document. Allows usage of facets.
     *
     * @param pQ the OpenSearch-format query
     * @param pFacets the facets to apply
     * @param pPageable the page
     * @return the page of entities matching the query
     * @throws SearchException when an error occurs while parsing the query
     */
    @RequestMapping(path = SEARCH_WITH_FACETS, method = RequestMethod.GET)
    @ResourceAccess(
            description = "Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of collection, dataset, dataobject and document.")
    @ResponseBody
    public ResponseEntity<FacettedPagedResources<Resource<AbstractEntity>>> searchAll(@RequestParam("q") String pQ,
            @RequestParam(value = "facets", required = false) Map<String, FacetType> pFacets, final Pageable pPageable)
            throws SearchException {
        SimpleSearchKey<AbstractEntity> searchKey = Searches.onAllEntities(runtimeTenantResolver.getTenant());
        FacetPage<AbstractEntity> result = (FacetPage<AbstractEntity>) catalogSearchService.search(pQ, searchKey,
                                                                                                   pFacets, pPageable);
        return new ResponseEntity<>(abstractEntityResourcesAssembler.toResource(result), HttpStatus.OK);
    }

    /**
     * Return the collection of passed URN_COLLECTION.
     *
     * @param pUrn the Uniform Resource Name of the collection
     * @return the collection
     * @throws SearchException
     */
    @RequestMapping(path = "/collections/{urn}", method = RequestMethod.GET)
    @ResourceAccess(description = "Return the collection of passed URN_COLLECTION.")
    @ResponseBody
    public ResponseEntity<Resource<Collection>> getCollection(@Valid @PathVariable("urn") UniformResourceName pUrn)
            throws SearchException {
        Collection collection = searchService.get(pUrn);
        Resource<Collection> resource = toResource(collection);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Perform an OpenSearch request on collections.
     *
     * @param pQ the OpenSearch-format query
     * @param pPageable the page
     * @param pAssembler injected by Spring
     * @return the page of collections matching the query
     * @throws SearchException when an error occurs while parsing the query
     */
    @RequestMapping(path = COLLECTIONS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(description = "Perform an OpenSearch request on collection.")
    @ResponseBody
    public ResponseEntity<PagedResources<Resource<Collection>>> searchCollections(@RequestParam("q") String pQ,
            final Pageable pPageable, final PagedResourcesAssembler<Collection> pAssembler) throws SearchException {
        SimpleSearchKey<Collection> searchKey = Searches.onSingleEntity(runtimeTenantResolver.getTenant(),
                                                                        EntityType.COLLECTION);
        Page<Collection> result = catalogSearchService.search(pQ, searchKey, null, pPageable);
        return new ResponseEntity<>(toPagedResources(result, pAssembler), HttpStatus.OK);
    }

    @RequestMapping(path = COLLECTIONS_SEARCH + DESCRIPTOR, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_XML_VALUE)
    @ResourceAccess(description = "endpoint allowing to get the OpenSearch descriptor for searches on collections",
            role = DefaultRole.PUBLIC)
    @ResponseBody
    public ResponseEntity<OpenSearchDescription> searchCollectionsDescriptor() {
        return new ResponseEntity<>(osDescriptorBuilder.build(EntityType.COLLECTION, PATH + COLLECTIONS_SEARCH),
                HttpStatus.OK);
    }

    /**
     * Return the dataset of passed URN_COLLECTION.
     *
     * @param pUrn the Uniform Resource Name of the dataset
     * @return the dataset
     * @throws SearchException
     */
    @RequestMapping(path = "/datasets/{urn}", method = RequestMethod.GET)
    @ResourceAccess(description = "Return the dataset of passed URN_COLLECTION.")
    @ResponseBody
    public ResponseEntity<Resource<Dataset>> getDataset(@Valid @PathVariable("urn") UniformResourceName pUrn)
            throws SearchException {
        Dataset dataset = searchService.get(pUrn);
        return new ResponseEntity<>(datasetResourcesAssembler.toResource(dataset), HttpStatus.OK);
    }

    /**
     * Perform an OpenSearch request on datasets.
     *
     * @param pQ the OpenSearch-format query
     * @param pPageable the page
     * @return the page of datasets matching the query
     * @throws SearchException when an error occurs while parsing the query
     */
    @RequestMapping(path = DATASETS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(description = "Perform an OpenSearch request on dataset.")
    @ResponseBody
    public ResponseEntity<PagedResources<Resource<Dataset>>> searchDatasets(@RequestParam("q") String pQ,
            final Pageable pPageable) throws SearchException {
        SimpleSearchKey<Dataset> searchKey = Searches.onSingleEntity(runtimeTenantResolver.getTenant(),
                                                                     EntityType.DATASET);
        Page<Dataset> result = catalogSearchService.search(pQ, searchKey, null, pPageable);
        return new ResponseEntity<>(pagedDatasetResourcesAssembler.toResource(result), HttpStatus.OK);
    }

    @RequestMapping(path = DATASETS_SEARCH + DESCRIPTOR, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_XML_VALUE)
    @ResourceAccess(description = "endpoint allowing to get the OpenSearch descriptor for searches on datasets",
            role = DefaultRole.PUBLIC)
    @ResponseBody
    public ResponseEntity<OpenSearchDescription> searchDatasetsDescriptor() {
        return new ResponseEntity<>(osDescriptorBuilder.build(EntityType.DATASET, PATH + DATASETS_SEARCH),
                HttpStatus.OK);
    }

    /**
     * Return the dataobject of passed URN_COLLECTION.
     *
     * @param pUrn the Uniform Resource Name of the dataobject
     * @return the dataobject
     * @throws SearchException
     */
    @RequestMapping(path = "/dataobjects/{urn}", method = RequestMethod.GET)
    @ResourceAccess(description = "Return the dataobject of passed URN_COLLECTION.")
    @ResponseBody
    public ResponseEntity<Resource<DataObject>> getDataobject(@Valid @PathVariable("urn") UniformResourceName pUrn)
            throws SearchException {
        DataObject dataobject = searchService.get(pUrn);
        Resource<DataObject> resource = toResource(dataobject);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Perform an OpenSearch request on dataobjects. Only return required facets.
     *
     * @param pQ the OpenSearch-format query
     * @param pFacets the facets to apply
     * @param pPageable the page
     * @return the page of dataobjects matching the query
     * @throws SearchException when an error occurs while parsing the query
     */
    @RequestMapping(path = DATAOBJECTS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(description = "Perform an OpenSearch request on dataobject. Only return required facets.")
    @ResponseBody
    public ResponseEntity<FacettedPagedResources<Resource<DataObject>>> searchDataobjects(@RequestParam("q") String pQ,
            @RequestParam(value = "facets", required = false) Map<String, FacetType> pFacets, final Pageable pPageable)
            throws SearchException {
        SimpleSearchKey<DataObject> searchKey = Searches.onSingleEntity(runtimeTenantResolver.getTenant(),
                                                                        EntityType.DATA);
        FacetPage<DataObject> result = (FacetPage<DataObject>) catalogSearchService.search(pQ, searchKey, pFacets,
                                                                                           pPageable);
        return new ResponseEntity<>(dataobjectResourcesAssembler.toResource(result), HttpStatus.OK);
    }

    @RequestMapping(path = DATAOBJECTS_SEARCH + DESCRIPTOR, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_XML_VALUE)
    @ResourceAccess(description = "endpoint allowing to get the OpenSearch descriptor for searches on data",
            role = DefaultRole.PUBLIC)
    @ResponseBody
    public ResponseEntity<OpenSearchDescription> searchDataobjectsDescriptor() {
        return new ResponseEntity<>(osDescriptorBuilder.build(EntityType.DATA, PATH + DATAOBJECTS_SEARCH),
                HttpStatus.OK);
    }

    /**
     * Perform an joined OpenSearch request. The search will be performed on dataobjects attributes, but will return the
     * associated datasets.
     *
     * @param pQ the OpenSearch-format query
     * @param pFacets the facets to apply
     * @param pPageable the page
     * @param pAssembler injected by Spring
     * @return the page of datasets matching the query
     * @throws SearchException when an error occurs while parsing the query
     */
    @RequestMapping(path = DATAOBJECTS_DATASETS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(description = "Perform an OpenSearch request on dataobject. Only return required facets.")
    @ResponseBody
    public ResponseEntity<PagedResources<Resource<Dataset>>> searchDataobjectsReturnDatasets(
            @RequestParam("q") String pQ,
            @RequestParam(value = "facets", required = false) Map<String, FacetType> pFacets, final Pageable pPageable,
            final PagedResourcesAssembler<Dataset> pAssembler) throws SearchException {
        JoinEntitySearchKey<DataObject, Dataset> searchKey = Searches
                .onSingleEntityReturningJoinEntity(runtimeTenantResolver.getTenant(), EntityType.DATA,
                                                   EntityType.DATASET);
        Page<Dataset> result = catalogSearchService.search(pQ, searchKey, pFacets, pPageable);
        return new ResponseEntity<>(toPagedResources(result, pAssembler), HttpStatus.OK);
    }

    @RequestMapping(path = DATAOBJECTS_DATASETS_SEARCH + DESCRIPTOR, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_XML_VALUE)
    @ResourceAccess(
            description = "endpoint allowing to get the OpenSearch descriptor for searches on data but result returned are datasets",
            role = DefaultRole.PUBLIC)
    @ResponseBody
    public ResponseEntity<OpenSearchDescription> searchDataobjectsReturnDatasetsDescriptor() {
        return new ResponseEntity<>(osDescriptorBuilder.build(EntityType.DATA, PATH + DATAOBJECTS_DATASETS_SEARCH),
                HttpStatus.OK);
    }

    /**
     * Return the document of passed URN_COLLECTION.
     *
     * @param pUrn the Uniform Resource Name of the document
     * @return the document
     * @throws SearchException
     */
    @RequestMapping(path = "/documents/{urn}", method = RequestMethod.GET)
    @ResourceAccess(description = "Return the document of passed URN_COLLECTION.")
    @ResponseBody
    public ResponseEntity<Resource<Document>> getDocument(@Valid @PathVariable("urn") UniformResourceName pUrn)
            throws SearchException {
        Document document = searchService.get(pUrn);
        Resource<Document> resource = toResource(document);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Perform an OpenSearch request on documents.
     *
     * @param pQ the OpenSearch-format query
     * @param pPageable the page
     * @param pAssembler injected by Spring
     * @return the page of documents matching the query
     * @throws SearchException when an error occurs while parsing the query
     */
    @RequestMapping(path = DOCUMENTS_SEARCH, method = RequestMethod.GET)
    @ResourceAccess(description = "Perform an OpenSearch request on document.")
    @ResponseBody
    public ResponseEntity<PagedResources<Resource<Document>>> searchDocuments(@RequestParam("q") String pQ,
            final Pageable pPageable, final PagedResourcesAssembler<Document> pAssembler) throws SearchException {
        SimpleSearchKey<Document> searchKey = Searches.onSingleEntity(runtimeTenantResolver.getTenant(),
                                                                      EntityType.DOCUMENT);
        Page<Document> result = catalogSearchService.search(pQ, searchKey, null, pPageable);
        return new ResponseEntity<>(toPagedResources(result, pAssembler), HttpStatus.OK);
    }

    @RequestMapping(path = DOCUMENTS_SEARCH + DESCRIPTOR, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_XML_VALUE)
    @ResourceAccess(
            description = "endpoint allowing to get the OpenSearch descriptor for searches on data but result returned are datasets",
            role = DefaultRole.PUBLIC)
    @ResponseBody
    public ResponseEntity<OpenSearchDescription> searchDocumentsDescriptor() {
        return new ResponseEntity<>(osDescriptorBuilder.build(EntityType.DOCUMENT, PATH + DOCUMENTS_SEARCH),
                HttpStatus.OK);
    }

    /**
     * Convert a list of elements to a list of {@link Resource}
     *
     * @param pElements list of elements to convert
     * @param pAssembler page resources assembler
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
