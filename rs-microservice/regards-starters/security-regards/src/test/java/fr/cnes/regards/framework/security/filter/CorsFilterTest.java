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
package fr.cnes.regards.framework.security.filter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletResponse;

import fr.cnes.regards.framework.security.domain.SecurityException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * Class CorsFilterTest
 *
 * Cors filter test class
 * @author Sébastien Binda
 */
public class CorsFilterTest {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(CorsFilterTest.class);

    /**
     * Test user Role
     */
    private static final String ROLE_NAME = "USER";

    /**
     * Tenant test
     */
    private static final String TENANT_NAME = "tenant";

    /**
     * Check security filter with cors requests
     * @throws SecurityException test error
     */
    @Requirement("REGARDS_DSL_SYS_ARC_030")
    @Purpose("Check security filter with cors requests")
    @Test
    public void corsFilterTest() throws SecurityException {

        final HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse mockedResponse = new MockHttpServletResponse();

        Mockito.doReturn("172.27.26.25").when(mockedRequest).getHeader(CorsFilter.REQUEST_HEADER_ORIGIN);

        final List<String> authorizedIp = new ArrayList<>();
        authorizedIp.add("172.27.26.*");
        authorizedIp.add("1.2.3.4");
        final CorsFilter filter = new CorsFilter(authorizedIp);

        final String errorMessage = "Error creating response cors header";

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());

            Assert.assertEquals(errorMessage, "*", mockedResponse.getHeader(CorsFilter.ALLOW_ORIGIN));

            Assert.assertEquals(errorMessage, "POST, PUT, GET, OPTIONS, DELETE",
                                mockedResponse.getHeader(CorsFilter.ALLOW_METHOD));

            Assert.assertEquals(errorMessage, "authorization, content-type, scope",
                                mockedResponse.getHeader(CorsFilter.ALLOW_HEADER));

            Assert.assertEquals(errorMessage, "3600", mockedResponse.getHeader(CorsFilter.CONTROL_MAX_AGE));

        } catch (ServletException | IOException e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Check security filter with cors requests
     * @throws SecurityException test error
     */
    @Requirement("REGARDS_DSL_SYS_ARC_030")
    @Requirement("REGARDS_DSL_SYS_ARC_040")
    @Purpose("Check security filter with cors requests access denied for a given Role")
    @Test
    public void corsFilterAccessDeniedTest() throws SecurityException {

        final HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse mockedResponse = new MockHttpServletResponse();

        Mockito.doReturn("172.27.26.25").when(mockedRequest).getHeader(CorsFilter.REQUEST_HEADER_ORIGIN);

        final List<String> authorizedIp = new ArrayList<>();
        authorizedIp.add("172.25.26.24");
        final CorsFilter filter = new CorsFilter(authorizedIp);

        final String errorMessage = "Error creating response cors header";

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());

            Assert.assertNotEquals(errorMessage, "*", mockedResponse.getHeader(CorsFilter.ALLOW_ORIGIN));

            Assert.assertNotEquals(errorMessage, "POST, PUT, GET, OPTIONS, DELETE",
                                   mockedResponse.getHeader(CorsFilter.ALLOW_METHOD));

            Assert.assertNotEquals(errorMessage, "authorization, content-type, scope",
                                   mockedResponse.getHeader(CorsFilter.ALLOW_HEADER));

            Assert.assertNotEquals(errorMessage, "3600", mockedResponse.getHeader(CorsFilter.CONTROL_MAX_AGE));

        } catch (ServletException | IOException e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

}
