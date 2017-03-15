/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.role.service;

import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public interface IBorrowRoleService {

    /**
     * generate a new JWT for the given role if the current user can switch to this role
     *
     * @param pTargetRoleName
     * @return couple (new JWT, Role name wanted)
     * @throws JwtException
     * @throws EntityOperationForbiddenException
     */
    CoupleJwtRole switchTo(String pTargetRoleName) throws JwtException, EntityOperationForbiddenException;

}
