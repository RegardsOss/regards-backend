/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.modules.processing.domain.PBatch;
import fr.cnes.regards.modules.processing.domain.PUserAuth;
import fr.cnes.regards.modules.processing.domain.service.IPUserAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
/**
 * This class is the implementation for the {@link IPUserAuthService} interface.
 *
 * @author gandrieu
 */
@Component
public class PUserAuthServiceImpl implements IPUserAuthService {

    private final FeignSecurityManager feignSecurityManager;

    @Autowired
    public PUserAuthServiceImpl(FeignSecurityManager feignSecurityManager) {
        this.feignSecurityManager = feignSecurityManager;
    }

    @Override public PUserAuth authFromUserEmailAndRole(String tenant, String email, String role) {
        try {
            FeignSecurityManager.asUser(email, role);
            String jwtToken = feignSecurityManager.getToken();
            return new PUserAuth(tenant, email, role, jwtToken);
        }
        finally {
            FeignSecurityManager.reset();
        }
    }

    @Override public PUserAuth authFromBatch(PBatch batch) {
        return authFromUserEmailAndRole(batch.getTenant(), batch.getUser(), batch.getUserRole());
    }

    @Override public PUserAuth fromContext(SecurityContext ctx) {
        JWTAuthentication authentication = (JWTAuthentication) ctx.getAuthentication();
        UserDetails user = authentication.getUser();
        return new PUserAuth(authentication.getTenant(), user.getEmail(), user.getRole(), authentication.getJwt());
    }
}
