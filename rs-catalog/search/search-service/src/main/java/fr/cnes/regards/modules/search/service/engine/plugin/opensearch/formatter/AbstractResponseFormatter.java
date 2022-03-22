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

        setResponseId(searchId);
        setResponseTitle(engineConf.getSearchTitle());
        setResponseDescription(engineConf.getSearchDescription());
        setResponseAuthor(engineConf.getContact(), engineConf.getAttribution());
        setResponseUpdated();
        setResponseLanguage(configuration.getLanguage());

        setResponsePaginationInfos(page);
        setResponseQuery(context, configuration.getUrlsRel());

        setResponseLinks(links);
        setResponseOpenSearchDescription(openSearchDescriptionUrl);
    }

    protected abstract U buildResponse();

    protected abstract void setResponseOpenSearchDescription(String openSearchDescriptionUrl);

    protected abstract void setResponseLanguage(String language);

    protected abstract void setResponseUpdated();

    protected abstract void setResponseAuthor(String contact, String attribution);

    protected abstract void setResponseQuery(SearchContext context, String role);

    protected abstract void setResponseLinks(List<Link> links);

    protected abstract void setResponsePaginationInfos(FacetPage<EntityFeature> page);

    protected abstract void setResponseDescription(String description);

    protected abstract void setResponseTitle(String title);

    protected abstract void setResponseId(String searchId);

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
        setFeatureId(id);
        setFeatureLinks(entityLinks);
        setFeatureTitle(entity.getLabel());
        setFeatureProviderId(entity.getProviderId());

        if (entityLastUpdate.isPresent()) {
            setFeatureUpdated(entityLastUpdate.get());
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

    protected abstract void setFeatureUpdated(OffsetDateTime date);

    protected abstract void setFeatureProviderId(String providerId);

    protected abstract void setFeatureTitle(String title);

    protected abstract void setFeatureLinks(List<Link> entityLinks);

    protected abstract void setFeatureId(UniformResourceName id);

    protected abstract void addToResponse(T entity);
}
