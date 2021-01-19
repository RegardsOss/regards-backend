/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.authentication;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.framework.authentication.internal.filter.RoleSysFilter;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * Class RoleSysFilterTest
 *
 * Test filter to deny access to all SYS role users.
 * @author Sébastien Binda
 */
public class RoleSysFilterTest {

    /**
     * Check access to gateway is not denied by the ROLE Filter for all non SYS roles users
     *
     * * @throws ServletException
     * test error
     * @throws IOException test error
     */
    @Purpose("Check access to gateway is not denied by the ROLE Filter for all non SYS roles users")
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Test
    public void testFilter() throws ServletException, IOException {

        final JWTAuthentication jwtAuth = new JWTAuthentication("foo");
        jwtAuth.setUser(new UserDetails("tenant", "test", "test@regards.fr", "role"));
        jwtAuth.setRole("role");
        SecurityContextHolder.getContext().setAuthentication(jwtAuth);

        final RoleSysFilter filter = new RoleSysFilter();
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        final FilterChain chaine = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chaine);

        Mockito.verify(chaine, Mockito.times(1)).doFilter(Mockito.any(), Mockito.any());

    }

    /**
     * Check error during oauth2 authentication process using default authentication plugin
     * @throws ServletException test error
     * @throws IOException      test error
     */
    @Purpose("Check access to gateway is denied for all SYS roles users ")
    @Requirement("REGARDS_DSL_SYS_SEC_100")
    @Test
    public void testFilterAcessDenied() throws ServletException, IOException {

        final JWTAuthentication jwtAuth = new JWTAuthentication("foo");
        jwtAuth.setUser(new UserDetails("tenant", "test", "test@regards.fr", "role"));
        jwtAuth.setRole(RoleAuthority.getSysRole("test"));
        SecurityContextHolder.getContext().setAuthentication(jwtAuth);

        final RoleSysFilter filter = new RoleSysFilter();
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        final HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        final FilterChain chaine = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chaine);

        Mockito.verify(response, Mockito.times(1)).sendError(Mockito.anyInt(), Mockito.anyString());
        Mockito.verify(chaine, Mockito.times(0)).doFilter(Mockito.any(), Mockito.any());

    }

}
