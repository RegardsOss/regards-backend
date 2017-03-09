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
     * @param pTargetRoleName
     * @return
     * @throws JwtException
     * @throws EntityOperationForbiddenException
     */
    String switchTo(String pTargetRoleName) throws JwtException, EntityOperationForbiddenException;

}
