/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import fr.cnes.regards.cloud.gateway.authentication.plugins.IAuthenticationPlugin;
import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.AuthenticationPluginResponse;
import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.AuthenticationStatus;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.accessrights.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 * Class Oauth2AuthenticationManager
 *
 * Authentication Manager. This class provide the authentication process to check user/password and retrieve user
 * account.
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 * 
 * @since 1.0-SNPASHOT
 */
public class Oauth2AuthenticationManager implements AuthenticationManager, BeanFactoryAware {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(Oauth2AuthenticationManager.class);

    /**
     * Current microservice name
     */
    private final String microserviceName;

    /**
     * Default authentication plugin to use if none is configured
     */
    private final IAuthenticationPlugin defaultAuthenticationPlugin;

    /**
     * Security JWT Service
     */
    private final JWTService jwtService;

    /**
     * Spring bean factory
     */
    private BeanFactory beanFactory;

    /**
     * The default constructor.
     *
     * @param pMicroserviceName
     *            The microservice name
     * @param pDefaultAuthenticationPlugin
     *            The {@link IAuthenticationPlugin} to used
     * @param pJwtService
     *            The {@link JWTService} to used
     */
    public Oauth2AuthenticationManager(final String pMicroserviceName,
            final IAuthenticationPlugin pDefaultAuthenticationPlugin, final JWTService pJwtService) {
        super();
        microserviceName = pMicroserviceName;
        defaultAuthenticationPlugin = pDefaultAuthenticationPlugin;
        jwtService = pJwtService;
    }

    @Override
    public Authentication authenticate(final Authentication pAuthentication) {

        final String name = pAuthentication.getName();
        final String password = (String) pAuthentication.getCredentials();

        if ((name == null) || (password == null)) {
            throw new BadCredentialsException("User login / password cannot be empty");
        }

        final Object details = pAuthentication.getDetails();
        final String scope;
        if (details instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, String> detailsMap = (Map<String, String>) details;
            scope = detailsMap.get("scope");
            if (scope == null) {
                throw new BadCredentialsException("Attribute scope is missing");
            }
        } else {
            final String message = "Invalid scope";
            LOG.error(message);
            throw new BadCredentialsException(message);
        }

        // There is no token in SecurityContext now. We have to set one for the given scope to allow access to JPA for
        // plugins service
        try {
            jwtService.injectToken(scope, RoleAuthority.getSysRole(microserviceName));
        } catch (final JwtException e) {
            LOG.error(e.getMessage(), e);
            throw new BadCredentialsException("Internal server error");
        }

        return doAuthentication(name, password, scope);

    }

    /**
     *
     * Authenticate a user for a given project
     *
     * @param pLogin
     *            user login
     * @param pPassword
     *            user password
     * @param pScope
     *            project to authenticate to
     * @return Authentication token
     * @since 1.0-SNAPSHOT
     */
    private Authentication doAuthentication(final String pLogin, final String pPassword, final String pScope) {

        Authentication token = null;

        // If the given is a valid project, then check for project authentication plugins
        if (checkScopeValidity(pScope)) {
            token = doScopePluginsAuthentication(pLogin, pPassword, pScope);
        }

        // If authentication is not valid, try with the default plugin
        if ((token == null) || !token.isAuthenticated()) {
            // Use default REGARDS internal plugin
            token = doPluginAuthentication(defaultAuthenticationPlugin, pLogin, pPassword, pScope);
        }

        // If authentication is valid, create account if it does not exists yet
        if ((token != null) && token.isAuthenticated()) {
            createMissingAccount(token);
        }

        return token;
    }

