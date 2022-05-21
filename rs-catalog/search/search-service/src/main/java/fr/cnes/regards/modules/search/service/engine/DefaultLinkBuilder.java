/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.service.engine;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.search.domain.plugin.IEntityLinkBuilder;
import fr.cnes.regards.modules.search.domain.plugin.SearchContext;
import org.apache.commons.compress.utils.Lists;
import org.springframework.data.domain.PageImpl;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author sbinda
 */
@Component
public class DefaultLinkBuilder implements IEntityLinkBuilder {

    @Override
    public List<Link> buildEntityLinks(IResourceService resourceService, SearchContext context, EntityFeature entity) {
        return Lists.newArrayList();
    }

    @Override
    public List<Link> buildEntityLinks(IResourceService resourceService,
                                       SearchContext context,
                                       EntityType entityType,
                                       UniformResourceName id) {
        return Lists.newArrayList();
    }

    @Override
    public Link buildExtraLink(IResourceService resourceService,
                               SearchContext context,
                               LinkRelation rel,
                               String extra) {
        return null;
    }

    @Override
    public Link buildPaginationLink(IResourceService resourceService,
                                    SearchContext context,
                                    int pageSize,
                                    int pageNumber,
                                    LinkRelation rel) {
        return null;
    }

    @Override
    public Link buildPaginationLink(IResourceService resourceService, SearchContext context, LinkRelation rel) {
        return null;
    }

    @Override
    public List<Link> buildPaginationLinks(IResourceService resourceService, PageImpl<?> page, SearchContext context) {
        return null;
    }

}
