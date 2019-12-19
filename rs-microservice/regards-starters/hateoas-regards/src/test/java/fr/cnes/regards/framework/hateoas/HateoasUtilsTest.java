/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.hateoas;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * Test class for {@link HateoasUtils}.
 * @author xbrochar
 */
public class HateoasUtilsTest {

    /**
     * A dummy href for links
     */
    private static final String HREF = "href";

    /**
     * Check that the wrapping method correctly wraps objects.
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check that the wrapping method correctly wraps objects.")
    public void wrap() {
        final Object input = new Object();
        final Link link = new Link(HREF);

        final EntityModel<Object> expected = new EntityModel<>(input, link);

        Assert.assertEquals(expected, HateoasUtils.wrap(input, link));
    }

    /**
     * Check that the wrapping method correctly wraps lists of objects.
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check that the wrapping method correctly wraps lists of objects.")
    public void wrapList() {
        final Object object = new Object();
        final List<Object> input = new ArrayList<>();
        input.add(object);

        final List<EntityModel<Object>> expected = new ArrayList<>();
        expected.add(new EntityModel<>(object));

        Assert.assertEquals(expected, HateoasUtils.wrapList(input));
    }

    /**
     * Check that the unwrapping method correctly unwraps objects.
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check that the unwrapping method correctly unwraps objects.")
    public void unwrap() {
        final Object expected = new Object();

        final EntityModel<Object> input = new EntityModel<>(expected);

        Assert.assertEquals(expected, HateoasUtils.unwrap(input));
    }

    /**
     * Check that the unwrapping method correctly unwraps objects lists.
     */
    @Test
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check that the unwrapping method correctly unwraps objects lists.")
    public void unwrapList() {
        final Object o0 = new Object();
        final Object o1 = new Object();

        final List<Object> expected = new ArrayList<>();
        expected.add(o0);
        expected.add(o1);

        final List<EntityModel<Object>> actual = expected.stream().map(o -> new EntityModel<>(o))
                .collect(Collectors.toList());

        Assert.assertEquals(expected, HateoasUtils.unwrapList(actual));
    }

    /**
     * Test util to retrieve all elements form a paginated endpoints with Hateoas resources
     * @since 1.0-SNAPHSOT
     */
    @Test
    public void retrieveAllEnttitiesFromPaginatedEndpoint() {

        // First with total elements = 20 and 5 elements per page
        List<String> allResults = HateoasUtils.retrieveAllPages(5, (final Pageable pPageable) -> {
            final List<String> entities = new ArrayList<>();

            for (int i = 0; i < pPageable.getPageSize(); i++) {
                entities.add("value_" + String.valueOf(pPageable.getOffset() + i));
            }

            final PageMetadata md = new PageMetadata(pPageable.getPageSize(), pPageable.getPageNumber(), 20);
            final PagedModel<EntityModel<String>> resources = new PagedModel<>(HateoasUtils.wrapList(entities), md,
                    new ArrayList<>());
            return ResponseEntity.ok(resources);
        });

        Assert.assertNotNull(allResults);
        Assert.assertEquals(20, allResults.size());
        Assert.assertEquals("value_0", allResults.get(0));
        Assert.assertEquals("value_1", allResults.get(1));

        // Second with no elements in results
        allResults = HateoasUtils.retrieveAllPages(5, (final Pageable pPageable) -> {
            final List<String> entities = new ArrayList<>();
            final PageMetadata md = new PageMetadata(pPageable.getPageSize(), pPageable.getPageNumber(), 0);
            final PagedModel<EntityModel<String>> resources = new PagedModel<>(HateoasUtils.wrapList(entities), md,
                    new ArrayList<>());
            return ResponseEntity.ok(resources);
        });

        Assert.assertNotNull(allResults);
        Assert.assertEquals(0, allResults.size());

        // Third with with 1 elements in results and 5 per page
        allResults = HateoasUtils.retrieveAllPages(5, (final Pageable pPageable) -> {
            final List<String> entities = new ArrayList<>();
            entities.add("value_" + String.valueOf(pPageable.getOffset()));
            final PageMetadata md = new PageMetadata(pPageable.getPageSize(), pPageable.getPageNumber(), 1);
            final PagedModel<EntityModel<String>> resources = new PagedModel<>(HateoasUtils.wrapList(entities), md,
                    new ArrayList<>());
            return ResponseEntity.ok(resources);
        });

        Assert.assertNotNull(allResults);
        Assert.assertEquals(1, allResults.size());
        Assert.assertEquals("value_0", allResults.get(0));

    }

}
