/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.authentication.exception.AuthenticationException;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.framework.utils.ResponseEntityUtils;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.authentication.domain.plugin.AuthenticationPluginResponse;
import fr.cnes.regards.modules.authentication.domain.plugin.IAuthenticationPlugin;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationInfo;
import fr.cnes.regards.modules.authentication.domain.service.IUserAccountManager;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Authentication Manager. This class provides authentication process to check user/password and retrieve user
 * account.
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 */
public class Oauth2AuthenticationManager implements AuthenticationManager, BeanFactoryAware {

    private static final String CHECK_USER_INFO_ERROR_MSG = "An error occurred while trying to check user status";

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
     * Static and fixed root login. To know if the user who want to log on is root user.
     */
    private final String staticRootLogin;

    /**
     * Spring bean factory
     */
    private BeanFactory beanFactory;

    /**
     * The default constructor.
     *
     * @param defaultAuthenticationPlugin The {@link IAuthenticationPlugin} to used
     */
    public Oauth2AuthenticationManager(IAuthenticationPlugin defaultAuthenticationPlugin,
                                       IRuntimeTenantResolver runTimeTenantResolver,
                                       String staticRootLogin) {
        super();
        this.defaultAuthenticationPlugin = defaultAuthenticationPlugin;
        this.runTimeTenantResolver = runTimeTenantResolver;
        this.staticRootLogin = staticRootLogin;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {

        String name = authentication.getName();
        String password = (String) authentication.getCredentials();

        if ((name == null) || (password == null)) {
            throw new BadCredentialsException("User login / password cannot be empty");
        }

        Object details = authentication.getDetails();
        String scope;
        if (details instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, String> detailsMap = (Map<String, String>) details;
            scope = detailsMap.get("scope");
            if (scope == null) {
                String message = "Attribute scope is missing";
                LOG.error(message);
                throw new BadCredentialsException(message);
            }
        } else {
            String message = "Invalid scope";
            LOG.error(message);
            throw new BadCredentialsException(message);
        }

        // There is no token in SecurityContext now. We have to set one for the given scope to allow access to JPA for
        // plugins service
        runTimeTenantResolver.forceTenant(scope);
        FeignSecurityManager.asSystem();
        Authentication auth = doAuthentication(name, password, scope);
        FeignSecurityManager.reset();
        return auth;

    }

    /**
     * Authenticate a user for a given project
     *
     * @param login    user login
     * @param password user password
     * @param scope    project to authenticate to
     * @return Authentication token
     */
    private Authentication doAuthentication(String login, String password, String scope) {

        AuthenticationPluginResponse response = new AuthenticationPluginResponse(false, null);

        // If the given is a valid project, then check for project authentication plugins
        if (checkScopeValidity(scope)) {
            response = doScopePluginsAuthentication(login, password, scope);
        }

        // If authentication is not valid, try with the default plugin
        if (!response.isAccessGranted()) {
            // Use default REGARDS internal plugin
            response = doPluginAuthentication(defaultAuthenticationPlugin, login, password, scope);
        }

        // Before returning generating token, check user status.
        AuthenticationStatus status = checkUserStatus(response.getEmail(), scope);

        // If authentication is granted and user does not exist and plugin is not the regards internal authentication.
        if (Boolean.TRUE.equals(response.isAccessGranted())
            && (status.equals(AuthenticationStatus.USER_UNKNOWN)
                || status.equals(AuthenticationStatus.ACCOUNT_UNKNOWN))
            && !response.getPluginClassName().equals(defaultAuthenticationPlugin.getClass().getName())) {
            this.createExternalProjectUser(response.getEmail(), response.getServiceProviderName());
            status = checkUserStatus(response.getEmail(), scope);
        }

        if (!status.equals(AuthenticationStatus.ACCESS_GRANTED)) {
            String message = String.format("Access denied for user %s. cause : user status is %s",
                                           response.getEmail(),
                                           status.name());
            throw new AuthenticationException(message, status);
        }

        // If access is not allowed then return an unknown account error response
        if (!response.isAccessGranted()) {
            String message = String.format("Access denied for user %s. cause: %s",
                                           response.getEmail(),
                                           response.getErrorMessage());
            throw new AuthenticationException(message, AuthenticationStatus.ACCOUNT_UNKNOWN);
        }

        LOG.info("The user <{}> is authenticated for the project {}", response.getEmail(), scope);

        return generateAuthenticationToken(scope, login, response.getEmail(), password);
    }

    /**
     * Authenticate thought authentication plugins for the given scope
     *
     * @param login    User login
     * @param password User password
     * @param scope    Project
     * @return Authentication
     */
    private AuthenticationPluginResponse doScopePluginsAuthentication(String login, String password, String scope) {

        AuthenticationPluginResponse pluginResponse = new AuthenticationPluginResponse(false, login);

        try {
            IPluginService pluginService = beanFactory.getBean(IPluginService.class);
            // Get all available authentication plugins
            List<PluginConfiguration> plgConfs = pluginService.getPluginConfigurationsByType(IAuthenticationPlugin.class);
            for (PluginConfiguration pluginConfiguration : plgConfs) {
                if (!pluginResponse.isAccessGranted()) {
                    try {
                        pluginResponse = doPluginAuthentication(pluginService.getPlugin(pluginConfiguration.getBusinessId()),
                                                                login,
                                                                password,
                                                                scope);
                        pluginResponse.setServiceProviderName(pluginConfiguration.getBusinessId());
                    } catch (ModuleException | NotAvailablePluginConfigurationException e) {
                        LOG.info(e.getMessage(), e);
                    }
                }
            }
            return pluginResponse;
        } catch (BeansException e) {
            String message = "Context not initialized, Authentication plugins cannot be retrieve";
            LOG.error(message, e);
            throw new BadCredentialsException(message);
        }
    }

    /**
     * Check with administration service, the existence of the given project
     *
     * @param scope Project to check
     * @return [true|false]
     */
    private boolean checkScopeValidity(String scope) {
        // Check for scope validity
        try {
            IProjectsClient projectsClient = beanFactory.getBean(IProjectsClient.class);

            ResponseEntity<EntityModel<Project>> response = projectsClient.retrieveProject(scope);
            return response.getStatusCode().equals(HttpStatus.OK);
        } catch (BeansException e) {
            String message = "Context not initialized, Projects client not available";
            LOG.error(message, e);
            throw new BadCredentialsException(message);
        }
    }

    /**
     * Create new account and project user by bypassing validation process
     */
    private void createExternalProjectUser(String userEmail, String serviceProviderName) {
        Try.of(() -> beanFactory.getBean(IUserAccountManager.class))
           .flatMap(userAccountManager -> userAccountManager.createUserWithAccountAndGroups(new ServiceProviderAuthenticationInfo.UserInfo.Builder().withEmail(
               userEmail).withFirstname(userEmail).withLastname(userEmail).build(), serviceProviderName));
    }

    /**
     * Check global user status by checking account status then projectUser status.
     *
     * @return {@link AuthenticationStatus}
     */
    private AuthenticationStatus checkUserStatus(String userEmail, String tenant) {

        try {
            // Default status
            AuthenticationStatus status;
            // Client account
            IAccountsClient accountClient = beanFactory.getBean(IAccountsClient.class);
            // Client account
            IProjectUsersClient projectUsersClient = beanFactory.getBean(IProjectUsersClient.class);
            try {
                FeignSecurityManager.asSystem();
                // Retrieve user account
                ResponseEntity<EntityModel<Account>> accountClientResponse = accountClient.retrieveAccounByEmail(
                    userEmail);

                if (!accountClientResponse.getStatusCode().equals(HttpStatus.OK)) {
                    status = AuthenticationStatus.ACCOUNT_UNKNOWN;
                } else {
                    Account account = ResponseEntityUtils.extractContentOrThrow(accountClientResponse,
                                                                                CHECK_USER_INFO_ERROR_MSG);
                    switch (account.getStatus()) {
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
            if (status.equals(AuthenticationStatus.ACCESS_GRANTED)
                && (tenant != null)
                && !runTimeTenantResolver.isInstance()
                && !userEmail.equals(staticRootLogin)) {
                // Retrieve user projectUser
                try {
                    FeignSecurityManager.asSystem();
                    ResponseEntity<EntityModel<ProjectUser>> projectUserClientResponse = projectUsersClient.retrieveProjectUserByEmail(
                        userEmail);

                    if (!projectUserClientResponse.getStatusCode().equals(HttpStatus.OK)) {
                        status = AuthenticationStatus.USER_UNKNOWN;
                    } else {
                        ProjectUser projectUser = ResponseEntityUtils.extractContentOrThrow(projectUserClientResponse,
                                                                                            CHECK_USER_INFO_ERROR_MSG);
                        switch (projectUser.getStatus()) {
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
        } catch (BeansException e) {
            String message = "Context not initialized, Accounts client is not available";
            LOG.error(message, e);
            throw new BadCredentialsException(message);
        } catch (ModuleException e) {
            throw new RsRuntimeException(e);
        }

    }

    /**
     * Do authentication job with the given authentication plugin
     *
     * @param plugin       IAuthenticationPlugin to use for authentication
     * @param userName     username
     * @param userPassword user password
     * @param scope        scope
     * @return AbstractAuthenticationToken
     */
    private AuthenticationPluginResponse doPluginAuthentication(IAuthenticationPlugin plugin,
                                                                String userName,
                                                                String userPassword,
                                                                String scope) {

        // Check user/password
        AuthenticationPluginResponse response = plugin.authenticate(userName, userPassword, scope);
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            if ((response == null) || !validator.validate(response).isEmpty()) {
                return new AuthenticationPluginResponse(false, userName);
            }
        }
        response.setPluginClassName(plugin.getClass().getName());

        return response;

    }

    /**
     * Generate a token for the given scope and userName
     *
     * @param scope    project
     * @param login    user email
     * @param password user password
     * @return {@link AbstractAuthenticationToken}
     */
    private AbstractAuthenticationToken generateAuthenticationToken(String scope,
                                                                    String login,
                                                                    String email,
                                                                    String password) {
        UserDetails userDetails;

        List<GrantedAuthority> grantedAuths = new ArrayList<>();

        // If instance tenant is requested, only instance user can be authenticated.
        if (runTimeTenantResolver.isInstance() && login.equals(staticRootLogin)) {
            // Manage root login
            userDetails = new UserDetails(scope, login, email, DefaultRole.INSTANCE_ADMIN.toString());
        } else if (!runTimeTenantResolver.isInstance()) {
            // Retrieve account
            userDetails = retrieveUserDetails(login, email, scope);
        } else {
            // Unauthorized access to instance tenant for authenticated user.
            throw new AuthenticationException("Access denied to REGARDS instance administration for user " + login,
                                              AuthenticationStatus.INSTANCE_ACCESS_DENIED);
        }
        grantedAuths.add(new SimpleGrantedAuthority(userDetails.getRole()));
        return new UsernamePasswordAuthenticationToken(userDetails, password, grantedAuths);
    }

    /**
     * Retrieve user information from internal REGARDS database
     *
     * @param login user login
     * @param email user email
     * @param scope project to authenticate to
     * @return UserDetails
     */
    public UserDetails retrieveUserDetails(String login, String email, String scope) {
        UserDetails user = null;
        try {

            IProjectUsersClient projectUsersClient = beanFactory.getBean(IProjectUsersClient.class);

            try {
                FeignSecurityManager.asSystem();
                ResponseEntity<EntityModel<ProjectUser>> response = projectUsersClient.retrieveProjectUserByEmail(email);
                if (response.getStatusCode() == HttpStatus.OK) {
                    // special case to handle root login because it is not a real ProjectUser
                    ProjectUser projectUser = ResponseEntityUtils.extractContentOrThrow(response,
                                                                                        "An error occurred while trying to retrieve user details");
                    if (!Objects.equals(email, staticRootLogin)) {
                        projectUser.setLastConnection(OffsetDateTime.now());
                        // update last connection date
                        projectUsersClient.updateProjectUser(projectUser.getId(), projectUser);
                    }
                    // In regards system login is same as email
                    user = new UserDetails(scope, projectUser.getEmail(), login, projectUser.getRole().getName());
                } else {
                    String message = String.format("Remote administration request error. Returned code %s",
                                                   response.getStatusCode());
                    LOG.error(message);
                    throw new EntityNotFoundException(email, ProjectUser.class);
                }
            } finally {
                FeignSecurityManager.reset();
            }
        } catch (EntityNotFoundException e) {
            LOG.error(e.getMessage(), e);
        } catch (BeansException e) {
            String message = "Context not initialized, Administration users client is not available";
            LOG.error(message, e);
            throw new BadCredentialsException(message);
        } catch (ModuleException e) {
            throw new RsRuntimeException(e);
        }

        return user;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

}
