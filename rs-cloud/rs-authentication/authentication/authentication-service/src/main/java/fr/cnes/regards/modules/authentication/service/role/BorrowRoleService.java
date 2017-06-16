/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.authentication.service.role;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.utils.HttpUtils;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Service
public class BorrowRoleService implements IBorrowRoleService {

    private final IRolesClient rolesClient;

    private final JWTService jwtService;

    public BorrowRoleService(IRolesClient pRolesClient, JWTService pJwtService) {
        super();
        rolesClient = pRolesClient;
        jwtService = pJwtService;
    }

    @Override
    public CoupleJwtRole switchTo(String pTargetRoleName) throws JwtException, EntityOperationForbiddenException {
        Set<String> borrowableRoleNames = getBorrowableRoleNames();

        JWTAuthentication currentToken = jwtService.getCurrentToken();
        if (!borrowableRoleNames.contains(pTargetRoleName)) {
            throw new EntityOperationForbiddenException(
                    String.format("Users of role %s are not allowed to borrow role %s",
                                  currentToken.getUser().getRole(), pTargetRoleName));
        }

        return new CoupleJwtRole(
                jwtService.generateToken(currentToken.getTenant(), currentToken.getName(), pTargetRoleName),
                pTargetRoleName);

    }

    private Set<String> getBorrowableRoleNames() {
        //DO NOT USE FEIGN SECURITY MANAGER HERE: we need to know the user that send the request
        ResponseEntity<List<Resource<Role>>> response = rolesClient.getBorrowableRoles();
        final HttpStatus responseStatus = response.getStatusCode();
        if (!HttpUtils.isSuccess(responseStatus)) {
            // if it gets here it's mainly because of 404 so it means entity not found
            return Sets.newHashSet();
        }
        List<Role> roleList = HateoasUtils.unwrapList(response.getBody());
        return roleList.stream().map(r -> r.getName()).collect(Collectors.toSet());
    }

}
