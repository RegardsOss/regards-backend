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

import feign.FeignException;
import fr.cnes.regards.framework.authentication.exception.AuthenticationException;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.client.IRegistrationClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.authentication.plugins.IAuthenticationPlugin;
import fr.cnes.regards.modules.authentication.plugins.domain.AuthenticationPluginResponse;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 * Authentication Manager. This class provide the authentication process to check user/password and retrieve user
 * account.
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 */
public class Oauth2AuthenticationManager implements AuthenticationManager, BeanFactoryAware {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(Oauth2AuthenticationManager.class);

    /**
     * Default authentication plugin to use if none is configured
     */
    private final IAuthenticationPlugin defaultAuthenticationPlugin;

    /**
     * Tenant resolver used to force tenant for client requests.
     */
    private final IRuntimeTenantResolver runTimeTenantResolver;

    /**
     * Static and fixed root login. To know if the user who want to autify is root user.
     */
    private final String staticRootLogin;

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
     */
    public Oauth2AuthenticationManager(final IAuthenticationPlugin pDefaultAuthenticationPlugin,
            final IRuntimeTenantResolver pRunTimeTenantResolver, final String pStaticRootLogin) {
        super();
        defaultAuthenticationPlugin = pDefaultAuthenticationPlugin;
        runTimeTenantResolver = pRunTimeTenantResolver;
        staticRootLogin = pStaticRootLogin;
    }

