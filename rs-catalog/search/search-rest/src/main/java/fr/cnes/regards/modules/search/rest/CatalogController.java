/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableMap;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.SearchException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.crawler.domain.IIndexable;
import fr.cnes.regards.modules.crawler.domain.SearchKey;
import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;
import fr.cnes.regards.modules.crawler.domain.criterion.StringMatchCriterion;
import fr.cnes.regards.modules.crawler.domain.facet.FacetType;
import fr.cnes.regards.modules.crawler.service.IIndexerService;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.Document;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;
import fr.cnes.regards.modules.search.service.accessright.IAccessRightFilter;
import fr.cnes.regards.modules.search.service.converter.IConverter;
import fr.cnes.regards.modules.search.service.filter.IFilterPlugin;
import fr.cnes.regards.modules.search.service.queryparser.RegardsQueryParser;
import fr.cnes.regards.modules.search.service.representation.IRepresentation;

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
 * criterion request. This is done with a plugin of type {@link IFilterPlugin}.
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
@RequestMapping("")
public class CatalogController {

    /**
     * The custom OpenSearch query parser building {@link ICriterion} from tu string query
     */
    @Autowired
    private final RegardsQueryParser queryParser;

    /**
     * Applies project filters, i.e. the OpenSearch query
     */
    @Autowired
    private final IFilterPlugin filterPlugin;

    /**
     * Adds user group and data access filters
     */
    @Autowired
    private final IAccessRightFilter accessRightFilter;

    /**
     * Service perfoming the ElasticSearch search
     */
    @Autowired
    private final IIndexerService indexerService;

    /**
     * Converts entities after search
     */
    @Autowired
    private final IConverter converter;

    /**
     * Get current tenant at runtime and allows tenant forcing
     */
    @Autowired
    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * The resource service
     */
    @Autowired
    private final IResourceService resourceService;

    /**
     * Map associating a {@link SearchType} and the corresponding class
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final ImmutableMap<String, Class<?>> TO_RESULT_CLASS = new ImmutableMap.Builder()
            .put(SearchType.ALL, AbstractEntity.class).put(SearchType.COLLECTION, Collection.class)
            .put(SearchType.DATASET, Dataset.class).put(SearchType.DATAOBJECT, DataObject.class)
            .put(SearchType.DOCUMENT, Document.class).build();

    /**
     * @param pQueryParser
     * @param pFilterPlugin
     * @param pAccessRightFilter
     * @param pIndexerService
     * @param pConverter
     * @param pRuntimeTenantResolver
     * @param pResourceService
     */
    public CatalogController(RegardsQueryParser pQueryParser, IFilterPlugin pFilterPlugin,
            IAccessRightFilter pAccessRightFilter, IIndexerService pIndexerService, IConverter pConverter,
            IRuntimeTenantResolver pRuntimeTenantResolver, IResourceService pResourceService) {
        super();
        queryParser = pQueryParser;
        filterPlugin = pFilterPlugin;
        accessRightFilter = pAccessRightFilter;
        indexerService = pIndexerService;
        converter = pConverter;
        runtimeTenantResolver = pRuntimeTenantResolver;
        resourceService = pResourceService;
    }

