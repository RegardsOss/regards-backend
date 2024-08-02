/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.authentication.service.oauth2;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.framework.utils.ResponseEntityUtils;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.authentication.domain.data.Authentication;
import fr.cnes.regards.modules.authentication.domain.data.AuthenticationStatus;
import fr.cnes.regards.modules.authentication.domain.exception.oauth2.AuthenticationException;
import fr.cnes.regards.modules.authentication.domain.plugin.AuthenticationPluginResponse;
import fr.cnes.regards.modules.authentication.domain.plugin.IAuthenticationPlugin;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationInfo;
import fr.cnes.regards.modules.authentication.domain.service.IUserAccountManager;
import fr.cnes.regards.modules.authentication.plugins.identityprovider.regards.RegardsInternalAuthenticationPlugin;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Authentication Service. This class provides authentication process to check user/password and retrieve user account.
 *
 * @author Olivier Rousselot
 */
@Service
public class Oauth2AuthenticationService {

    private static final String CHECK_USER_INFO_ERROR_MSG = "An error occurred while trying to check user status";

    private static final Logger LOGGER = LoggerFactory.getLogger(Oauth2AuthenticationService.class);

    /**
     * Default authentication plugin to use if none is configured
     */
    private final IAuthenticationPlugin defaultAuthenticationPlugin;

    /**
     * Tenant resolver used to force tenant for client requests.
     */
    private final IRuntimeTenantResolver runTimeTenantResolver;

    private final IProjectUsersClient projectUsersClient;

    private final IPluginService pluginService;

    private final IProjectsClient projectsClient;

    private final IAccountsClient accountClient;

    private final IUserAccountManager userAccountManager;

    private final JWTService jwtService;

    /**
     * Static and fixed root login. To know if the user who want to log on is root user.
     */
    private final String staticRootLogin;

    /**
     * The default constructor.
     */
    public Oauth2AuthenticationService(IRuntimeTenantResolver runTimeTenantResolver,
                                       IProjectUsersClient projectUsersClient,
                                       IPluginService pluginService,
                                       IProjectsClient projectsClient,
                                       IAccountsClient accountClient,
                                       IUserAccountManager userAccountManager,
                                       JWTService jwtService,
                                       @Value("${regards.accounts.root.user.login}") String staticRootLogin) {
        super();
        this.runTimeTenantResolver = runTimeTenantResolver;
        this.projectUsersClient = projectUsersClient;
        this.pluginService = pluginService;
        this.projectsClient = projectsClient;
        this.accountClient = accountClient;
        this.userAccountManager = userAccountManager;
        this.jwtService = jwtService;
        this.staticRootLogin = staticRootLogin;

        defaultAuthenticationPlugin = new RegardsInternalAuthenticationPlugin(this.accountClient);
    }

