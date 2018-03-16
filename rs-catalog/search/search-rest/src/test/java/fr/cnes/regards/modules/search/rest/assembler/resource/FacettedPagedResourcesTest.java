/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest.assembler.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources.PageMetadata;

import fr.cnes.regards.modules.indexer.domain.facet.IFacet;
import fr.cnes.regards.modules.indexer.domain.facet.StringFacet;
import fr.cnes.regards.modules.search.rest.assembler.resource.FacettedPagedResources;

/**
 * Unit test for {@link FacettedPagedResources}.
 * @author Xavier-Alexandre Brochard
 */
public class FacettedPagedResourcesTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.search.rest.assembler.resource.FacettedPagedResources#hashCode()}.
     */
    @Test
    public final void testHashCode_shouldBeEqual() {
        FacettedPagedResources<Pojo> first = buildDummyEmpty();
        FacettedPagedResources<Pojo> second = buildDummyEmpty();
        Assert.assertEquals(first.hashCode(), second.hashCode());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.search.rest.assembler.resource.FacettedPagedResources#hashCode()}.
     */
    @Test
    public final void testHashCode_shouldBeDifferent() {
        FacettedPagedResources<Pojo> first = buildDummyEmpty();
        FacettedPagedResources<Pojo> second = buildDummyWithFacets();
        Assert.assertNotEquals(first.hashCode(), second.hashCode());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.search.rest.assembler.resource.FacettedPagedResources#getFacets()}.
     */
    @Test
    public final void testGetFacets() {
        FacettedPagedResources<Pojo> dummy = buildDummyWithFacets();
        Assert.assertNotNull(dummy.getFacets());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.search.rest.assembler.resource.FacettedPagedResources#equals(java.lang.Object)}.
     */
    @Test
    public final void testEqualsObject_shouldBeDifferent() {
        FacettedPagedResources<Pojo> first = buildDummyEmpty();
        FacettedPagedResources<Pojo> second = buildDummyWithFacets();
        Assert.assertNotEquals(first, second);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.search.rest.assembler.resource.FacettedPagedResources#equals(java.lang.Object)}.
     */
    @Test
    public final void testEqualsObject_shouldBeEqual() {
        FacettedPagedResources<Pojo> first = buildDummyEmpty();
        FacettedPagedResources<Pojo> second = buildDummyEmpty();
        Assert.assertEquals(first, second);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.search.rest.assembler.resource.FacettedPagedResources#toString()}.
     */
    @Test
    public final void testToString() {
        FacettedPagedResources<Pojo> dummy = buildDummyWithFacets();
        Assert.assertNotNull(dummy.toString());
    }

    /**
     * Simple POJO for testing
     * @author Xavier-Alexandre Brochard
     */
    public class Pojo {

    }

    private final FacettedPagedResources<Pojo> buildDummyEmpty() {
        Set<IFacet<?>> facets = new HashSet<>();
        Collection<Pojo> pContent = new ArrayList<>();
        PageMetadata metadata = new PageMetadata(0, 0, 0);
        Iterable<Link> links = new ArrayList<>();
        return new FacettedPagedResources<>(facets, pContent, metadata, links);
    }

    private final FacettedPagedResources<Pojo> buildDummyWithFacets() {
        Set<IFacet<?>> facets = new HashSet<>();
        facets.add(new StringFacet("toto", null, 10));
        Collection<Pojo> pContent = new ArrayList<>();
        PageMetadata metadata = new PageMetadata(0, 0, 0);
        Iterable<Link> links = new ArrayList<>();
        return new FacettedPagedResources<>(facets, pContent, metadata, links);
    }
}
