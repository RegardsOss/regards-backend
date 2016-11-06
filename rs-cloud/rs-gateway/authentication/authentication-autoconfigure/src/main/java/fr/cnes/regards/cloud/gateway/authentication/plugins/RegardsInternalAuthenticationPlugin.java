/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.accessrights.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;

/**
 *
 * Class SimpleAuthentication
 *
 * TODO : This class should be a REGARDS authentication plugin.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Component
public class RegardsInternalAuthenticationPlugin implements IAuthenticationPlugin {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RegardsInternalAuthenticationPlugin.class);

    /**
     * Current microservice name
     */
    @Value("${spring.application.name}")
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
    public UserDetails retreiveUserDetails(final String pName, final String pScope) throws UserNotFoundException {
        final UserDetails user = new UserDetails();
        try {
            jwtService.injectToken(pScope, RoleAuthority.getSysRole(microserviceName));

            final ResponseEntity<Resource<ProjectUser>> response = projectUsersClient.retrieveProjectUser(pName);
            if (response.getStatusCode() == HttpStatus.OK) {
                final ProjectUser projectUser = response.getBody().getContent();
                user.setEmail(projectUser.getEmail());
                user.setName(projectUser.getEmail());
                user.setRole(projectUser.getRole().getName());
            } else {
                final String message = String.format("Remote administration request error. Returned code %s",
                                                     response.getStatusCode());
                LOG.error(message);
                throw new UserNotFoundException(message);
            }
        } catch (final JwtException e) {
            LOG.error(e.getMessage(), e);
        } catch (final EntityNotFoundException e) {
            LOG.info(e.getMessage(), e);
        }
        return user;
    }

    @Override
    public AuthenticateStatus authenticate(final String pName, final String pPassword, final String pScope) {
        LOG.info("Trying to authenticate user " + pName + " with password=" + pPassword + " for project " + pScope);

        AuthenticateStatus status = AuthenticateStatus.ACCESS_DENIED;

        try {
            jwtService.injectToken(pScope, RoleAuthority.getSysRole(microserviceName));
            final ResponseEntity<Void> response = accountsClient.validatePassword(pName, pPassword);
            if (response.getStatusCode() == HttpStatus.OK) {
                status = AuthenticateStatus.ACCESS_GRANTED;
            } else {
                LOG.error(String.format("Remote administration request error. Returned code %s",
                                        response.getStatusCode()));
            }
        } catch (final JwtException e) {
            LOG.error(e.getMessage(), e);
        } catch (final EntityNotFoundException e) {
            LOG.error(e.getMessage(), e);
            LOG.error(String.format("Accound %s doesn't exists", pName));
            status = AuthenticateStatus.ACCOUNT_NOT_FOUD;
        }

        return status;

    }

}
