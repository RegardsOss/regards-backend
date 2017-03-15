/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.role.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.authentication.role.service.CoupleJwtRole;
import fr.cnes.regards.framework.authentication.role.service.IBorrowRoleService;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;

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

    public static final String PATH_BORROW_ROLE = "/borrowRole";

    public static final String PATH_BORROW_ROLE_TARGET = "/{target_name}";

    @Autowired
    private IBorrowRoleService borrowRoleService;

    @ResponseBody
    @ResourceAccess(role = DefaultRole.PUBLIC, description = "endpoint allowing to switch role")
    @RequestMapping(method = RequestMethod.GET, path = PATH_BORROW_ROLE_TARGET)
    public ResponseEntity<CoupleJwtRole> switchRole(@PathVariable("target_name") String pTargetRoleName)
            throws EntityOperationForbiddenException, JwtException {
        CoupleJwtRole newToken = borrowRoleService.switchTo(pTargetRoleName);
        return new ResponseEntity<>(newToken, HttpStatus.OK);

    }
}
