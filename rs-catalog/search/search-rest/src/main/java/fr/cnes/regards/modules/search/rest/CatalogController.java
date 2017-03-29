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
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.SearchException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
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
import fr.cnes.regards.modules.search.domain.IRepresentation;
import fr.cnes.regards.modules.search.rest.facet.FacettedPagedResources;
import fr.cnes.regards.modules.search.rest.facet.FacettedPagedResourcesAssembler;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;
import fr.cnes.regards.modules.search.service.accessright.IAccessRightFilter;

/**
 * REST controller managing the research of REGARDS entities ({@link Collection}s, {@link Dataset}s, {@link DataObject}s
 * and {@link Document}s).
 *
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
     * The main path
     */
    static final String PATH = "";

    /**
     * Service performing the search from the query string
     */
    private final ICatalogSearchService catalogSearchService;

    /**
     * Service perfoming the ElasticSearch search directly
     */
    private final ISearchService searchService;

    /**
     * The resource service
     */
    private final IResourceService resourceService;

    /**
     * The resource assembler to use for abstract entities in order to add facets
     */
    private final FacettedPagedResourcesAssembler<AbstractEntity> abstractEntityResourcesAssembler;

    /**
     * The resource assembler to use for dataobject in order to add facets
     */
    private final FacettedPagedResourcesAssembler<DataObject> dataobjectResourcesAssembler;

    /**
     * Get current tenant at runtime and allows tenant forcing. Autowired.
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * @param pCatalogSearchService
     * @param pSearchService
     * @param pResourceService
     * @param pAbstractEntityResourcesAssembler
     * @param pDataobjectResourcesAssembler
     * @param pRuntimeTenantResolver
     */
    public CatalogController(ICatalogSearchService pCatalogSearchService, ISearchService pSearchService,
            IResourceService pResourceService,
            FacettedPagedResourcesAssembler<AbstractEntity> pAbstractEntityResourcesAssembler,
            FacettedPagedResourcesAssembler<DataObject> pDataobjectResourcesAssembler,
            IRuntimeTenantResolver pRuntimeTenantResolver) {
        super();
        catalogSearchService = pCatalogSearchService;
        searchService = pSearchService;
        resourceService = pResourceService;
        abstractEntityResourcesAssembler = pAbstractEntityResourcesAssembler;
        dataobjectResourcesAssembler = pDataobjectResourcesAssembler;
        runtimeTenantResolver = pRuntimeTenantResolver;
    }

    /**
     * Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of
     * collection, dataset, dataobject and document.
     *
     * @param pQ
     *            the OpenSearch-format query
     * @param pFacets
     *            the facets to apply
     * @param pPageable
     *            the page
     * @param pAssembler
     *            injected by Spring
     * @return the page of entities matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = "/search", method = RequestMethod.GET)
    @ResourceAccess(
            description = "Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of collection, dataset, dataobject and document.")
    public ResponseEntity<PagedResources<Resource<AbstractEntity>>> searchAll(@RequestParam("q") String pQ,
            final Pageable pPageable) throws SearchException {
        SimpleSearchKey<AbstractEntity> searchKey = Searches.onAllEntities(runtimeTenantResolver.getTenant());
        Page<AbstractEntity> result = catalogSearchService.search(pQ, searchKey, null, pPageable);
        return new ResponseEntity<>(abstractEntityResourcesAssembler.toResource(result), HttpStatus.OK);
    }

    /**
     * Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of
     * collection, dataset, dataobject and document. Allows usage of facets.
     *
     * @param pQ
     *            the OpenSearch-format query
     * @param pFacets
     *            the facets to apply
     * @param pPageable
     *            the page
     * @param pAssembler
     *            injected by Spring
     * @return the page of entities matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = "/searchwithfacets", method = RequestMethod.GET)
    @ResourceAccess(
            description = "Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of collection, dataset, dataobject and document.")
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
     * @param pUrn
     *            the Uniform Resource Name of the collection
     * @return the collection
     * @throws SearchException
     */
    @RequestMapping(path = "/collections/{urn}", method = RequestMethod.GET)
    @ResourceAccess(description = "Return the collection of passed URN_COLLECTION.")
    public ResponseEntity<Resource<Collection>> getCollection(@Valid @PathVariable("urn") UniformResourceName pUrn)
            throws SearchException {
        Collection collection = searchService.get(pUrn);
        Resource<Collection> resource = toResource(collection);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Perform an OpenSearch request on collections.
     *
     * @param pQ
     *            the OpenSearch-format query
     * @param pPageable
     *            the page
     * @param pAssembler
     *            injected by Spring
     * @return the page of collections matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = "/collections/search", method = RequestMethod.GET)
    @ResourceAccess(description = "Perform an OpenSearch request on collection.")
    public ResponseEntity<PagedResources<Resource<Collection>>> searchCollections(@RequestParam("q") String pQ,
            final Pageable pPageable, final PagedResourcesAssembler<Collection> pAssembler) throws SearchException {
        SimpleSearchKey<Collection> searchKey = Searches.onSingleEntity(runtimeTenantResolver.getTenant(),
                                                                        EntityType.COLLECTION);
        Page<Collection> result = catalogSearchService.search(pQ, searchKey, null, pPageable);
        return new ResponseEntity<>(toPagedResources(result, pAssembler), HttpStatus.OK);
    }

    /**
     * Return the dataset of passed URN_COLLECTION.
     *
     * @param pUrn
     *            the Uniform Resource Name of the dataset
     * @return the dataset
     * @throws SearchException
     */
    @RequestMapping(path = "/datasets/{urn}", method = RequestMethod.GET)
    @ResourceAccess(description = "Return the dataset of passed URN_COLLECTION.")
    public ResponseEntity<Resource<Dataset>> getDataset(@Valid @PathVariable("urn") UniformResourceName pUrn)
            throws SearchException {
        Dataset dataset = searchService.get(pUrn);
        Resource<Dataset> resource = toResource(dataset);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Perform an OpenSearch request on datasets.
     *
     * @param pQ
     *            the OpenSearch-format query
     * @param pPageable
     *            the page
     * @param pAssembler
     *            injected by Spring
     * @return the page of datasets matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = "/datasets/search", method = RequestMethod.GET)
    @ResourceAccess(description = "Perform an OpenSearch request on dataset.")
    public ResponseEntity<PagedResources<Resource<Dataset>>> searchDatasets(@RequestParam("q") String pQ,
            final Pageable pPageable, final PagedResourcesAssembler<Dataset> pAssembler) throws SearchException {
        SimpleSearchKey<Dataset> searchKey = Searches.onSingleEntity(runtimeTenantResolver.getTenant(),
                                                                     EntityType.DATASET);
        Page<Dataset> result = catalogSearchService.search(pQ, searchKey, null, pPageable);
        return new ResponseEntity<>(toPagedResources(result, pAssembler), HttpStatus.OK);
    }

    /**
     * Return the dataobject of passed URN_COLLECTION.
     *
     * @param pUrn
     *            the Uniform Resource Name of the dataobject
     * @return the dataobject
     * @throws SearchException
     */
    @RequestMapping(path = "/dataobjects/{urn}", method = RequestMethod.GET)
    @ResourceAccess(description = "Return the dataobject of passed URN_COLLECTION.")
    public ResponseEntity<Resource<DataObject>> getDataobject(@Valid @PathVariable("urn") UniformResourceName pUrn)
            throws SearchException {
        DataObject dataobject = searchService.get(pUrn);
        Resource<DataObject> resource = toResource(dataobject);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Perform an OpenSearch request on dataobjects. Only return required facets.
     *
     * @param pQ
     *            the OpenSearch-format query
     * @param pFacets
     *            the facets to apply
     * @param pPageable
     *            the page
     * @return the page of dataobjects matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = "/dataobjects/search", method = RequestMethod.GET)
    @ResourceAccess(description = "Perform an OpenSearch request on dataobject. Only return required facets.")
    public ResponseEntity<FacettedPagedResources<Resource<DataObject>>> searchDataobjects(@RequestParam("q") String pQ,
            @RequestParam(value = "facets", required = false) Map<String, FacetType> pFacets, final Pageable pPageable)
            throws SearchException {
        SimpleSearchKey<DataObject> searchKey = Searches.onSingleEntity(runtimeTenantResolver.getTenant(),
                                                                        EntityType.DATA);
        FacetPage<DataObject> result = (FacetPage<DataObject>) catalogSearchService.search(pQ, searchKey, pFacets,
                                                                                           pPageable);
        return new ResponseEntity<>(dataobjectResourcesAssembler.toResource(result), HttpStatus.OK);
    }

    /**
     * Perform an joined OpenSearch request. The search will be performed on dataobjects attributes, but will return the
     * associated datasets.
     *
     * @param pQ
     *            the OpenSearch-format query
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
    @RequestMapping(path = "/dataobjects/datasets/search", method = RequestMethod.GET)
    @ResourceAccess(description = "Perform an OpenSearch request on dataobject. Only return required facets.")
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

    /**
     * Return the document of passed URN_COLLECTION.
     *
     * @param pUrn
     *            the Uniform Resource Name of the document
     * @return the document
     * @throws SearchException
     */
    @RequestMapping(path = "/documents/{urn}", method = RequestMethod.GET)
    @ResourceAccess(description = "Return the document of passed URN_COLLECTION.")
    public ResponseEntity<Resource<Document>> getDocument(@Valid @PathVariable("urn") UniformResourceName pUrn)
            throws SearchException {
        Document document = searchService.get(pUrn);
        Resource<Document> resource = toResource(document);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Perform an OpenSearch request on documents.
     *
     * @param pQ
     *            the OpenSearch-format query
     * @param pPageable
     *            the page
     * @param pAssembler
     *            injected by Spring
     * @return the page of documents matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = "/documents/search", method = RequestMethod.GET)
    @ResourceAccess(description = "Perform an OpenSearch request on document.")
    public ResponseEntity<PagedResources<Resource<Document>>> searchDocuments(@RequestParam("q") String pQ,
            final Pageable pPageable, final PagedResourcesAssembler<Document> pAssembler) throws SearchException {
        SimpleSearchKey<Document> searchKey = Searches.onSingleEntity(runtimeTenantResolver.getTenant(),
                                                                      EntityType.DOCUMENT);
        Page<Document> result = catalogSearchService.search(pQ, searchKey, null, pPageable);
        return new ResponseEntity<>(toPagedResources(result, pAssembler), HttpStatus.OK);
    }

    /**
     * Convert a list of elements to a list of {@link Resource}
     *
     * @param pElements
     *            list of elements to convert
     * @param pExtras
     *            Extra URL path parameters for links
     * @return a list of {@link Resource}
     */
    private <T> PagedResources<Resource<T>> toPagedResources(final Page<T> pElements,
            final PagedResourcesAssembler<T> pAssembler, final Object... pExtras) {
        Assert.notNull(pElements);
        final PagedResources<Resource<T>> pageResources = pAssembler.toResource(pElements);
        pageResources.forEach(resource -> resource.add(toResource(resource.getContent(), pExtras).getLinks()));
        return pageResources;
    }

    private <T> Resource<T> toResource(final T pElement, final Object... pExtras) {
        // TODO: Add links
        return resourceService.toResource(pElement);
    }

}
