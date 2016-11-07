/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.hateoas;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * Test class for {@link HateoasUtils}.
 *
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

        final Resource<Object> expected = new Resource<>(input, link);

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

        final List<Resource<Object>> expected = new ArrayList<>();
        expected.add(new Resource<Object>(object));

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

        final Resource<Object> input = new Resource<>(expected);

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

        final List<Resource<Object>> actual = expected.stream().map(o -> new Resource<>(o))
                .collect(Collectors.toList());

        Assert.assertEquals(expected, HateoasUtils.unwrapList(actual));
    }

}
