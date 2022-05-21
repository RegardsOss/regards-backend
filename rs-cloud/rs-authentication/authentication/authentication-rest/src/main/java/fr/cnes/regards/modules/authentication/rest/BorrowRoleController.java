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
package fr.cnes.regards.modules.authentication.rest;

import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.authentication.service.role.IBorrowRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.web.bind.annotation.*;

/**
 * Controller handling the role borrowing feature
 *
 * @author Sylvain Vissiere-Guerinet
 */
@RestController
@RequestMapping(path = BorrowRoleController.PATH_BORROW_ROLE)
public class BorrowRoleController {

    /**
     * Controller base path
     */
    public static final String PATH_BORROW_ROLE = "/borrowRole";

    /**
     * Controller sub path to change role
     */
    public static final String PATH_BORROW_ROLE_TARGET = "/{target_name}";

    /**
     * {@link IBorrowRoleService} instance
     */
    @Autowired
    private IBorrowRoleService borrowRoleService;

    /**
     * Allows to switch role
     *
     * @return information to switch role
     */
    @ResponseBody
    @ResourceAccess(role = DefaultRole.PUBLIC, description = "endpoint allowing to switch role")
    @RequestMapping(method = RequestMethod.GET, path = PATH_BORROW_ROLE_TARGET)
    public ResponseEntity<DefaultOAuth2AccessToken> switchRole(@PathVariable("target_name") String targetRoleName)
        throws EntityOperationForbiddenException, JwtException {
        return new ResponseEntity<>(borrowRoleService.switchTo(targetRoleName), HttpStatus.OK);

    }
}
