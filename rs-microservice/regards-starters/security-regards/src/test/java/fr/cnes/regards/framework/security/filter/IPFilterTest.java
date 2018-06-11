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
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.framework.security.domain.SecurityException;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 *
 * Class IPFilterTest
 *
 * IP Filter tests
 *
 * @author sbinda
 * @since 1.0-SNAPSHOT
 */
public class IPFilterTest {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(IPFilterTest.class);

    /**
     * Test authorized address
     */
    private static final String AUTHORIZED_ADRESS = "127.0.0.1";

    /**
     * Test authorized address
     */
    private static final String AUTHORIZED_ADRESS_PATTERN = "127.0.0.*";

    /**
     * Test user Role
     */
    private static final String ROLE_NAME = "USER";

    /**
     * Tenant test
     */
    private static final String TENANT_NAME = "tenant";

    /**
     *
     * Check security filter with ip adress for endpoints accesses
     *
     * @throws SecurityException
     *             test error
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Check security filter with ip adress for endpoints accesses")
    @Test
    public void ipFilterTest() throws SecurityException {

        final HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse mockedResponse = new MockHttpServletResponse();

        final JWTAuthentication token = new JWTAuthentication("token");
        final UserDetails user = new UserDetails();
        user.setTenant(TENANT_NAME);
        token.setUser(user);
        token.setRole(ROLE_NAME);

        SecurityContextHolder.getContext().setAuthentication(token);

        final List<String> results = new ArrayList<>();
        results.add(AUTHORIZED_ADRESS);

        final RoleAuthority roleAuth = new RoleAuthority(ROLE_NAME);
        roleAuth.setAuthorizedIpAdresses(results);

        final MethodAuthorizationService service = Mockito.mock(MethodAuthorizationService.class);
        Mockito.when(service.getRoleAuthority(ROLE_NAME, TENANT_NAME)).thenReturn(Optional.of(roleAuth));

        final IpFilter filter = new IpFilter(service);

        Mockito.when(mockedRequest.getRemoteAddr()).thenReturn(AUTHORIZED_ADRESS);

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());
            if (mockedResponse.getStatus() != HttpStatus.OK.value()) {
                Assert.fail("Access should be granted for the given address");
            }
        } catch (IOException | ServletException e) {

            Assert.fail(e.getMessage());
        }

        Mockito.when(mockedRequest.getRemoteAddr()).thenReturn("127.0.0.2");

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());
            if (mockedResponse.getStatus() == HttpStatus.OK.value()) {
                Assert.fail("There should be an error. Address not authorized");
            }
        } catch (final InsufficientAuthenticationException e) {
            LOG.info(e.getMessage());
        } catch (IOException | ServletException e) {
            Assert.fail(e.getMessage());
        }

    }

    /**
     *
     * Check security filter with ip adress for endpoints accesses
     *
     * @throws SecurityException
     *             test error
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Check security filter with subdomain ip adress for endpoints accesses")
    @Test
    public void subdomainIpFilterTest() throws SecurityException {

        final HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse mockedResponse = new MockHttpServletResponse();

        final JWTAuthentication token = new JWTAuthentication("token");
        final UserDetails user = new UserDetails();
        user.setTenant(TENANT_NAME);
        token.setUser(user);
        token.setRole(ROLE_NAME);
        SecurityContextHolder.getContext().setAuthentication(token);

        final List<String> results = new ArrayList<>();
        results.add(AUTHORIZED_ADRESS_PATTERN);

        final RoleAuthority roleAuth = new RoleAuthority(ROLE_NAME);
        roleAuth.setAuthorizedIpAdresses(results);

        final MethodAuthorizationService service = Mockito.mock(MethodAuthorizationService.class);
        Mockito.when(service.getRoleAuthority(ROLE_NAME, TENANT_NAME)).thenReturn(Optional.of(roleAuth));
        final IpFilter filter = new IpFilter(service);

        Mockito.when(mockedRequest.getRemoteAddr()).thenReturn(AUTHORIZED_ADRESS);

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());
            if (mockedResponse.getStatus() != HttpStatus.OK.value()) {
                Assert.fail("Access should be granted for the given address");
            }
        } catch (IOException | ServletException e) {
            Assert.fail(e.getMessage());
        }

        Mockito.when(mockedRequest.getRemoteAddr()).thenReturn("127.0.1.1");

        try {
            filter.doFilter(mockedRequest, mockedResponse, new MockFilterChain());
            if (mockedResponse.getStatus() == HttpStatus.OK.value()) {
                Assert.fail("There should be an error. Address not authorized");
            }
        } catch (final InsufficientAuthenticationException e) {
            LOG.info(e.getMessage());
        } catch (IOException | ServletException e) {
            Assert.fail(e.getMessage());
        }

    }

}
