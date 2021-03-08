package fr.cnes.regards.modules.authentication.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.client.IAccessSettingsClient;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.client.IRegistrationClient;
import fr.cnes.regards.modules.accessrights.domain.UserVisibility;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountNPassword;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationInfo;
import fr.cnes.regards.modules.authentication.domain.service.IUserAccountManager;
import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import fr.cnes.regards.modules.dam.client.dataaccess.IUserClient;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.control.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

@Component
@MultitenantTransactional
public class UserAccountManagerImpl implements IUserAccountManager {

    private IAccountsClient accountsClient;

    private IProjectUsersClient usersClient;

    private IAccessSettingsClient accessSettingsClient;

    private IRegistrationClient registrationClient;

    private IUserClient userAccessGroupsClient;

    private INotificationClient notificationClient;

    public UserAccountManagerImpl() {
    }

    @Autowired
    public UserAccountManagerImpl(
        IAccountsClient accountsClient,
        IProjectUsersClient usersClient,
        IAccessSettingsClient accessSettingsClient,
        IRegistrationClient registrationClient,
        IUserClient userAccessGroupsClient,
        INotificationClient notificationClient
    ) {
        this.accountsClient = accountsClient;
        this.usersClient = usersClient;
        this.accessSettingsClient = accessSettingsClient;
        this.registrationClient = registrationClient;
        this.userAccessGroupsClient = userAccessGroupsClient;
        this.notificationClient = notificationClient;
    }

    @Override
    public Try<String> createUserWithAccountAndGroups(String serviceProviderName, ServiceProviderAuthenticationInfo.UserInfo userInfo) {
        return Try
            .run(FeignSecurityManager::asSystem)
            .flatMap(unit -> createAccount(userInfo)
                // Notify admin if account retrieval or creation failed
                .onFailure(t ->
                    notificationClient.notify(
                        String.format(
                            "The user account creation via the Service Provider %s failed with the following error message : %s.",
                            serviceProviderName,
                            t.getMessage()
                        ),
                        "User account creation failed",
                        NotificationLevel.INFO,
                        MimeTypeUtils.TEXT_PLAIN,
                        userInfo.getEmail(),
                        DefaultRole.PROJECT_ADMIN
                    )
                ))
//            .flatMap(unit -> autoAcceptAccount(userInfo)
//                .onFailure(t ->
//                    notificationClient.notify(
//                        String.format(
//                            "The user account activation via the Service Provider %s failed with the following error message : %s.\nThe account is created but not accepted.",
//                            serviceProviderName,
//                            t.getMessage()
//                        ),
//                        "User account activation failed",
//                        NotificationLevel.INFO,
//                        MimeTypeUtils.TEXT_PLAIN,
//                        userInfo.getEmail(),
//                        DefaultRole.PROJECT_ADMIN
//                    )
//                ))
            .flatMap(unit -> getAccessSettings()
                .transform(t -> wrapInUserCreationFailedHandler(t, serviceProviderName, userInfo)))
            .flatMap(accessSettings -> {
                    String role = accessSettings.getDefaultRole().getName();
                    List<String> groups = List.ofAll(accessSettings.getDefaultGroups());
                    return createProjectUser(userInfo, role)
                        .transform(t -> wrapInUserCreationFailedHandler(t, serviceProviderName, userInfo))
                        .flatMap(unit -> configureAccessGroups(userInfo, groups)
                            .onFailure(t ->
                                notificationClient.notify(
                                    String.format(
                                        "The association of groups [%s] to user via the Service Provider %s failed with the following error message : %s.\nThe user is created but not fully configured.",
                                        Joiner.on(", ").join(groups),
                                        serviceProviderName,
                                        t.getMessage()
                                    ),
                                    "Access groups configuration failed",
                                    NotificationLevel.INFO,
                                    MimeTypeUtils.TEXT_PLAIN,
                                    userInfo.getEmail(),
                                    DefaultRole.PROJECT_ADMIN
                                )
                            ))
                        .map(unit -> role);
                }
            )
            .andFinally(FeignSecurityManager::reset);
    }

