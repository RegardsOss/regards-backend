package fr.cnes.regards.modules.authentication.service;

import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.client.IAccessSettingsClient;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.client.IRegistrationClient;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationInfo;
import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import fr.cnes.regards.modules.dam.client.dataaccess.IUserClient;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test various failure (and one success) scenarios for createUserWithAccountAndGroups.
 * The other methods in UserAccountManagerImpl (IUserAccountManager) are visible only for mocking purposes.
 */
public class UserAccountManagerTest {

    public static final String PROVIDER_NAME = "foo";
    public static final ServiceProviderAuthenticationInfo.UserInfo PROVIDER_USER_INFO =
        new ServiceProviderAuthenticationInfo.UserInfo.Builder()
            .withEmail("email")
            .withFirstname("firstname")
            .withLastname("lastname")
            .build();
    public static final AccessSettings ACCESS_SETTINGS;
    static {
        AccessSettings accessSettings = new AccessSettings();
        accessSettings.setDefaultRole(new Role(DefaultRole.REGISTERED_USER.toString()));
        accessSettings.setDefaultGroups(Collections.emptyList());
        ACCESS_SETTINGS = accessSettings;
    }

    @Mock
    private IAccountsClient accountsClient;

    @Mock
    private IProjectUsersClient usersClient;

    @Mock
    private IAccessSettingsClient accessSettingsClient;

    @Mock
    private IRegistrationClient registrationClient;

    @Mock
    private IUserClient userAccessGroupsClient;

    @Mock
    private INotificationClient notificationClient;

