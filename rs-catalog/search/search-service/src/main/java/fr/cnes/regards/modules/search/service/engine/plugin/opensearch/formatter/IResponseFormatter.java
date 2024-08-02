/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter;

import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.Configuration;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.EngineConfiguration;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.ParameterConfiguration;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.IOpenSearchExtension;
import org.springframework.hateoas.Link;

import java.util.List;

/**
 * Interface to define OpenSearch response formatters. Formatters are made for serialized Feature and descriptor in ATOM or JSON format.
 *
 * @param <R> Search Output result format.
 * @author Sébastien Binda
 * @author Léo Mieulet
 */
public interface IResponseFormatter<R> {

    /**
     * Add metadatas to the global search results collection.
     *
     * @param searchId                 {@link String}
     * @param engineConf               {@link EngineConfiguration}
     * @param openSearchDescriptionUrl {@link String}
     * @param context                  {@link SearchContext}
     * @param page                     {@link FacetPage} results of the search
     * @param links                    {@link Link}s of the entities collection
     */
    void addMetadata(String searchId,
                     EngineConfiguration engineConf,
                     String openSearchDescriptionUrl,
                     SearchContext context,
                     Configuration configuration,
                     FacetPage<AbstractEntity<EntityFeature>> page,
                     List<Link> links);

    /**
     * Add a new response entity to the builder. An entity is a {@link AbstractEntity} from an catalog search response.
     *
     * @param entity              {@link AbstractEntity}
     * @param paramConfigurations {@link ParameterConfiguration}s
     */
    void addEntity(AbstractEntity<EntityFeature> entity,
                   List<ParameterConfiguration> paramConfigurations,
                   List<Link> entityLinks);

    /**
     * Clear all added {@link AbstractEntity}s to the current builder.
     */
    void clear();

    /**
     * Build the oupout search results collection.
     *
     * @return Search Output result formatted
     */
    R build();

    /**
     * Add a new {@link IOpenSearchExtension} to the current builder.
     *
     * @param extension {@link IOpenSearchExtension}
     */
    void addExtension(IOpenSearchExtension extension);

}
