/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.authentication.service.role;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * @author Sylvain Vissiere-Guerinet
 */
public class BorrowRoleServiceTest {

    private IBorrowRoleService borrowRoleService;

    private JWTService JwtService;

    private IRolesClient mockedRoleClient;

    private Role rolePublic;

    private Role roleRegisteredUser;

    private Role roleAdmin;

    private Role roleProjectAdmin;

    @Before
    public void init() {
        rolePublic = new Role(DefaultRole.PUBLIC.toString(), null);
        rolePublic.setNative(true);
        roleRegisteredUser = new Role(DefaultRole.REGISTERED_USER.toString(), rolePublic);
        roleRegisteredUser.setNative(true);
        roleAdmin = new Role(DefaultRole.ADMIN.toString(), roleRegisteredUser);
        roleAdmin.setNative(true);
        roleProjectAdmin = new Role(DefaultRole.PROJECT_ADMIN.toString(), null);
        roleProjectAdmin.setNative(true);
        JwtService = new JWTService();
        JwtService.setSecret("11111111111111111111111111111111111111111111111111111111111111111111111111");
        mockedRoleClient = Mockito.mock(IRolesClient.class);
        borrowRoleService = new BorrowRoleService(mockedRoleClient, JwtService);
    }

    @Test
    @Requirement("PM003") // FIXME
    @Purpose("Checks that a new token is generated when the required role is borrowable")
    public void testSwitchToRole() throws JwtException, EntityOperationForbiddenException {
        // mock the role client answer
        List<Role> borrowableRolesForAdmin = Lists.newArrayList(roleAdmin, roleRegisteredUser, rolePublic);
        ResponseEntity<List<EntityModel<Role>>> mockedResponse = new ResponseEntity<>(
                HateoasUtils.wrapList(borrowableRolesForAdmin), HttpStatus.OK);
        Mockito.when(mockedRoleClient.getBorrowableRoles()).thenReturn(mockedResponse);
        // mock JWTAuthentication
        JwtService.injectToken("test", "ADMIN", "test@test.test", "test@test.test");

        DefaultOAuth2AccessToken newToken = borrowRoleService.switchTo(DefaultRole.PUBLIC.toString());

        JWTAuthentication newAuthentication = new JWTAuthentication(newToken.getValue());
        newAuthentication = JwtService.parseToken(newAuthentication);
        Assert.assertNotNull(newToken);
        Assert.assertEquals("test@test.test", newAuthentication.getName());
        Assert.assertEquals("test", newAuthentication.getTenant());
        Assert.assertEquals(DefaultRole.PUBLIC.toString(), newAuthentication.getUser().getRole());
    }

    @Test(expected = EntityOperationForbiddenException.class)
    @Requirement("PM003") // FIXME
    @Purpose("Checks that a new token is generated only when the required role is borrowable")
    public void testSwitchToRoleUnborrowable() throws JwtException, EntityOperationForbiddenException {
        // mock the role client answer
        List<Role> borrowableRolesForAdmin = Lists.newArrayList();
        ResponseEntity<List<EntityModel<Role>>> mockedResponse = new ResponseEntity<>(
                HateoasUtils.wrapList(borrowableRolesForAdmin), HttpStatus.OK);
        Mockito.when(mockedRoleClient.getBorrowableRoles()).thenReturn(mockedResponse);
        // mock JWTAuthentication
        JwtService.injectToken("test", DefaultRole.PUBLIC.toString(), "test@test.test", "test@test.test");

        borrowRoleService.switchTo(DefaultRole.ADMIN.toString());
        Assert.fail("Exception should have been thrown before here");
    }

}
