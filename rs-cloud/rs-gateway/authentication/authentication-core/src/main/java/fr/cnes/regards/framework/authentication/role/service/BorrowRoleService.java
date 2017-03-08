/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.role.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.util.Sets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.utils.HttpUtils;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.JwtTokenUtils;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Service
@EnableFeignClients(clients = { IRolesClient.class })
public class BorrowRoleService implements IBorrowRoleService {

    // @Autowired
    // private Oauth2AuthenticationManager oauthManager;

    private final IRolesClient rolesClient;

    private final JWTService jwtService;

    @Value("${spring.application.name}")
    private final String microserviceName;

    public BorrowRoleService(IRolesClient pRolesClient, JWTService pJwtService, String pMicroserviceName) {
        super();
        rolesClient = pRolesClient;
        jwtService = pJwtService;
        microserviceName = pMicroserviceName;
    }

    @Override
    public String switchTo(String pTargetRoleName) throws JwtException, EntityOperationForbiddenException {
        Set<String> borrowableRoleNames = JwtTokenUtils
                .asSafeCallableOnRole(this::getBorrowableRoleNames, jwtService, null)
                .apply(RoleAuthority.getSysRole(microserviceName));

        JWTAuthentication currentToken = jwtService.getCurrentToken();
        if (!borrowableRoleNames.contains(pTargetRoleName)) {
            throw new EntityOperationForbiddenException(
                    String.format("Users of role %s are not allowed to borrow role %s",
                                  currentToken.getUser().getRole(), pTargetRoleName));
        }

        JWTAuthentication newToken = new JWTAuthentication(
                jwtService.generateToken(currentToken.getProject(), currentToken.getName(), pTargetRoleName));
        // oauthManager.authenticate(newToken);
        return newToken.getJwt();
    }

    private Set<String> getBorrowableRoleNames() {
        ResponseEntity<List<Resource<Role>>> response = rolesClient.retrieveBorrowableRoles();
        final HttpStatus responseStatus = response.getStatusCode();
        if (!HttpUtils.isSuccess(responseStatus)) {
            // if it gets here it's mainly because of 404 so it means entity not found
            return Sets.newHashSet();
        }
        List<Role> roleList = HateoasUtils.unwrapList(response.getBody());
        return roleList.stream().map(r -> r.getName()).collect(Collectors.toSet());
    }

}
