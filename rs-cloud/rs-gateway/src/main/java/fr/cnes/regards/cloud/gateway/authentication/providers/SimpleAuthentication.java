/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.providers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.netflix.hystrix.exception.HystrixRuntimeException;

import fr.cnes.regards.cloud.gateway.authentication.interfaces.IAuthenticationProvider;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.accessrights.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;

/**
 *
 * Class SimpleAuthentication
 *
 * TODO : This class should be a REGARDS authentication plugin.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Component
public class SimpleAuthentication implements IAuthenticationProvider {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(SimpleAuthentication.class);

    /**
     * Current microservice name
     */
    @Value("${spring.application.name")
    private String microserviceName;

    /**
     * rs-admin microservice client for accounts
     */
    @Autowired
    private IAccountsClient accountsClient;

    /**
     * rs-admin microservice client for accounts
     */
    @Autowired
    private IProjectUsersClient projectUsersClient;

    /**
     * Security JWT service
     */
    @Autowired
    private JWTService jwtService;

    @Override
    public ProjectUser retreiveUser(final String pName, final String pScope) {
        ProjectUser user = null;
        try {
            jwtService.injectToken(pScope, RoleAuthority.getSysRole(microserviceName));

            final ResponseEntity<Resource<ProjectUser>> response = projectUsersClient.retrieveProjectUser(pName);
            if (response.getStatusCode() == HttpStatus.OK) {
                user = response.getBody().getContent();
            } else {
                LOG.error(String.format("Remote administration request error. Returned code %s",
                                        response.getStatusCode()));
            }
        } catch (final JwtException e) {
            LOG.error(e.getMessage(), e);
        } catch (final EntityNotFoundException e) {
            LOG.info(e.getMessage(), e);
        }
        return user;
    }

    @Override
    public UserStatus authenticate(final String pName, final String pPassword, final String pScope) {
        LOG.info("Trying to authenticate user " + pName + " with password=" + pPassword + " for project " + pScope);

        UserStatus status = UserStatus.ACCESS_DENIED;

        try {
            jwtService.injectToken(pScope, RoleAuthority.getSysRole(microserviceName));
            final ResponseEntity<Void> response = accountsClient.validatePassword(pName, pPassword);
            if (response.getStatusCode() == HttpStatus.OK) {
                status = UserStatus.ACCESS_GRANTED;
            } else {
                LOG.error(String.format("Remote administration request error. Returned code %s",
                                        response.getStatusCode()));
            }
        } catch (final HystrixRuntimeException | JwtException e) {
            LOG.error(e.getMessage(), e);
        } catch (final EntityNotFoundException e) {
            LOG.error(e.getMessage(), e);
            LOG.error(String.format("Accound %s doesn't exists", pName));
        }

        return status;

    }

}
