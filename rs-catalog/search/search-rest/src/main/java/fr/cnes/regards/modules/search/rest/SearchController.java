/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.crawler.domain.criterion.ICriterion;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.Collection;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.Document;
import fr.cnes.regards.modules.search.service.IIndexService;
import fr.cnes.regards.modules.search.service.IRepresentation;
import fr.cnes.regards.modules.search.service.accessright.IAccessRightFilter;
import fr.cnes.regards.modules.search.service.filter.IFilterPlugin;

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
@RequestMapping("/search")
public class SearchController {

    // @Autowired
    // private IResourceService resourceService;

    @Autowired
    private IFilterPlugin filterPlugin;

    @Autowired
    private IAccessRightFilter accessRightFilter;

    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Searches on indexed data.")
    public <T extends AbstractEntity> ResponseEntity<PagedResources<Resource<T>>> search(@PathVariable final String pQ,
            final Pageable pPageable, final PagedResourcesAssembler<T> pAssembler) throws QueryNodeException {

        // Apply project filters
        ICriterion criterion = filterPlugin.getFilters(pQ);

        // Apply security filters
        // criterion = accessRightFilterService.addGroupFilter();
        // criterion = accessRightFilterService.addAccessRightsFilter();

        // Perform the search
        // final Page<T> entities = (Page<T>) indexService.search(pClass, pPageable, criterion);

        // Format output response

        // Return
        // return new ResponseEntity<>(toPagedResources(entities, pAssembler), HttpStatus.OK);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // @Override
    // public Resource<T> toResource(final T pElement, final Object... pExtras) {
    // // TODO add hateoas links
    // return resourceService.toResource(pElement);
    // }
}