    @Override
    public Authentication authenticate(final Authentication pAuthentication) {

        final String name = pAuthentication.getName();
        final String password = (String) pAuthentication.getCredentials();

        if ((name == null) || (password == null)) {
            throw new BadCredentialsException("User login / password cannot be empty");
        }

        final Object details = pAuthentication.getDetails();
        String origineUrl = "";
        String requestLink = "";
        final String scope;
        if (details instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, String> detailsMap = (Map<String, String>) details;
            scope = detailsMap.get("scope");
            if (scope == null) {
                final String message = "Attribute scope is missing";
                LOG.error(message);
                throw new BadCredentialsException(message);
            }
            origineUrl = detailsMap.get("origineUrl");
            requestLink = detailsMap.get("requestLink");
        } else {
            final String message = "Invalid scope";
            LOG.error(message);
            throw new BadCredentialsException(message);
        }

        // There is no token in SecurityContext now. We have to set one for the given scope to allow access to JPA for
        // plugins service
        runTimeTenantResolver.forceTenant(scope);
        FeignSecurityManager.asSystem();
        final Authentication auth = doAuthentication(name, password, scope, origineUrl, requestLink);
        FeignSecurityManager.reset();
        return auth;

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
    private Authentication doAuthentication(final String pLogin, final String pPassword, final String pScope,
            final String pOrigineUrl, final String pRequestLink) {

        AuthenticationPluginResponse response = new AuthenticationPluginResponse(false, null);

        // If the given is a valid project, then check for project authentication plugins
        if (checkScopeValidity(pScope)) {
            response = doScopePluginsAuthentication(pLogin, pPassword, pScope);
        }

        // If authentication is not valid, try with the default plugin
        if (!response.getAccessGranted()) {
            // Use default REGARDS internal plugin
            response = doPluginAuthentication(defaultAuthenticationPlugin, pLogin, pPassword, pScope);
        }

        // Before returning generating token, check user status.
        AuthenticationStatus status = checkUserStatus(response.getEmail(), pScope);
        // If authentication is granted and user does not exists and plugin is not the regards internal authentication.
        if (response.getAccessGranted()
                && (status.equals(AuthenticationStatus.USER_UNKNOWN)
                        || status.equals(AuthenticationStatus.ACCOUNT_UNKNOWN))
                && (!response.getPluginClassName().equals(defaultAuthenticationPlugin.getClass().getName()))) {
            this.createMissingProjectUser(response.getEmail(), pOrigineUrl, pRequestLink);
            status = checkUserStatus(response.getEmail(), pScope);
        }

        if (!status.equals(AuthenticationStatus.ACCESS_GRANTED)) {
            final String message = String.format("Access denied for user %s. cause : user status is %s",
                                                 response.getEmail(), status.name());
            throw new AuthenticationException(message, status);
        }

        // If access is not allowed then return an unknown account error response
        if (!response.getAccessGranted()) {
            final String message = String.format("Access denied for user %s. cause: %s", response.getEmail(),
                                                 response.getErrorMessage());
            throw new AuthenticationException(message, AuthenticationStatus.ACCOUNT_UNKNOWN);
        }

        LOG.info("The user <{}> is authenticate for the project {}", response.getEmail(), pScope);

        return generateToken(pScope, response.getEmail(), pPassword);
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
    private AuthenticationPluginResponse doScopePluginsAuthentication(final String pLogin, final String pPassword,
            final String pScope) {

        AuthenticationPluginResponse pluginResponse = new AuthenticationPluginResponse(false, pLogin);

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
        while (it.hasNext() && !pluginResponse.getAccessGranted()) {
            try {
                pluginResponse = doPluginAuthentication(pluginService.getPlugin(it.next().getId()), pLogin, pPassword,
                                                        pScope);
            } catch (final ModuleException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return pluginResponse;
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
     * Create new account and project user by bypassing validation process
     *
     * @param pUserEmail
     * @since 1.0-SNAPSHOT
     */
    private void createMissingProjectUser(final String pUserEmail, final String pOrigineUrl,
            final String pRequestLink) {
        final IRegistrationClient client = beanFactory.getBean(IRegistrationClient.class);
        if (client == null) {
            final String message = "Context not initialized, Accounts client is not available";
            LOG.error(message);
            throw new BadCredentialsException(message);
        }
        LOG.info("Creating new account for user email=" + pUserEmail);
        try {
            FeignSecurityManager.asSystem();
            client.requestAccess(new AccessRequestDto(pUserEmail, pUserEmail, pUserEmail, DefaultRole.PUBLIC.name(),
                    null, null, pOrigineUrl, pRequestLink));
        } catch (FeignException e) {
            final String message = String.format("Error creation new account for user %s. Cause : %s", pUserEmail,
                                                 e.getMessage());
            LOG.error(message);
            throw new BadCredentialsException(message);
        } finally {
            FeignSecurityManager.reset();
        }
    }

    /**
     *
     * Check global user status by checking account status then projectUser status.
     *
     * @param pUserEmail
     * @param pTenant
     * @return {@link AuthenticationStatus}
     * @since 1.0-SNAPSHOT
     */
    private AuthenticationStatus checkUserStatus(final String pUserEmail, final String pTenant) {

        // Default status
        AuthenticationStatus status;

        // Client account
        final IAccountsClient accountClient = beanFactory.getBean(IAccountsClient.class);
        if (accountClient == null) {
            final String message = "Context not initialized, Accounts client is not available";
            LOG.error(message);
            throw new BadCredentialsException(message);
        }

        // Client account
        final IProjectUsersClient projectUsersClient = beanFactory.getBean(IProjectUsersClient.class);
        if (projectUsersClient == null) {
            final String message = "Context not initialized, ProjectUsers client is not available";
            LOG.error(message);
            throw new BadCredentialsException(message);
        }

        try {
            FeignSecurityManager.asSystem();
            // Retrieve user account
            final ResponseEntity<Resource<Account>> accountClientResponse = accountClient
                    .retrieveAccounByEmail(pUserEmail);

            if (!accountClientResponse.getStatusCode().equals(HttpStatus.OK)) {
                status = AuthenticationStatus.ACCOUNT_UNKNOWN;
            } else {
                switch (accountClientResponse.getBody().getContent().getStatus()) {
                    case ACTIVE:
                        status = AuthenticationStatus.ACCESS_GRANTED;
                        break;
                    case INACTIVE:
                        status = AuthenticationStatus.ACCOUNT_INACTIVE;
                        break;
                    case INACTIVE_PASSWORD:
                        status = AuthenticationStatus.ACCOUNT_INACTIVE_PASSWORD;
                        break;
                    case LOCKED:
                        status = AuthenticationStatus.ACCOUNT_LOCKED;
                        break;
                    case PENDING:
                        status = AuthenticationStatus.ACCOUNT_PENDING;
                        break;
                    default:
                        status = AuthenticationStatus.ACCOUNT_UNKNOWN;
                }
            }
        } finally {
            FeignSecurityManager.reset();
        }

        // Check for project user status if the tenant to access is not instance and the user logged is not instance
        // root user.
        if (status.equals(AuthenticationStatus.ACCESS_GRANTED) && (pTenant != null)
                && !runTimeTenantResolver.isInstance() && !pUserEmail.equals(staticRootLogin)) {
            // Retrieve user projectUser
            try {
                FeignSecurityManager.asSystem();
                final ResponseEntity<Resource<ProjectUser>> projectUserClientResponse = projectUsersClient
                        .retrieveProjectUserByEmail(pUserEmail);

                if (!projectUserClientResponse.getStatusCode().equals(HttpStatus.OK)) {
                    status = AuthenticationStatus.USER_UNKNOWN;
                } else {
                    switch (projectUserClientResponse.getBody().getContent().getStatus()) {
                        case WAITING_ACCESS:
                            status = AuthenticationStatus.USER_WAITING_ACCESS;
                            break;
                        case WAITING_EMAIL_VERIFICATION:
                            status = AuthenticationStatus.USER_WAITING_EMAIL_VERIFICATION;
                            break;
                        case ACCESS_DENIED:
                            status = AuthenticationStatus.USER_ACCESS_DENIED;
                            break;
                        case ACCESS_GRANTED:
                            status = AuthenticationStatus.ACCESS_GRANTED;
                            break;
                        case ACCESS_INACTIVE:
                            status = AuthenticationStatus.USER_ACCESS_INACTIVE;
                            break;
                        default:
                            status = AuthenticationStatus.USER_UNKNOWN;
                    }
                }
            } finally {
                FeignSecurityManager.reset();
            }
        }

        return status;
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
    private AuthenticationPluginResponse doPluginAuthentication(final IAuthenticationPlugin pPlugin,
            final String pUserName, final String pUserPassword, final String pScope) {

        // Check user/password
        final AuthenticationPluginResponse response = pPlugin.authenticate(pUserName, pUserPassword, pScope);
        final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        final Validator validator = factory.getValidator();

        if ((response == null) || !validator.validate(response).isEmpty()) {
            return new AuthenticationPluginResponse(false, pUserName);
        }

        response.setPluginClassName(pPlugin.getClass().getName());

        return response;

    }

    /**
     *
     * Generate a token for the given scope and userName
     *
     * @param pScope
     *            project
     * @param pUserName
     *            user email
     * @param pUserPassword
     *            user password
     * @return {@link AbstractAuthenticationToken}
     * @since 1.0-SNAPSHOT
     */
    private AbstractAuthenticationToken generateToken(final String pScope, final String pUserName,
            final String pUserPassword) {
        UserDetails userDetails;

        final List<GrantedAuthority> grantedAuths = new ArrayList<>();

        // If instance tenant is requested, only instance user can be authenticated.
        if (runTimeTenantResolver.isInstance() && pUserName.equals(staticRootLogin)) {
            // Manage root login
            userDetails = new UserDetails();
            userDetails.setName(pUserName);
            userDetails.setRole(DefaultRole.INSTANCE_ADMIN.toString());
            userDetails.setTenant(pScope);
        } else if (!runTimeTenantResolver.isInstance()) {
            // Retrieve account
            try {
                userDetails = retrieveUserDetails(pUserName, pScope);
            } catch (final EntityNotFoundException e) {
                LOG.debug(e.getMessage(), e);
                throw new BadCredentialsException(String.format("User %s does not exists ", pUserName));
            }
        } else {
            // Unauthorized access to instance tenant for authenticated user.
            throw new AuthenticationException("Access denied to REGARDS instance administration for user " + pUserName,
                    AuthenticationStatus.INSTANCE_ACCESS_DENIED);
        }
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
            try {
                FeignSecurityManager.asSystem();
                final ResponseEntity<Resource<ProjectUser>> response = projectUsersClient
                        .retrieveProjectUserByEmail(pEmail);
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
            } finally {
                FeignSecurityManager.reset();
            }
        } catch (final EntityNotFoundException e) {
            LOG.error(e.getMessage(), e);
        }

        return user;
    }

    @Override
    public void setBeanFactory(final BeanFactory pBeanFactory) {
        beanFactory = pBeanFactory;
    }

}
