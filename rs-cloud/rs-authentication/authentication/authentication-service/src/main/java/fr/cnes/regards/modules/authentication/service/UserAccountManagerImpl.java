package fr.cnes.regards.modules.authentication.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import com.google.common.annotations.VisibleForTesting;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
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

@Component
@MultitenantTransactional
public class UserAccountManagerImpl implements IUserAccountManager {

    private IAccountsClient accountsClient;

    private IProjectUsersClient usersClient;

    private INotificationClient notificationClient;

    public UserAccountManagerImpl() {
    }

    @Autowired
    public UserAccountManagerImpl(IAccountsClient accountsClient, IProjectUsersClient usersClient,
            INotificationClient notificationClient) {
        this.accountsClient = accountsClient;
        this.usersClient = usersClient;
        this.notificationClient = notificationClient;
    }

    @Override
    public Try<String> createUserWithAccountAndGroups(ServiceProviderAuthenticationInfo.UserInfo userInfo) {
        return Try.run(FeignSecurityManager::asSystem).flatMap(unit -> createAccount(userInfo)
                // Notify admin if account retrieval or creation failed
                .onFailure(t -> notificationClient.notify(String.format(
                        "The user account creation failed with the following error message : %s.",
                        t.getMessage()),
                                                          "User account creation failed",
                                                          NotificationLevel.INFO,
                                                          MimeTypeUtils.TEXT_PLAIN,
                                                          userInfo.getEmail(),
                                                          DefaultRole.PROJECT_ADMIN))).flatMap(unit -> {
            return createProjectUser(userInfo).transform(t -> wrapInUserCreationFailedHandler(t, userInfo));
        }).map(user -> user.getRole().getName()).andFinally(FeignSecurityManager::reset);
    }

    @VisibleForTesting
    protected Try<Unit> createAccount(ServiceProviderAuthenticationInfo.UserInfo userInfo) {
        return Try.of(() -> accountsClient.retrieveAccounByEmail(userInfo.getEmail()))
                .map(ResponseEntity::getStatusCode)
                // Create account if not exist
                .map(status -> {
                    switch (status) {
                        case OK:
                            return HttpStatus.CREATED;
                        case NOT_FOUND:
                            Account account = new Account(userInfo.getEmail(),
                                                          userInfo.getFirstname(),
                                                          userInfo.getLastname(),
                                                          null);
                            account.setStatus(AccountStatus.ACTIVE);
                            account.setExternal(true);
                            AccountNPassword accountNPassword = new AccountNPassword(account, account.getPassword());
                            return accountsClient.createAccount(accountNPassword).getStatusCode();
                        default:
                            return status;
                    }
                }).andThen(status -> {
                    if (status != HttpStatus.CREATED) {
                        throw new RuntimeException(String.format(
                                "Failed to retrieve existing account or create new account. Returned status code is %s.",
                                status));
                    }
                }).map(ignored -> Unit.UNIT);
    }

    @VisibleForTesting
    protected Try<ProjectUser> createProjectUser(ServiceProviderAuthenticationInfo.UserInfo userInfo) {
        return Try.of(() -> usersClient.retrieveProjectUserByEmail(userInfo.getEmail()))
                // Create account if not exist
                .flatMap(response -> {
                    HttpStatus status = response.getStatusCode();
                    switch (status) {
                        case OK:
                            return Try.success(response.getBody().getContent());
                        case NOT_FOUND:
                            return Try.of(() -> usersClient.createUser(new AccessRequestDto(userInfo.getEmail(),
                                                                                            userInfo.getFirstname(),
                                                                                            userInfo.getLastname(),
                                                                                            null,
                                                                                            userInfo.getMetadata()
                                                                                                    .map(t -> new MetaData(
                                                                                                            t._1,
                                                                                                            t._2,
                                                                                                            UserVisibility.READABLE))
                                                                                                    .toJavaList(),
                                                                                            null,
                                                                                            null,
                                                                                            null)).getBody()
                                    .getContent());
                        default:
                            return Try.failure(new RuntimeException(String.format(
                                    "Failed to retrieve existing or to create new project user. Returned status code is %s.",
                                    status)));
                    }
                });
    }

    private <T> Try<T> wrapInUserCreationFailedHandler(Try<T> call,
            ServiceProviderAuthenticationInfo.UserInfo userInfo) {
        return call.onFailure(t -> notificationClient.notify(String.format(
                "The project user creation failed with the following error message : %s.\nThe associated account exists.",
                t.getMessage()),
                                                             "Project user creation failed",
                                                             NotificationLevel.INFO,
                                                             MimeTypeUtils.TEXT_PLAIN,
                                                             userInfo.getEmail(),
                                                             DefaultRole.PROJECT_ADMIN));
    }

}
