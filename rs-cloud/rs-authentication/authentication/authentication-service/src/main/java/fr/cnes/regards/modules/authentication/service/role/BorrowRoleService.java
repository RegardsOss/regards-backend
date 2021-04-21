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
package fr.cnes.regards.modules.authentication.service.role;

import java.sql.Date;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.utils.HttpUtils;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@Service
public class BorrowRoleService implements IBorrowRoleService {

    /**
     * {@link IRolesClient} instance
     */
    private final IRolesClient rolesClient;

    /**
     * {@link JWTService} instance
     */
    private final JWTService jwtService;

    /**
     * Constructor setting the parameters as attributes
     */
    public BorrowRoleService(IRolesClient pRolesClient, JWTService pJwtService) {
        super();
        rolesClient = pRolesClient;
        jwtService = pJwtService;
    }

    @Override
    public DefaultOAuth2AccessToken switchTo(String targetRoleName)
            throws JwtException, EntityOperationForbiddenException {
        Set<String> borrowableRoleNames = getBorrowableRoleNames();

        JWTAuthentication currentToken = jwtService.getCurrentToken();
        if (!borrowableRoleNames.contains(targetRoleName)) {
            throw new EntityOperationForbiddenException(
                    String.format("Users of role %s are not allowed to borrow role %s",
                                  currentToken.getUser().getRole(), targetRoleName));
        }
        String name = currentToken.getName();
        String tenant = currentToken.getTenant();
        String email = currentToken.getUser().getEmail();
        DefaultOAuth2AccessToken newToken = new DefaultOAuth2AccessToken(
                jwtService.generateToken(tenant, name, email, targetRoleName));
        newToken.setAdditionalInformation(jwtService.generateClaims(tenant, targetRoleName, name, email));
        newToken.setExpiration(Date.from(jwtService.getExpirationDate(OffsetDateTime.now()).toInstant()));
        //FIXME: refreshToken(jti) is not set here to avoid not analysed behaviour,
        //FIXME: should be fixed by making JWTService the only token source of the application.
        //FIXME: That means overriding DefaultTokenServices.
        return newToken;

    }

    private Set<String> getBorrowableRoleNames() {
        //DO NOT USE FEIGN SECURITY MANAGER HERE: we need to know the user who send the request
        ResponseEntity<List<EntityModel<Role>>> response = rolesClient.getBorrowableRoles();
        final HttpStatus responseStatus = response.getStatusCode();
        if (!HttpUtils.isSuccess(responseStatus)) {
            // if it gets here it's mainly because of 404 so it means entity not found
            return Sets.newHashSet();
        }
        List<Role> roleList = HateoasUtils.unwrapList(response.getBody());
        return roleList.stream().map(Role::getName).collect(Collectors.toSet());
    }

}
