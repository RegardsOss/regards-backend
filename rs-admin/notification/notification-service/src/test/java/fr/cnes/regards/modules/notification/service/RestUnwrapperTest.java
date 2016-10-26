/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.notification.service.utils.RestUnwrapper;

/**
 * Test class for {@link RestUnwrapper}.
 *
 * @author xbrochar
 */
public class RestUnwrapperTest {

    /**
     * Check that the unwrapping method correctly unwraps objects.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the unwrapping method correctly unwraps objects.")
    public void unwrap() {
        final Object expected = new Object();

        final HttpEntity<Resource<Object>> input = new HttpEntity<>(new Resource<>(expected));

        Assert.assertThat(expected, CoreMatchers.is(CoreMatchers.equalTo(RestUnwrapper.unwrap(input))));
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

        Assert.assertThat(expected, CoreMatchers.is(CoreMatchers.equalTo(RestUnwrapper.unwrapList(input))));
    }

}