    @VisibleForTesting
    protected Try<Unit> createAccount(ServiceProviderAuthenticationInfo.UserInfo userInfo) {
        return Try
            .of(() -> accountsClient.retrieveAccounByEmail(userInfo.getEmail()))
            .map(ResponseEntity::getStatusCode)
            // Create account if not exist
            .map(status -> {
                switch (status) {
                    case OK:
                        return HttpStatus.CREATED;
                    case NOT_FOUND:
                        Account account =
                            new Account(
                                userInfo.getEmail(),
                                userInfo.getFirstname(),
                                userInfo.getLastname(),
                                null
                            );
                        account.setStatus(AccountStatus.ACTIVE);
                        account.setExternal(true);
                        AccountNPassword accountNPassword = new AccountNPassword(account, account.getPassword());
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
    protected Try<Unit> autoAcceptAccount(ServiceProviderAuthenticationInfo.UserInfo userInfo) {
        return Try
            .of(() -> accountsClient.acceptAccount(userInfo.getEmail()))
            .map(ResponseEntity::getStatusCode)
            .andThen(status -> {
                if (status != HttpStatus.OK) {
                    throw new RuntimeException(String.format("Failed to auto-accept account. Returned status code is %s.", status));
                }
            })
            .map(ignored -> Unit.UNIT);
    }

    @VisibleForTesting
    protected Try<AccessSettings> getAccessSettings() {
        return Try.of(() -> accessSettingsClient.retrieveAccessSettings())
            .flatMap(response -> {
                HttpStatus status = response.getStatusCode();
                if (status == HttpStatus.OK) {
                    return Try.success(response.getBody().getContent());
                } else {
                    return Try.failure(new RuntimeException(String.format("Failed to retrieve access settings. Returned status code is %s.", status)));
                }
            });
    }

    @VisibleForTesting
    protected Try<Unit> createProjectUser(ServiceProviderAuthenticationInfo.UserInfo userInfo, String roleName) {
        return Try
            .of(() -> usersClient.retrieveProjectUserByEmail(userInfo.getEmail()))
            .map(ResponseEntity::getStatusCode)
            // Create account if not exist
            .map(status -> {
                switch (status) {
                    case OK:
                        return HttpStatus.CREATED;
                    case NOT_FOUND:
                        return usersClient.createUser(
                            new AccessRequestDto(
                                userInfo.getEmail(),
                                userInfo.getFirstname(),
                                userInfo.getLastname(),
                                roleName,
                                userInfo.getMetadata()
                                    .map(t -> new MetaData(t._1, t._2, UserVisibility.READABLE))
                                    .toJavaList(),
                                null,
                                null,
                                null
                            )
                        ).getStatusCode();
                    default:
                        return status;
                }
            })
            .andThen(status -> {
                if (status != HttpStatus.CREATED) {
                    throw new RuntimeException(String.format("Failed to retrieve existing or to create new project user. Returned status code is %s.", status));
                }
            })
            .map(ignored -> Unit.UNIT);
    }

    private <T> Try<T> wrapInUserCreationFailedHandler(Try<T> call, String serviceProviderName, ServiceProviderAuthenticationInfo.UserInfo userInfo) {
        return call.onFailure(t ->
            notificationClient.notify(
                String.format(
                    "The project user creation via the Service Provider %s failed with the following error message : %s.\nThe associated account exists.",
                    serviceProviderName,
                    t.getMessage()
                ),
                "Project user creation failed",
                NotificationLevel.INFO,
                MimeTypeUtils.TEXT_PLAIN,
                userInfo.getEmail(),
                DefaultRole.PROJECT_ADMIN
            )
        );
    }

    @VisibleForTesting
    protected Try<Unit> configureAccessGroups(ServiceProviderAuthenticationInfo.UserInfo userInfo, List<String> groups) {
        Set<String> toAssociate = groups.toSet();
        Set<String> associated =
            toAssociate.takeWhile(group -> {
                    try {
                        return userAccessGroupsClient.associateAccessGroupToUser(userInfo.getEmail(), group).getStatusCode() == HttpStatus.OK;
                    } catch (Throwable t) {
                        return false;
                    }
                }
            );

        Set<String> notAssociated = toAssociate.diff(associated);
        if (notAssociated.isEmpty()) {
            return Try.success(Unit.UNIT);
        } else {
            String groupsString = Joiner.on(", ").join(notAssociated);
            return Try.failure(new RuntimeException(String.format("Failed to associate user to groups [%s].", groupsString)))
                .map(ignored -> Unit.UNIT);
        }
    }
}
