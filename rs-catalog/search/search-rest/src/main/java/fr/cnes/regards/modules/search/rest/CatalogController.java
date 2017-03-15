/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.SortDefault;
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

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.SearchException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.crawler.domain.IIndexable;
import fr.cnes.regards.modules.crawler.domain.SearchKey;
import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;
import fr.cnes.regards.modules.crawler.domain.facet.FacetType;
import fr.cnes.regards.modules.crawler.service.IIndexerService;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.Document;
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
    private RegardsQueryParser queryParser;

    /**
     * Applies project filters, i.e. the OpenSearch query
     */
    @Autowired
    private IFilterPlugin filterPlugin;

    /**
     * Adds user group and data access filters
     */
    @Autowired
    private IAccessRightFilter accessRightFilter;

    /**
     * Service perfoming the ElasticSearch search
     */
    @Autowired
    private IIndexerService indexerService;

    /**
     * Converts entities after search
     */
    @Autowired
    private IConverter converter;

    /**
     * Get current tenant at runtime and allows tenant forcing
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * The resource service
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Perform a search in the catalog on given search type.<br>
     * The search returns the same type.
     *
     * @param pQ
     *            the query in OpenSearch format
     * @param pSearchType
     *            the type of document to search. Must not be null
     * @param pResultClass
     *            the class of result document search. In case search type is null, this class must be compatible with
     *            all sorts of result objects (ie AbstractEntity for Regards entity types)
     * @param pFacets
     *            the applicable facets as a list of attribute names
     * @param pAscSortMap
     *            the sort map
     * @param pPageable
     *            the pagination information
     * @param pAssembler
     *            spring resources assembler
     * @return
     * @throws SearchException
     * @throws QueryNodeException
     */

    @RequestMapping(path = "/search", method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(
            description = "Perform an OpenSearch request on all indexed data, regardless of the type. The return objects can be any mix of collections, datasets, dataobjects and documents.")
    public ResponseEntity<PagedResources<Resource<AbstractEntity>>> searchAll(@RequestParam("q") String pQ,
            @RequestParam("facets") List<String> pFacets, @SortDefault("name") Sort pSort, final Pageable pPageable,
            final PagedResourcesAssembler<AbstractEntity> pAssembler) throws SearchException {
        return doSearch(pQ, SearchType.all, AbstractEntity.class, pFacets, pSort, pPageable, pAssembler);
    }

    @RequestMapping(path = "/collections/{urn}", method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Return the collection of passed URN.")
    public ResponseEntity<Resource<Collection>> getCollection(@PathVariable("urn") String pUrn) throws SearchException {
        // TODO
        return null;
    }

    @RequestMapping(path = "/collections/search", method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Perform an OpenSearch request on collections.")
    public ResponseEntity<PagedResources<Resource<Collection>>> searchCollections(@RequestParam("q") String pQ,
            @SortDefault("name") Sort pSort, final Pageable pPageable,
            final PagedResourcesAssembler<Collection> pAssembler) throws SearchException {
        return doSearch(pQ, SearchType.collections, Collection.class, null, pSort, pPageable, pAssembler);
    }

    @RequestMapping(path = "/datasets/{urn}", method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Return the dataset of passed URN.")
    public ResponseEntity<Resource<Dataset>> getDataset(@PathVariable("urn") String pUrn) throws SearchException {
        // TODO
        return null;
    }

    @RequestMapping(path = "/datasets/search", method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Perform an OpenSearch request on datasets.")
    public ResponseEntity<PagedResources<Resource<Dataset>>> searchDatasets(@RequestParam("q") String pQ,
            @SortDefault("name") Sort pSort, final Pageable pPageable,
            final PagedResourcesAssembler<Dataset> pAssembler) throws SearchException {
        return doSearch(pQ, SearchType.datasets, Dataset.class, null, pSort, pPageable, pAssembler);
    }

    @RequestMapping(path = "/dataobjects/{urn}", method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Return the dataobject of passed URN.")
    public ResponseEntity<Resource<DataObject>> getDataobject(@PathVariable("urn") String pUrn) throws SearchException {
        // TODO
        return null;
    }

    @RequestMapping(path = "/dataobjects/search", method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Perform an OpenSearch request on dataobjects. Only return required facets.")
    public ResponseEntity<PagedResources<Resource<DataObject>>> searchDataobjects(@RequestParam("q") String pQ,
            @RequestParam("facets") List<String> pFacets, @SortDefault("name") Sort pSort, final Pageable pPageable,
            final PagedResourcesAssembler<DataObject> pAssembler) throws SearchException {
        return doSearch(pQ, SearchType.dataobjects, DataObject.class, pFacets, pSort, pPageable, pAssembler);
    }

    @RequestMapping(path = "/documents/{urn}", method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Return the document of passed URN.")
    public ResponseEntity<Resource<Document>> getDocument(@PathVariable("urn") String pUrn) throws SearchException {
        // TODO
        return null;
    }

    @RequestMapping(path = "/documents/search", method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Perform an OpenSearch request on documents.")
    public ResponseEntity<PagedResources<Resource<Document>>> searchDocuments(@RequestParam("q") String pQ,
            @SortDefault("name") Sort pSort, final Pageable pPageable,
            final PagedResourcesAssembler<Document> pAssembler) throws SearchException {
        return doSearch(pQ, SearchType.documents, Document.class, null, pSort, pPageable, pAssembler);
    }

    private <T extends IIndexable> ResponseEntity<PagedResources<Resource<T>>> doSearch(String pQ,
            SearchType pSearchType, Class<T> pResultClass, List<String> pFacets, Sort pSort, final Pageable pPageable,
            final PagedResourcesAssembler<T> pAssembler) throws SearchException {
        try {
            Class<T> resultClass = pResultClass;

            // Build criterion from query
            ICriterion criterion;
            criterion = queryParser.parse(pQ);

            // Handle "target" criterion
            Predicate<ICriterion> weHaveATargetCriterion = pCriterion -> false;
            if (weHaveATargetCriterion.test(criterion)) {
                // resultClass = classe de la valeur du criterion target;
            }

            // Handle "dataset" criterion
            Predicate<ICriterion> weHaveADatasetCriterion = pCriterion -> false;
            if (weHaveADatasetCriterion.test(criterion)) {
                criterion = filterPlugin.addFilter(null, criterion);
                // resultClass = classe de la valeur du criterion target;
            }

            // Handle "datasets" criterion
            Predicate<ICriterion> weHaveADatasetsCriterion = pCriterion -> false;
            if (weHaveADatasetsCriterion.test(criterion)) {
                // resultClass = classe de la valeur du criterion target;
            }

            // Apply security filters
            criterion = accessRightFilter.removeGroupFilter(criterion);
            criterion = accessRightFilter.addGroupFilter(criterion);
            criterion = accessRightFilter.addAccessRightsFilter(criterion);

            // Perform the search
            Page<T> entities;
            SearchKey<T> searchKey = new SearchKey<>(runtimeTenantResolver.getTenant(), pSearchType.toString(),
                    resultClass);
            BiPredicate<SearchType, Class<T>> searchTypeAndResultClassAreDifferent = (pST, pRC) -> false;

            if (searchTypeAndResultClassAreDifferent.test(pSearchType, resultClass)) {
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
}