    /**
     * Authenticate a user for a given project
     *
     * @param login    user login
     * @param password user password
     * @param scope    project to authenticate to
     * @return Authentication token
     */
    public Authentication doAuthentication(String login, String password, String scope) {
        if ((login == null) || (password == null)) {
            throw new BadCredentialsException("User login / password cannot be empty");
        }
        if (scope == null) {
            String message = "Attribute scope is missing";
            LOGGER.error(message);
            throw new BadCredentialsException(message);
        }

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
        if (response.isAccessGranted()
            && ((status == AuthenticationStatus.USER_UNKNOWN) || //
                (status == AuthenticationStatus.ACCOUNT_UNKNOWN))
            && !response.getPluginClassName().equals(defaultAuthenticationPlugin.getClass().getName())) {
            this.createExternalProjectUser(response.getEmail(), response.getServiceProviderName());
            status = checkUserStatus(response.getEmail(), scope);
        }

        if (status != AuthenticationStatus.ACCESS_GRANTED) {
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

        LOGGER.info("The user <{}> is authenticated for the project {}", response.getEmail(), scope);

        AbstractAuthenticationToken abstractAuthenticationToken = generateAuthenticationUser(scope,
                                                                                             login,
                                                                                             response.getEmail(),
                                                                                             password);
        UserDetails userDetails = (UserDetails) abstractAuthenticationToken.getPrincipal();
        String email = userDetails.getEmail();
        OffsetDateTime expirationDate = jwtService.getExpirationDate(OffsetDateTime.now());
        Map<String, Object> claims = jwtService.generateClaims(userDetails.getTenant(),
                                                               userDetails.getRole(),
                                                               email,
                                                               email);
        String token = jwtService.generateToken(userDetails.getTenant(),
                                                email,
                                                email,
                                                userDetails.getRole(),
                                                expirationDate,
                                                claims);
        return new Authentication(userDetails.getTenant(),
                                  email,
                                  userDetails.getRole(),
                                  response.getServiceProviderName(),
                                  token,
                                  expirationDate);
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
                    } catch (ModuleException e) {
                        LOGGER.info(e.getMessage(), e);
                    }
                }
            }
            return pluginResponse;
        } catch (BeansException e) {
            String message = "Context not initialized, Authentication plugins cannot be retrieve";
            LOGGER.error(message, e);
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
            FeignSecurityManager.asSystem();
            ResponseEntity<EntityModel<Project>> response = projectsClient.retrieveProject(scope);
            return response.getStatusCode().equals(HttpStatus.OK);
        } catch (HttpServerErrorException | HttpClientErrorException e) {
            LOGGER.error("Error while retrieving project {}", scope, e);
            return false;
        } catch (BeansException e) {
            String message = "Context not initialized, Projects client not available";
            LOGGER.error(message, e);
            throw new BadCredentialsException(message);
        } finally {
            FeignSecurityManager.reset();
        }
    }

    /**
     * Create new account and project user by bypassing validation process
     */
    private void createExternalProjectUser(String userEmail, String serviceProviderName) {
        userAccountManager.createUserWithAccountAndGroups(new ServiceProviderAuthenticationInfo.UserInfo.Builder().withEmail(
            userEmail).withFirstname(userEmail).withLastname(userEmail).build(), serviceProviderName);
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
            try {
                FeignSecurityManager.asSystem();
                // Retrieve user account
                ResponseEntity<EntityModel<Account>> accountClientResponse = accountClient.retrieveAccountByEmail(
                    userEmail);

                if (!accountClientResponse.getStatusCode().equals(HttpStatus.OK)) {
                    status = AuthenticationStatus.ACCOUNT_UNKNOWN;
                } else {
                    Account account = ResponseEntityUtils.extractContentOrThrow(accountClientResponse,
                                                                                CHECK_USER_INFO_ERROR_MSG);
                    status = switch (account.getStatus()) {
                        case ACTIVE -> AuthenticationStatus.ACCESS_GRANTED;
                        case INACTIVE -> AuthenticationStatus.ACCOUNT_INACTIVE;
                        case INACTIVE_PASSWORD -> AuthenticationStatus.ACCOUNT_INACTIVE_PASSWORD;
                        case LOCKED -> AuthenticationStatus.ACCOUNT_LOCKED;
                        case PENDING -> AuthenticationStatus.ACCOUNT_PENDING;
                    };
                }
            } catch (HttpServerErrorException | HttpClientErrorException e) {
                LOGGER.error(e.getMessage(), e);
                throw new RsRuntimeException(String.format("Cannot retrieve account with email %s", userEmail), e);
            } finally {
                FeignSecurityManager.reset();
            }

            // Check for project user status if the tenant to access is not instance and the user logged is not instance
            // root user.
            if ((status == AuthenticationStatus.ACCESS_GRANTED)
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
                        status = switch (projectUser.getStatus()) {
                            case WAITING_ACCESS -> AuthenticationStatus.USER_WAITING_ACCESS;
                            case WAITING_EMAIL_VERIFICATION -> AuthenticationStatus.USER_WAITING_EMAIL_VERIFICATION;
                            case ACCESS_DENIED -> AuthenticationStatus.USER_ACCESS_DENIED;
                            case ACCESS_GRANTED -> AuthenticationStatus.ACCESS_GRANTED;
                            case ACCESS_INACTIVE -> AuthenticationStatus.USER_ACCESS_INACTIVE;
                            default -> AuthenticationStatus.USER_UNKNOWN;
                        };
                    }
                } catch (HttpServerErrorException | HttpClientErrorException e) {
                    LOGGER.error(e.getMessage(), e);
                    throw new RsRuntimeException(String.format("Cannot retrieve poroject user with email %s",
                                                               userEmail), e);
                } finally {
                    FeignSecurityManager.reset();
                }
            }
            return status;
        } catch (BeansException e) {
            String message = "Context not initialized, Accounts client is not available";
            LOGGER.error(message, e);
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
    private AbstractAuthenticationToken generateAuthenticationUser(String scope,
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
                    LOGGER.error(message);
                    throw new EntityNotFoundException(email, ProjectUser.class);
                }
            } finally {
                FeignSecurityManager.reset();
            }
        } catch (EntityNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (BeansException e) {
            String message = "Context not initialized, Administration users client is not available";
            LOGGER.error(message, e);
            throw new BadCredentialsException(message);
        } catch (ModuleException e) {
            throw new RsRuntimeException(e);
        }
        return user;
    }

}
