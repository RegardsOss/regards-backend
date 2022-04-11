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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter;

import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.dao.FacetPage;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.Configuration;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.EngineConfiguration;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.ParameterConfiguration;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.IOpenSearchExtension;
import org.springframework.hateoas.Link;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Léo Mieulet
 */
public abstract class AbstractResponseFormatter<T, U> implements IResponseFormatter<U> {

    protected final String token;

    /**
     * List of {@link IOpenSearchExtension} to handle for ATOM format.
     */
    private final List<IOpenSearchExtension> extensions = new ArrayList<>();

    protected T feature;

    protected U response;

    public AbstractResponseFormatter(String token) {
        this.token = token;
    }

    @Override
    public void addExtension(IOpenSearchExtension configuration) {
        extensions.add(configuration);
    }

    @Override
    public void addMetadata(String searchId,
                            EngineConfiguration engineConf,
                            String openSearchDescriptionUrl,
                            SearchContext context,
                            Configuration configuration,
                            FacetPage<EntityFeature> page,
                            List<Link> links) {
        response = buildResponse();

        addResponseId(searchId);
        addResponseTitle(engineConf.getSearchTitle());
        addResponseDescription(engineConf.getSearchDescription());
        addResponseAuthor(engineConf.getContact(), engineConf.getAttribution());
        addResponseUpdated();
        addResponseLanguage(configuration.getLanguage());

        addResponsePaginationInfos(page);
        addResponseQuery(context, configuration.getUrlsRel());

        addResponseLinks(links);
        addResponseOpenSearchDescription(openSearchDescriptionUrl);
    }

    protected abstract U buildResponse();

    protected abstract void addResponseOpenSearchDescription(String openSearchDescriptionUrl);

    protected abstract void addResponseLanguage(String language);

    protected abstract void addResponseUpdated();

    protected abstract void addResponseAuthor(String contact, String attribution);

    protected abstract void addResponseQuery(SearchContext context, String role);

    protected abstract void addResponseLinks(List<Link> links);

    protected abstract void addResponsePaginationInfos(FacetPage<EntityFeature> page);

    protected abstract void addResponseDescription(String description);

    protected abstract void addResponseTitle(String title);

    protected abstract void addResponseId(String searchId);

    @Override
    public void addEntity(EntityFeature entity,
                          Optional<OffsetDateTime> entityLastUpdate,
                          List<ParameterConfiguration> paramConfigurations,
                          List<Link> entityLinks) {
        addToResponse(buildFeature(entity, entityLastUpdate, paramConfigurations, entityLinks));
    }

    public T buildFeature(EntityFeature entity,
                          Optional<OffsetDateTime> entityLastUpdate,
                          List<ParameterConfiguration> paramConfigurations,
                          List<Link> entityLinks) {
        feature = buildFeature();

        UniformResourceName id = entity.getId();
        addFeatureId(id);
        addFeatureLinks(entityLinks);
        addFeatureTitle(entity.getLabel());
        addFeatureProviderId(entity.getProviderId());

        if (entityLastUpdate.isPresent()) {
            addFeatureUpdated(entityLastUpdate.get());
        }
        // Handle extensions
        for (IOpenSearchExtension extension : extensions) {
            if (extension.isActivated()) {
                updateEntityWithExtension(extension, entity, paramConfigurations);
            }
        }
        return feature;
    }

    protected abstract T buildFeature();

    protected abstract void updateEntityWithExtension(IOpenSearchExtension extension,
                                                      EntityFeature entity,
                                                      List<ParameterConfiguration> paramConfigurations);

    protected abstract void addFeatureUpdated(OffsetDateTime date);

    protected abstract void addFeatureProviderId(String providerId);

    protected abstract void addFeatureTitle(String title);

    protected abstract void addFeatureLinks(List<Link> entityLinks);

    protected abstract void addFeatureId(UniformResourceName id);

    protected abstract void addToResponse(T entity);
}