    /**
     *
     * Authenticate thought authentication plugins for the given scope
     *
     * @param pLogin
     *            User login
     * @param pPassword
     *            User password
     * @param pScope
     *            Project
     * @return Authentication
     * @since 1.0-SNAPSHOT
     */
    private Authentication doScopePluginsAuthentication(final String pLogin, final String pPassword,
            final String pScope) {
        Authentication token = null;
        final IPluginService pluginService = beanFactory.getBean(IPluginService.class);
        if (pluginService == null) {
            final String message = "Context not initialized, Authentication plugins cannot be retreive";
            LOG.error(message);
            throw new BadCredentialsException(message);
        }

        // Get all available authentication plugins
        final List<PluginConfiguration> pluginConfigurations = pluginService
                .getPluginConfigurationsByType(IAuthenticationPlugin.class);
        final Iterator<PluginConfiguration> it = pluginConfigurations.iterator();
        while (it.hasNext() && ((token == null) || !token.isAuthenticated())) {
            try {
                token = doPluginAuthentication(pluginService.getPlugin(it.next().getId()), pLogin, pPassword, pScope);
            } catch (final ModuleException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return token;
    }

    /**
     *
     * Check with administration service, the existence of the given project
     *
     * @param pScope
     *            Project to check
     * @return [true|false]
     * @since 1.0-SNAPSHOT
     */
    private boolean checkScopeValidity(final String pScope) {
        // Check for scope validity
        final IProjectsClient projectsClient = beanFactory.getBean(IProjectsClient.class);
        if (projectsClient == null) {
            final String message = "Context not initialized, Projects client not available";
            LOG.error(message);
            throw new BadCredentialsException(message);
        }

        final ResponseEntity<Resource<Project>> response = projectsClient.retrieveProject(pScope);
        return response.getStatusCode().equals(HttpStatus.OK);
    }

    /**
     *
     * Create account into REGARDS internal accounts system if the account does already exists
     *
     * @param pToken
     *            Authentication token containing user details informations
     * @since 1.0-SNAPSHOT
     */
    private void createMissingAccount(final Authentication pToken) {

        final IAccountsClient accountClient = beanFactory.getBean(IAccountsClient.class);
        if (accountClient == null) {
            final String message = "Context not initialized, Accounts client is not available";
            LOG.error(message);
            throw new BadCredentialsException(message);
        }

        final UserDetails details = (UserDetails) pToken.getPrincipal();

        final ResponseEntity<Resource<Account>> response = accountClient.retrieveAccounByEmail(details.getName());
        if (response.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            accountClient.createAccount(new Account(details.getName(), "", "", null));
        }

    }

    /**
     *
     * Do authentication job with the given authentication plugin
     *
     * @param pPlugin
     *            IAuthenticationPlugin to use for authentication
     * @param pUserName
     *            user name
     * @param pUserPassword
     *            user password
     * @param pScope
     *            scope
     * @return AbstractAuthenticationToken
     */
    private AbstractAuthenticationToken doPluginAuthentication(final IAuthenticationPlugin pPlugin,
            final String pUserName, final String pUserPassword, final String pScope) {

        // Check user/password
        final AuthenticationPluginResponse response = pPlugin.authenticate(pUserName, pUserPassword, pScope);
        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        final Validator validator = factory.getValidator();
        if (!validator.validate(response).isEmpty()) {
            final String message = String.format("Access denied for user %s. Authentication informations are not valid",
                                                 pUserName);
            throw new BadCredentialsException(message);
        }

        if ((response == null) || !response.getStatus().equals(AuthenticationStatus.ACCESS_GRANTED)) {
            String message = String.format("Access denied for user %s.", pUserName);
            if (response != null) {
                message = message + String.format(" Cause : %s", response.getErrorMessage());
            }
            throw new BadCredentialsException(message);
        }

        // Retrieve account
        final UserDetails userDetails;
        try {
            userDetails = retrieveUserDetails(pUserName, pScope);
        } catch (final EntityNotFoundException e) {
            LOG.debug(e.getMessage(), e);
            throw new BadCredentialsException(String.format("User %s does not exists ", pUserName));
        }

        final List<GrantedAuthority> grantedAuths = new ArrayList<>();
        grantedAuths.add(new SimpleGrantedAuthority(userDetails.getRole()));

        return new UsernamePasswordAuthenticationToken(userDetails, pUserPassword, grantedAuths);

    }

    /**
     *
     * Retrieve user information from internal REGARDS database
     *
     * @param pEmail
     *            user email
     * @param pScope
     *            project to authenticate to
     * @return UserDetails
     * @throws EntityNotFoundException
     *             user not found in internal REGARDS database
     * @since 1.0-SNAPSHOT
     */
    public UserDetails retrieveUserDetails(final String pEmail, final String pScope) throws EntityNotFoundException {
        final UserDetails user = new UserDetails();
        try {

            final IProjectUsersClient projectUsersClient = beanFactory.getBean(IProjectUsersClient.class);
            if (projectUsersClient == null) {
                final String message = "Context not initialized, Administration users client is not available";
                LOG.error(message);
                throw new BadCredentialsException(message);
            }

            jwtService.injectToken(pScope, RoleAuthority.getSysRole(microserviceName));

            final ResponseEntity<Resource<ProjectUser>> response = projectUsersClient.retrieveProjectUser(pEmail);
            if (response.getStatusCode() == HttpStatus.OK) {
                final ProjectUser projectUser = response.getBody().getContent();
                user.setName(projectUser.getEmail());
                user.setRole(projectUser.getRole().getName());
            } else {
                final String message = String.format("Remote administration request error. Returned code %s",
                                                     response.getStatusCode());
                LOG.error(message);
                throw new EntityNotFoundException(pEmail, ProjectUser.class);
            }
        } catch (final JwtException | EntityNotFoundException e) {
            LOG.error(e.getMessage(), e);
        }

        return user;
    }

    @Override
    public void setBeanFactory(final BeanFactory pBeanFactory) {
        beanFactory = pBeanFactory;
    }

}
