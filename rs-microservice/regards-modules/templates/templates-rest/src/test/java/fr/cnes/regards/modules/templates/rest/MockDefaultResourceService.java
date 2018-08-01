/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.templates.rest;

import java.lang.reflect.Method;
import java.net.URI;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.core.AnnotationMappingDiscoverer;
import org.springframework.hateoas.core.MappingDiscoverer;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriTemplate;

import fr.cnes.regards.framework.hateoas.DefaultResourceService;

/**
 *
 * Mock default resource service to avoid bad servlet context issue *
 *
 * @author msordi
 *
 */
public class MockDefaultResourceService extends DefaultResourceService {

    /**
     * Mapping discoverer
     */
    private static final MappingDiscoverer DISCOVERER = new AnnotationMappingDiscoverer(RequestMapping.class);

    public MockDefaultResourceService(AccessDecisionManager pAccessDecisionManager) {
        super(pAccessDecisionManager);
    }

    @Override
    protected Link buildLink(final Method pMethod, final String pRel, final Object... pParameterValues) {

        Assert.notNull(pMethod, "Method must not be null!");

        final UriTemplate template = new UriTemplate(DISCOVERER.getMapping(pMethod.getDeclaringClass(), pMethod));
        final URI uri = template.expand(pParameterValues);

        return new Link(uri.toString(), pRel);
    }
}