    private UserAccountManagerImpl accountManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        accountManager = spy(new UserAccountManagerImpl(
            accountsClient,
            usersClient,
            accessSettingsClient,
            registrationClient,
            userAccessGroupsClient,
            notificationClient
        ));
    }

    @Test
    public void createUserWithAccountAndGroups_fails_when_createAccount_fails() {
        RuntimeException expected = new RuntimeException("expected");
        doReturn(Try.failure(expected))
            .when(accountManager)
            .createAccount(eq(PROVIDER_USER_INFO));

        Try<String> result =
            accountManager.createUserWithAccountAndGroups(
                PROVIDER_NAME,
                PROVIDER_USER_INFO
            );

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isEqualTo(expected);
        verify(accountManager)
            .createAccount(eq(PROVIDER_USER_INFO));
        verify(accountManager)
            .createUserWithAccountAndGroups(eq(PROVIDER_NAME), eq(PROVIDER_USER_INFO));
        verifyNoMoreInteractions(accountManager);

        verify(notificationClient).notify(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void createUserWithAccountAndGroups_fails_when_autoAcceptAccount_fails() {
        RuntimeException expected = new RuntimeException("expected");
        doReturn(Try.success(Unit.UNIT))
            .when(accountManager)
            .createAccount(eq(PROVIDER_USER_INFO));
        doReturn(Try.failure(expected))
            .when(accountManager)
            .autoAcceptAccount(eq(PROVIDER_USER_INFO));

        Try<String> result =
            accountManager.createUserWithAccountAndGroups(
                PROVIDER_NAME,
                PROVIDER_USER_INFO
            );

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isEqualTo(expected);
        verify(accountManager)
            .createAccount(eq(PROVIDER_USER_INFO));
        verify(accountManager)
            .autoAcceptAccount(eq(PROVIDER_USER_INFO));
        verify(accountManager)
            .createUserWithAccountAndGroups(eq(PROVIDER_NAME), eq(PROVIDER_USER_INFO));
        verifyNoMoreInteractions(accountManager);

        verify(notificationClient).notify(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void createUserWithAccountAndGroups_fails_when_getAccessSettings_fails() {
        RuntimeException expected = new RuntimeException("expected");
        doReturn(Try.success(Unit.UNIT))
            .when(accountManager)
            .createAccount(eq(PROVIDER_USER_INFO));
        doReturn(Try.success(Unit.UNIT))
            .when(accountManager)
            .autoAcceptAccount(eq(PROVIDER_USER_INFO));
        doReturn(Try.failure(expected))
            .when(accountManager)
            .getAccessSettings();

        Try<String> result =
            accountManager.createUserWithAccountAndGroups(
                PROVIDER_NAME,
                PROVIDER_USER_INFO
            );

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isEqualTo(expected);
        verify(accountManager)
            .createAccount(eq(PROVIDER_USER_INFO));
        verify(accountManager)
            .autoAcceptAccount(eq(PROVIDER_USER_INFO));
        verify(accountManager)
            .getAccessSettings();
        verify(accountManager)
            .createUserWithAccountAndGroups(eq(PROVIDER_NAME), eq(PROVIDER_USER_INFO));
        verifyNoMoreInteractions(accountManager);

        verify(notificationClient).notify(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void createUserWithAccountAndGroups_fails_when_createProjectUser_fails() {
        RuntimeException expected = new RuntimeException("expected");
        doReturn(Try.success(Unit.UNIT))
            .when(accountManager)
            .createAccount(eq(PROVIDER_USER_INFO));
        doReturn(Try.success(Unit.UNIT))
            .when(accountManager)
            .autoAcceptAccount(eq(PROVIDER_USER_INFO));
        doReturn(Try.success(ACCESS_SETTINGS))
            .when(accountManager)
            .getAccessSettings();
        doReturn(Try.failure(expected))
            .when(accountManager)
            .createProjectUser(eq(PROVIDER_USER_INFO), eq(ACCESS_SETTINGS.getDefaultRole().getName()));

        Try<String> result =
            accountManager.createUserWithAccountAndGroups(
                PROVIDER_NAME,
                PROVIDER_USER_INFO
            );

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isEqualTo(expected);
        verify(accountManager)
            .createAccount(eq(PROVIDER_USER_INFO));
        verify(accountManager)
            .autoAcceptAccount(eq(PROVIDER_USER_INFO));
        verify(accountManager)
            .getAccessSettings();
        verify(accountManager)
            .createProjectUser(eq(PROVIDER_USER_INFO), eq(ACCESS_SETTINGS.getDefaultRole().getName()));
        verify(accountManager)
            .createUserWithAccountAndGroups(eq(PROVIDER_NAME), eq(PROVIDER_USER_INFO));
        verifyNoMoreInteractions(accountManager);

        verify(notificationClient).notify(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void createUserWithAccountAndGroups_fails_when_configureAccessGroups_fails() {
        RuntimeException expected = new RuntimeException("expected");
        doReturn(Try.success(Unit.UNIT))
            .when(accountManager)
            .createAccount(eq(PROVIDER_USER_INFO));
        doReturn(Try.success(Unit.UNIT))
            .when(accountManager)
            .autoAcceptAccount(eq(PROVIDER_USER_INFO));
        doReturn(Try.success(ACCESS_SETTINGS))
            .when(accountManager)
            .getAccessSettings();
        doReturn(Try.success(Unit.UNIT))
            .when(accountManager)
            .createProjectUser(eq(PROVIDER_USER_INFO), eq(ACCESS_SETTINGS.getDefaultRole().getName()));
        doReturn(Try.failure(expected))
            .when(accountManager)
            .configureAccessGroups(eq(PROVIDER_USER_INFO), eq(List.ofAll(ACCESS_SETTINGS.getDefaultGroups())));

        Try<String> result =
            accountManager.createUserWithAccountAndGroups(
                PROVIDER_NAME,
                PROVIDER_USER_INFO
            );

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isEqualTo(expected);
        verify(accountManager)
            .createAccount(eq(PROVIDER_USER_INFO));
        verify(accountManager)
            .autoAcceptAccount(eq(PROVIDER_USER_INFO));
        verify(accountManager)
            .getAccessSettings();
        verify(accountManager)
            .createProjectUser(eq(PROVIDER_USER_INFO), eq(ACCESS_SETTINGS.getDefaultRole().getName()));
        verify(accountManager)
            .configureAccessGroups(eq(PROVIDER_USER_INFO), eq(List.ofAll(ACCESS_SETTINGS.getDefaultGroups())));
        verify(accountManager)
            .createUserWithAccountAndGroups(eq(PROVIDER_NAME), eq(PROVIDER_USER_INFO));
        verifyNoMoreInteractions(accountManager);

        verify(notificationClient).notify(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void createUserWithAccountAndGroups_ok_when_all_is_well() {
        doReturn(Try.success(Unit.UNIT))
            .when(accountManager)
            .createAccount(eq(PROVIDER_USER_INFO));
        doReturn(Try.success(Unit.UNIT))
            .when(accountManager)
            .autoAcceptAccount(eq(PROVIDER_USER_INFO));
        doReturn(Try.success(ACCESS_SETTINGS))
            .when(accountManager)
            .getAccessSettings();
        doReturn(Try.success(Unit.UNIT))
            .when(accountManager)
            .createProjectUser(eq(PROVIDER_USER_INFO), eq(ACCESS_SETTINGS.getDefaultRole().getName()));
        doReturn(Try.success(Unit.UNIT))
            .when(accountManager)
            .configureAccessGroups(eq(PROVIDER_USER_INFO), eq(List.ofAll(ACCESS_SETTINGS.getDefaultGroups())));

        Try<String> result =
            accountManager.createUserWithAccountAndGroups(
                PROVIDER_NAME,
                PROVIDER_USER_INFO
            );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo(ACCESS_SETTINGS.getDefaultRole().getName());
        verify(accountManager)
            .createAccount(eq(PROVIDER_USER_INFO));
        verify(accountManager)
            .autoAcceptAccount(eq(PROVIDER_USER_INFO));
        verify(accountManager)
            .getAccessSettings();
        verify(accountManager)
            .createProjectUser(eq(PROVIDER_USER_INFO), eq(ACCESS_SETTINGS.getDefaultRole().getName()));
        verify(accountManager)
            .configureAccessGroups(eq(PROVIDER_USER_INFO), eq(List.ofAll(ACCESS_SETTINGS.getDefaultGroups())));
        verify(accountManager)
            .createUserWithAccountAndGroups(eq(PROVIDER_NAME), eq(PROVIDER_USER_INFO));
        verifyNoMoreInteractions(accountManager);
    }
}