    /**
     * Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of
     * collection, dataset, dataobject and document.
     *
     * @param pQ
     *            the OpenSearch-format query
     * @param pFacets
     *            the facets to apply
     * @param pSort
     *            the sort
     * @param pPageable
     *            the page
     * @param pAssembler
     *            injected by Spring
     * @return the page of entities matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = "/search", method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(
            description = "Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of collection, dataset, dataobject and document.")
    public ResponseEntity<PagedResources<Resource<AbstractEntity>>> searchAll(@RequestParam("q") String pQ,
            @RequestParam("facets") List<String> pFacets, @RequestParam("sort") Sort pSort, final Pageable pPageable,
            final PagedResourcesAssembler<AbstractEntity> pAssembler) throws SearchException {
        return doSearch(pQ, SearchType.ALL, pFacets, pSort, pPageable, pAssembler);
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
    @ResponseBody
    @ResourceAccess(description = "Return the collection of passed URN_COLLECTION.")
    public ResponseEntity<Resource<Collection>> getCollection(@PathVariable("urn") UniformResourceName pUrn)
            throws SearchException {
        Collection collection = indexerService.get(pUrn);
        Resource<Collection> resource = toResource(collection);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Perform an OpenSearch request on collections.
     *
     * @param pQ
     *            the OpenSearch-format query
     * @param pSort
     *            the sort
     * @param pPageable
     *            the page
     * @param pAssembler
     *            injected by Spring
     * @return the page of collections matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = "/collections/search", method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Perform an OpenSearch request on collection.")
    public ResponseEntity<PagedResources<Resource<Collection>>> searchCollections(@RequestParam("q") String pQ,
            @RequestParam("sort") Sort pSort, final Pageable pPageable,
            final PagedResourcesAssembler<Collection> pAssembler) throws SearchException {
        return doSearch(pQ, SearchType.COLLECTION, null, pSort, pPageable, pAssembler);
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
    @ResponseBody
    @ResourceAccess(description = "Return the dataset of passed URN_COLLECTION.")
    public ResponseEntity<Resource<Dataset>> getDataset(@PathVariable("urn") UniformResourceName pUrn)
            throws SearchException {
        Dataset dataset = indexerService.get(pUrn);
        Resource<Dataset> resource = toResource(dataset);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Perform an OpenSearch request on datasets.
     *
     * @param pQ
     *            the OpenSearch-format query
     * @param pSort
     *            the sort
     * @param pPageable
     *            the page
     * @param pAssembler
     *            injected by Spring
     * @return the page of datasets matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = "/datasets/search", method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Perform an OpenSearch request on dataset.")
    public ResponseEntity<PagedResources<Resource<Dataset>>> searchDatasets(@RequestParam("q") String pQ,
            @RequestParam("sort") Sort pSort, final Pageable pPageable,
            final PagedResourcesAssembler<Dataset> pAssembler) throws SearchException {
        return doSearch(pQ, SearchType.DATASET, null, pSort, pPageable, pAssembler);
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
    @ResponseBody
    @ResourceAccess(description = "Return the dataobject of passed URN_COLLECTION.")
    public ResponseEntity<Resource<DataObject>> getDataobject(@PathVariable("urn") UniformResourceName pUrn)
            throws SearchException {
        DataObject dataobject = indexerService.get(pUrn);
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
     * @param pSort
     *            the sort
     * @param pPageable
     *            the page
     * @param pAssembler
     *            injected by Spring
     * @return the page of dataobjects matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = "/dataobjects/search", method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Perform an OpenSearch request on dataobject. Only return required facets.")
    public ResponseEntity<PagedResources<Resource<DataObject>>> searchDataobjects(@RequestParam("q") String pQ,
            @RequestParam("facets") List<String> pFacets, @RequestParam("sort") Sort pSort, final Pageable pPageable,
            final PagedResourcesAssembler<DataObject> pAssembler) throws SearchException {
        return doSearch(pQ, SearchType.DATAOBJECT, pFacets, pSort, pPageable, pAssembler);
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
    @ResponseBody
    @ResourceAccess(description = "Return the document of passed URN_COLLECTION.")
    public ResponseEntity<Resource<Document>> getDocument(@PathVariable("urn") UniformResourceName pUrn)
            throws SearchException {
        Document document = indexerService.get(pUrn);
        Resource<Document> resource = toResource(document);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Perform an OpenSearch request on documents.
     *
     * @param pQ
     *            the OpenSearch-format query
     * @param pSort
     *            the sort
     * @param pPageable
     *            the page
     * @param pAssembler
     *            injected by Spring
     * @return the page of documents matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @RequestMapping(path = "/documents/search", method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Perform an OpenSearch request on document.")
    public ResponseEntity<PagedResources<Resource<Document>>> searchDocuments(@RequestParam("q") String pQ,
            @RequestParam("sort") Sort pSort, final Pageable pPageable,
            final PagedResourcesAssembler<Document> pAssembler) throws SearchException {
        return doSearch(pQ, SearchType.DOCUMENT, null, pSort, pPageable, pAssembler);
    }

    /**
     * Perform an OpenSearch request on a type.
     *
     * @param pQ
     *            the OpenSearch-format query
     * @param pSearchType
     *            the indexed type on which we perform the search (not necessary the type returned!)
     * @param pFacets
     *            the facets applicable
     * @param pSort
     *            the sort
     * @param pPageable
     *            the page
     * @param pAssembler
     *            injected by Spring
     * @return the page of elements matching the query
     * @throws SearchException
     *             when an error occurs while parsing the query
     */
    @SuppressWarnings({ "unchecked" })
    public <T extends IIndexable> ResponseEntity<PagedResources<Resource<T>>> doSearch(String pQ,
            SearchType pSearchType, List<String> pFacets, Sort pSort, final Pageable pPageable,
            final PagedResourcesAssembler<T> pAssembler) throws SearchException {
        try {

            Class<T> resultClass = (Class<T>) TO_RESULT_CLASS.get(pSearchType);

            // Build criterion from query
            ICriterion criterion;
            criterion = queryParser.parse(pQ);

            // Special case of dataobjects: we might change the return type
            if (SearchType.DATAOBJECT.equals(pSearchType)) {
                // Handle "target" criterion
                Optional<ICriterion> targetCriterion = criterion.accept(new NamedCriterionFinderVisitor("target"));
                if (targetCriterion.isPresent()) {
                    StringMatchCriterion crit = (StringMatchCriterion) targetCriterion.get();
                    resultClass = (Class<T>) TO_RESULT_CLASS.get(SearchType.valueOf(crit.getValue()));
                }

                // Handle "dataset" criterion
                Optional<ICriterion> datasetCriterion = criterion.accept(new NamedCriterionFinderVisitor("dataset"));
                if (datasetCriterion.isPresent()) {
                    criterion = filterPlugin.addFilter(null, criterion);
                    resultClass = (Class<T>) Dataset.class;
                }

                // Handle "dataset" criterion
                Optional<ICriterion> datasetsCriterion = criterion.accept(new NamedCriterionFinderVisitor("datasets"));
                if (datasetsCriterion.isPresent()) {
                    resultClass = (Class<T>) Dataset.class;
                }
            }

            // Apply security filters
            criterion = accessRightFilter.removeGroupFilter(criterion);
            criterion = accessRightFilter.addGroupFilter(criterion);
            criterion = accessRightFilter.addAccessRightsFilter(criterion);

            // Perform the search
            Page<T> entities;
            SearchKey<T> searchKey = new SearchKey<>(runtimeTenantResolver.getTenant(), pSearchType.toString(),
                    resultClass);
            if (!TO_RESULT_CLASS.get(pSearchType).equals(resultClass)) {
                entities = indexerService.searchAndReturnJoinedEntities(searchKey, pPageable.getPageSize(), criterion);
            } else {
                LinkedHashMap<String, Boolean> ascSortMap = null;
                Map<String, FacetType> facetsMap = null; // Use pFacets
                entities = indexerService.search(searchKey, pPageable, criterion, facetsMap, ascSortMap);
            }

            // Format output response
            // entities = converter.convert(entities);

            // Return
            return new ResponseEntity<>(toPagedResources(entities, pAssembler), HttpStatus.OK);
        } catch (QueryNodeException e) {
            throw new SearchException(pQ, e);
        }
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

    /**
     * List the acceptable search types for the {@link CatalogController} endpoints.
     *
     * @author Xavier-Alexandre Brochard
     */
    public enum SearchType {
        ALL("all"), COLLECTION("collection"), DATASET("dataset"), DATAOBJECT("dataobject"), DOCUMENT("document");

        private final String name;

        /**
         * @param pName
         */
        private SearchType(String pName) {
            name = pName;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

    }

}
