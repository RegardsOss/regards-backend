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
package fr.cnes.regards.modules.authentication.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.authentication.service.role.CoupleJwtRole;
import fr.cnes.regards.modules.authentication.service.role.IBorrowRoleService;

/**
 *
 * Controller handling the role borrowing feature
 *
 * @author Sylvain Vissiere-Guerinet
 *
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
     * @param pTargetRoleName
     * @return information to switch role
     * @throws EntityOperationForbiddenException
     * @throws JwtException
     */
    @ResponseBody
    @ResourceAccess(role = DefaultRole.PUBLIC, description = "endpoint allowing to switch role")
    @RequestMapping(method = RequestMethod.GET, path = PATH_BORROW_ROLE_TARGET)
    public ResponseEntity<CoupleJwtRole> switchRole(@PathVariable("target_name") String pTargetRoleName)
            throws EntityOperationForbiddenException, JwtException {
        CoupleJwtRole newToken = borrowRoleService.switchTo(pTargetRoleName);
        return new ResponseEntity<>(newToken, HttpStatus.OK);

    }
}
