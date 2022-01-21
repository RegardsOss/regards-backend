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

package fr.cnes.regards.modules.processing.service;

import static fr.cnes.regards.framework.security.utils.HttpConstants.BEARER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.domain.service.IRoleCheckerService;
import reactor.core.publisher.Mono;

/**
 * This class is the implementation for {@link IRoleCheckerService} in the context or rs-order.
 *
 * @author gandrieu
 */
@Service
@MultitenantTransactional
public class RoleCheckerService implements IRoleCheckerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoleCheckerService.class);

    private final IRolesClient rolesClient;

    @Autowired
    public RoleCheckerService(IRolesClient rolesClient) {
        this.rolesClient = rolesClient;
    }

    @Override
    public Mono<Boolean> roleIsUnder(PUserAuth auth, String role) {
        return Mono.defer(() -> {
            try {
                FeignSecurityManager.asUser(auth.getEmail(), role);
                ResponseEntity<Boolean> resp = rolesClient.shouldAccessToResourceRequiring(role);
                if (resp.getStatusCode().is2xxSuccessful()) {
                    return Mono.just(resp.getBody());
                } else {
                    return Mono.error(new RoleCanNotBeCheckedException());
                }
            } catch (EntityNotFoundException | RuntimeException e) {
                LOGGER.debug("Unable check access role", e);
                return Mono.error(e);
            }
        });
    }

    private String authHeader(PUserAuth auth) {
        return BEARER + ": " + auth.getAuthToken();
    }

    public static class RoleCanNotBeCheckedException extends Exception {

    }
}
