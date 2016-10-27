/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.notification.service.utils.RestResponseUtils;

/**
 * Test class for {@link RestResponseUtils}.
 *
 * @author xbrochar
 */
public class RestResponseUtilsTest {

    /**
     * A dummy href for links
     */
    private static final String HREF = "href";

    /**
     * Check that the wrapping method correctly wraps objects.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the wrapping method correctly wraps objects.")
    public void wrap() {
        final Object input = new Object();
        final HttpStatus status = HttpStatus.OK;
        final Link link = new Link(HREF);

        final ResponseEntity<Resource<Object>> expected = new ResponseEntity<>(new Resource<>(input, link), status);

        Assert.assertEquals(expected, RestResponseUtils.wrap(input, status, link));
    }

    /**
     * Check that the wrapping method correctly wraps lists of objects.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the wrapping method correctly wraps lists of objects.")
    public void wrapList() {
        final Object object = new Object();
        final List<Object> input = new ArrayList<>();
        input.add(object);
        final HttpStatus status = HttpStatus.OK;

        final List<Resource<Object>> asListOfResources = new ArrayList<>();
        asListOfResources.add(new Resource<Object>(object));

        final ResponseEntity<List<Resource<Object>>> expected = new ResponseEntity<>(asListOfResources, status);

        Assert.assertEquals(expected, RestResponseUtils.wrapList(input, status));
    }

    /**
     * Check that the unwrapping method correctly unwraps objects.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the unwrapping method correctly unwraps objects.")
    public void unwrap() {
        final Object expected = new Object();

        final HttpEntity<Resource<Object>> input = new HttpEntity<>(new Resource<>(expected));

        Assert.assertEquals(expected, RestResponseUtils.unwrap(input));
    }

    /**
     * Check that the unwrapping method correctly unwraps objects lists.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the unwrapping method correctly unwraps objects lists.")
    public void unwrapList() {
        final Object o0 = new Object();
        final Object o1 = new Object();

        final List<Object> expected = new ArrayList<>();
        expected.add(o0);
        expected.add(o1);

        final List<Resource<Object>> asResources = expected.stream().map(o -> new Resource<>(o))
                .collect(Collectors.toList());

        final HttpEntity<List<Resource<Object>>> input = new HttpEntity<>(asResources);

        Assert.assertEquals(expected, RestResponseUtils.unwrapList(input));
    }

}
