/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import java.util.LinkedHashMap;
import java.util.List;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.crawler.domain.SearchKey;
import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;
import fr.cnes.regards.modules.crawler.service.IIndexerService;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.Document;
import fr.cnes.regards.modules.search.service.accessright.IAccessRightFilter;
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
 * @param <T>
 */
@RestController
@ModuleInfo(name = "search", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping("")
public class SearchController<T extends AbstractEntity> implements IResourceController<T> {

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
     * Perform a search in the catalog.
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
     * @throws QueryNodeException
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Searches on indexed data.")
    public ResponseEntity<PagedResources<Resource<T>>> search(@RequestParam("q") String pQ,
            @RequestParam("searchType") String pSearchType, @RequestParam("resultClass") Class<T> pResultClass,
            @RequestParam("facets") List<String> pFacets,
            @RequestParam("sort") LinkedHashMap<String, Boolean> pAscSortMap, final Pageable pPageable,
            final PagedResourcesAssembler<T> pAssembler) throws QueryNodeException {
        // Build criterion from query
        ICriterion criterion = queryParser.parse(pQ);

        // Apply project filters
        // criterion = filterPlugin.addFilter(pRequest, criterion);

        // Apply security filters
        criterion = accessRightFilter.addGroupFilter(criterion);
        criterion = accessRightFilter.addAccessRightsFilter(criterion);

        // Perform the search
        // TODO: Handle facets instead of null
        SearchKey<T> searchKey = new SearchKey<>(runtimeTenantResolver.getTenant(), pSearchType, pResultClass);
        Page<T> entities = indexerService.search(searchKey, pPageable, criterion, null, pAscSortMap);

        // Format output response

        // Return
        return new ResponseEntity<>(toPagedResources(entities, pAssembler), HttpStatus.OK);
    }

    /**
     * Perform a search in the catalog.<br>
     * The only difference with the other search endpoint is that the search type is in the path.
     *
     * @param pSearchType
     *            the type of document to search. Must not be null
     * @param pQ
     *            the query in OpenSearch format
     * @param pResultClass
     *            the class of result document search. In case search type is null, this class must be compatible with
     *            all sorts of result objects (ie AbstractEntity for Regards entity types)
     * @param pFacets
     *            the applicable facets as a list of attribute names
     * @param pAcsSortMap
     *            the sort map
     * @param pPageable
     *            the pagination information
     * @param pAssembler
     *            spring resources assembler
     * @return
     * @throws QueryNodeException
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Searches on indexed data.")
    public ResponseEntity<PagedResources<Resource<T>>> searchWithTypeInPath(
            @PathVariable("searchType") String pSearchType, @RequestParam("q") String pQ,
            @RequestParam("resultClass") Class<T> pResultClass, @RequestParam("facets") List<String> pFacets,
            @RequestParam("sort") LinkedHashMap<String, Boolean> pAcsSortMap, final Pageable pPageable,
            final PagedResourcesAssembler<T> pAssembler) throws QueryNodeException {
        return search(pQ, pSearchType, pResultClass, pFacets, pAcsSortMap, pPageable, pAssembler);
    }

    @Override
    public Resource<T> toResource(T pElement, Object... pExtras) {
        // TODO: Add links
        return resourceService.toResource(pElement);
    }

}
