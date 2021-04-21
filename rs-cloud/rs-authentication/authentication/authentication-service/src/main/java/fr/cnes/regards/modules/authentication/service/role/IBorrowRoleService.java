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

import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;

import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;

/**
 * @author Sylvain Vissiere-Guerinet
 */
public interface IBorrowRoleService {

    /**
     * generate a new JWT for the given role if the current user can switch to this role
     * @return couple (new JWT, Role name wanted)
     */
    DefaultOAuth2AccessToken switchTo(String pTargetRoleName) throws JwtException, EntityOperationForbiddenException;

}
