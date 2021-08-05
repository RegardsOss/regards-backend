/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.authentication.service;

import com.google.common.annotations.VisibleForTesting;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.UserVisibility;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountNPassword;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationInfo;
import fr.cnes.regards.modules.authentication.domain.service.IUserAccountManager;
import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import io.vavr.control.Try;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

@Component
@MultitenantTransactional
public class UserAccountManagerImpl implements IUserAccountManager {

    private final IAccountsClient accountsClient;
    private final IProjectUsersClient usersClient;
    private final INotificationClient notificationClient;
    private final IRuntimeTenantResolver runtimeTenantResolver;

    public UserAccountManagerImpl(IAccountsClient accountsClient, IProjectUsersClient usersClient, INotificationClient notificationClient,
            IRuntimeTenantResolver runtimeTenantResolver
    ) {
        this.accountsClient = accountsClient;
        this.usersClient = usersClient;
        this.notificationClient = notificationClient;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public Try<String> createUserWithAccountAndGroups(ServiceProviderAuthenticationInfo.UserInfo userInfo, String serviceProviderName) {
        return Try
                .run(FeignSecurityManager::asSystem)
                .flatMap(unit -> createAccount(userInfo, serviceProviderName)
                        .onFailure(t -> notificationClient.notify(
                                String.format("The user account creation failed with the following error message : %s.", t.getMessage()),
                                "User account creation failed",
                                NotificationLevel.INFO,
                                MimeTypeUtils.TEXT_PLAIN,
                                userInfo.getEmail(),
                                DefaultRole.PROJECT_ADMIN)))
                .flatMap(unit -> createProjectUser(userInfo, serviceProviderName)
                        .transform(t -> wrapInUserCreationFailedHandler(t, userInfo)))
                .map(user -> user.getRole().getName())
                .andFinally(FeignSecurityManager::reset);
    }

    @VisibleForTesting
    protected Try<Unit> createAccount(ServiceProviderAuthenticationInfo.UserInfo userInfo, String serviceProviderName) {
        return Try
                .of(() -> accountsClient.retrieveAccounByEmail(userInfo.getEmail()))
                .map(ResponseEntity::getStatusCode)
                .map(status -> {
                    switch (status) {
                        case OK:
                            return HttpStatus.CREATED;
                        case NOT_FOUND:
                            Account account = new Account(userInfo.getEmail(), userInfo.getFirstname(), userInfo.getLastname(), null);
                            account.setStatus(AccountStatus.ACTIVE);
                            account.setOrigin(serviceProviderName);
                            AccountNPassword accountNPassword = new AccountNPassword(account, account.getPassword(), runtimeTenantResolver.getTenant());
                            return accountsClient.createAccount(accountNPassword).getStatusCode();
                        default:
                            return status;
                    }
                })
                .andThen(status -> {
                    if (status != HttpStatus.CREATED) {
                        throw new RuntimeException(String.format("Failed to retrieve existing account or create new account. Returned status code is %s.", status));
                    }
                })
                .map(ignored -> Unit.UNIT);
    }

    @VisibleForTesting
    protected Try<ProjectUser> createProjectUser(ServiceProviderAuthenticationInfo.UserInfo userInfo, String serviceProviderName) {
        return Try
                .of(() -> usersClient.retrieveProjectUserByEmail(userInfo.getEmail()))
                .flatMap(response -> {
                    HttpStatus status = response.getStatusCode();
                    switch (status) {
                        case OK:
                            return Try.success(response.getBody().getContent());
                        case NOT_FOUND:
                            AccessRequestDto accessRequestDto = new AccessRequestDto()
                                    .setEmail(userInfo.getEmail())
                                    .setFirstName(userInfo.getFirstname())
                                    .setLastName(userInfo.getLastname())
                                    .setMetadata(userInfo.getMetadata().map(t -> new MetaData(t._1, t._2, UserVisibility.READABLE)).toJavaList())
                                    .setOrigin(serviceProviderName);
                            return Try.of(() -> usersClient.createUser(accessRequestDto).getBody().getContent());
                        default:
                            return Try.failure(
                                    new RuntimeException(String.format("Failed to retrieve existing or to create new project user. Returned status code is %s.", status)));
                    }
                });
    }

    private <T> Try<T> wrapInUserCreationFailedHandler(Try<T> call, ServiceProviderAuthenticationInfo.UserInfo userInfo) {
        return call.onFailure(t -> notificationClient.notify(
                String.format("The project user creation failed with the following error message : %s.%nThe associated account exists.", t.getMessage()),
                "Project user creation failed",
                NotificationLevel.INFO,
                MimeTypeUtils.TEXT_PLAIN,
                userInfo.getEmail(),
                DefaultRole.PROJECT_ADMIN));
    }

}
