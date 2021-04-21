/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.domain.plugin;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.summary.DocFilesSummary;
import fr.cnes.regards.modules.search.domain.PropertyBound;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Search engine plugin contract<br/>
 *
 * To call business search layer, please use IBusinessSearchService.<br/>
 * To build custom criterion for a given AttributeModel, use IFeatureCriterion<br/>
 * To find an AttributeModel from a parameter name, use IAttributeFinder.
 *
 * @author Marc Sordi
 *
 * @param <R> search result type
 * @param <E> extra result type
 * @param <T> single entity type
 * @param <V> property values
 *
 */
@PluginInterface(
        description = "Plugin to define a search engine. A Searh engine define the format of queries and format of response from a search request.")
public interface ISearchEngine<R, E, T, V extends Collection<?>> {

    /**
     * Check if plugin supports the required search type. A plugin may support several search types.
     */
    boolean supports(SearchType searchType);

    /**
     * Engine search method.<br>
     * <hr/>
     * Search context :
     * <ol>
     * <li>{@link SearchContext} may contain a dataset URN so you have to consider it using {@link Optional#isPresent()}
     * method on {@link SearchContext#getDatasetUrn()} and add it to the search criterions.</li>
     * </ol>
     */
    ResponseEntity<R> search(SearchContext context, ISearchEngine<?, ?, ?, ?> requestParser,
            IEntityLinkBuilder linkBuilder) throws ModuleException;

    /**
     * Parse query parameters and transform to {@link ICriterion} (available for all search method)<br/>
     * Use {@link ICriterion} as criterion builder.<br/>
     * <b>This method implementation is required for subsetting feature.</b>
     * @param context containing all query parameters
     * @return {@link ICriterion}
     */
    ICriterion parse(SearchContext context) throws ModuleException;

    /**
     * Additional extra path handling. Not supported by default<br/>
     * <hr/>
     * Search context :
     * <ol>
     * <li>{@link SearchContext} contains extra path parameter, get it using {@link Optional#get()} on
     * {@link SearchContext#getExtra()}</li>
     * <li>{@link SearchContext} may contain a dataset URN so you have to consider it using {@link Optional#isPresent()}
     * method on {@link SearchContext#getDatasetUrn()} and add it to the search criterions.</li>
     * </ol>
     */
    default ResponseEntity<E> extra(SearchContext context, IEntityLinkBuilder linkBuilder) throws ModuleException {
        throw new UnsupportedOperationException(
                "Additional path handling not implemented for engine " + context.getEngineType());
    }

    /**
     * Tells if the default search links must be embedded in the configuration representation.
     *
     * @return true if the default generation of search links in the configuration can be used
     */
    default boolean useDefaultConfigurationLinks() {
        return true;
    }

    /**
     * Allows the plugin to add its own links in the configuration representation.
     *
     * @return true if the default generation of search links in the configuration can be used
     */
    default List<Link> extraLinks(Class<?> searchEngineControllerClass, SearchEngineConfiguration element) {
        return new ArrayList<>();
    }

    /**
     * Retrieve a single entity from its URN ({@link SearchContext#getUrn()})
     * <hr/>
     * Search context :
     * <ol>
     * <li>{@link SearchContext} contains entity URN path parameter, get it using {@link Optional#get()} on
     * {@link SearchContext#getUrn()}</li>
     * </ol>
     */
    ResponseEntity<T> getEntity(SearchContext context, IEntityLinkBuilder linkBuilder) throws ModuleException;

    /**
     * Get contextual property values<br/>
     * <hr/>
     * Search context :
     * <ol>
     * <li>{@link SearchContext} contains property name path parameter, get it using {@link Optional#get()} on
     * {@link SearchContext#getPropertyNames()}</li>
     * <li>{@link SearchContext} contains max count result, get it using {@link Optional#get()} on
     * {@link SearchContext#getMaxCount()}</li>
     * <li>{@link SearchContext} may contain a dataset URN so you have to consider it using {@link Optional#isPresent()}
     * method on {@link SearchContext#getDatasetUrn()} and add it to the search criterions.</li>
     * </ol>
     */
    default ResponseEntity<V> getPropertyValues(SearchContext context) throws ModuleException {
        throw new UnsupportedOperationException(
                "Retrieving property values not implemented for engine " + context.getEngineType());
    }

    /**
     * Compute a DocFileSummary for current user, for specified opensearch request, for asked file types (see DataType)
     * and eventualy restricted to a given dataset.<br/>
     * <hr/>
     * Search context :
     * <ol>
     * <li>{@link SearchContext} contains data types, get it using {@link Optional#get()} on
     * {@link SearchContext#getDateTypes()}</li>
     * <li>{@link SearchContext} may contain a dataset URN so you have to consider it using {@link Optional#isPresent()}
     * method on {@link SearchContext#getDatasetUrn()} and add it to the search criterions.</li>
     * </ol>
     */
    default ResponseEntity<DocFilesSummary> getSummary(SearchContext context) throws ModuleException {
        throw new UnsupportedOperationException(
                "Computing file summary not implemented for engine " + context.getEngineType());
    }

    /**
     * Get contextual properties bounds.<br/>
     * <hr/>
     * Search context :
     * <ol>
     * <li>{@link SearchContext} contains property names path parameter, get it using {@link Optional#get()} on
     * {@link SearchContext#getPropertyNames()}</li>
     * <li>{@link SearchContext} may contain a dataset URN so you have to consider it using {@link Optional#isPresent()}
     * method on {@link SearchContext#getDatasetUrn()} and add it to the search criterions.</li>
     * </ol>
     * @throws ModuleException
     */
    default ResponseEntity<List<EntityModel<PropertyBound<?>>>> getPropertiesBounds(SearchContext context)
            throws ModuleException {
        throw new UnsupportedOperationException(
                "Bound calculation not implemented for engine " + context.getEngineType());
    }
}
