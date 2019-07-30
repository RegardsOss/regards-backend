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
package fr.cnes.regards.framework.security.endpoint.voter;

import java.util.Collection;

import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;

import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 * REGARDS endpoint security voter to manage resource access dynamically at method level.
 *
 * @author msordi
 */
public class ResourceAccessVoter implements AccessDecisionVoter<Object> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ResourceAccessVoter.class);

    /**
     * Method authorization service
     */
    private final MethodAuthorizationService methodAuthService;

    /**
     * Constructor
     * @param methodAuthService the method authoization service
     */
    public ResourceAccessVoter(MethodAuthorizationService methodAuthService) {
        this.methodAuthService = methodAuthService;
    }

    @Override
    public boolean supports(ConfigAttribute pAttribute) {
        return true;
    }

    /**
     * This implementation supports any type of class, because it does not query the presented secure object.
     * @param clazz the secure object
     * @return always <code>true</code>
     */
    @Override
    public boolean supports(Class<?> clazz) {
        return true;
    }

    @Override
    public int vote(Authentication authentication, Object object, Collection<ConfigAttribute> attributes) {

        // Default behavior : deny access
        int access = ACCESS_DENIED;

        JWTAuthentication jwtAuth = (JWTAuthentication) authentication;

        if (object instanceof MethodInvocation) {

            MethodInvocation mi = (MethodInvocation) object;
            if (methodAuthService.hasAccess(jwtAuth, mi.getMethod())) {
                access = ACCESS_GRANTED;
            } else {
                access = ACCESS_DENIED;
            }
        }

        // CHECKSTYLE:OFF
        String decision = access == ACCESS_GRANTED ? "granted" : "denied";
        // CHECKSTYLE:ON
        LOG.debug("Access {} for user {}.", decision, jwtAuth.getName());

        return access;
    }
}
