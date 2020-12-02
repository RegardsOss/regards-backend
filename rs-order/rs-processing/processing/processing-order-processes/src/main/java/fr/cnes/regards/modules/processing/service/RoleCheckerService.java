/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.domain.service.IRoleCheckerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static fr.cnes.regards.framework.security.utils.HttpConstants.BEARER;

/**
 * This class is the implementation for {@link IRoleCheckerService} in the context or rs-order.
 *
 * @author gandrieu
 */
@Service
public class RoleCheckerService implements IRoleCheckerService {

    private final IRolesClient rolesClient;

    @Autowired
    public RoleCheckerService(IRolesClient rolesClient) {
        this.rolesClient = rolesClient;
    }

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
            }
            catch(Exception e) {
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
