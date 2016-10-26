/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.filter;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletResponse;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 *
 * Class CorsFilterTest
 *
 * Cors filter test class
 *
 * @author sbinda
 * @since 1.0-SNAPSHOT
 */
public class CorsFilterTest {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(CorsFilterTest.class);

    /**
     *
     * Check security filter with cors requests
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_030")
    @Purpose("Check security filter with cors requests")
    @Test
    public void corsFilterTest() {

        final HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse mockedResponse = new MockHttpServletResponse();

        final CorsFilter filter = new CorsFilter();

        final String errorMessage = "Error creating response cors header";

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());

            Assert.assertTrue(errorMessage, mockedResponse.getHeader(CorsFilter.ALLOW_ORIGIN).equals("*"));

            Assert.assertTrue(errorMessage, mockedResponse.getHeader(CorsFilter.ALLOW_METHOD)
                    .equals("POST, PUT, GET, OPTIONS, DELETE"));

            Assert.assertTrue(errorMessage,
                              mockedResponse.getHeader(CorsFilter.ALLOW_HEADER).equals("authorization, content-type"));

            Assert.assertTrue(errorMessage, mockedResponse.getHeader(CorsFilter.CONTROL_MAX_AGE).equals("3600"));
        } catch (ServletException | IOException e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

}
