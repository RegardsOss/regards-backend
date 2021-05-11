package fr.cnes.regards.modules.authentication.service;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.client.IAccessRightSettingClient;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationInfo;
import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import io.vavr.control.Try;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test various failure (and one success) scenarios for createUserWithAccountAndGroups.
 * The other methods in UserAccountManagerImpl (IUserAccountManager) are visible only for mocking purposes.
 */
public class UserAccountManagerTest {

    public static final String PROVIDER_NAME = "foo";

    public static final ServiceProviderAuthenticationInfo.UserInfo PROVIDER_USER_INFO = new ServiceProviderAuthenticationInfo.UserInfo.Builder()
            .withEmail("email").withFirstname("firstname").withLastname("lastname").build();

    public static final Map<String, DynamicTenantSetting> ACCESS_SETTINGS;

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UserAccountManagerTest.class);

    static {
        GsonUtil.setGson(new Gson());
        ACCESS_SETTINGS = AccessSettings.SETTING_LIST.stream()
                .collect(Collectors.toMap(DynamicTenantSetting::getName, Function.identity()));
    }

    @Mock
    private IAccountsClient accountsClient;

    @Mock
    private IProjectUsersClient usersClient;

    @Mock
    private IAccessRightSettingClient accessSettingsClient;

    @Mock
    private INotificationClient notificationClient;

    private UserAccountManagerImpl accountManager;

    private ProjectUser user;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        user = new ProjectUser();
        user.setEmail("plop@plop.fr");
        user.setRole(new Role(DefaultRole.PUBLIC.toString()));

        accountManager = spy(new UserAccountManagerImpl(accountsClient, usersClient, notificationClient));
    }

    @Test
    public void createUserWithAccountAndGroups_fails_when_createAccount_fails() {
        RuntimeException expected = new RuntimeException("expected");
        doReturn(Try.failure(expected)).when(accountManager).createAccount(eq(PROVIDER_USER_INFO));

        Try<String> result = accountManager.createUserWithAccountAndGroups(PROVIDER_USER_INFO);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isEqualTo(expected);
        verify(accountManager).createAccount(eq(PROVIDER_USER_INFO));
        verify(accountManager).createUserWithAccountAndGroups(eq(PROVIDER_USER_INFO));
        verifyNoMoreInteractions(accountManager);

        verify(notificationClient).notify(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void createUserWithAccountAndGroups_fails_when_createProjectUser_fails() {
        RuntimeException expected = new RuntimeException("expected");
        doReturn(Try.success(Unit.UNIT)).when(accountManager).createAccount(eq(PROVIDER_USER_INFO));
        doReturn(Try.failure(expected)).when(accountManager).createProjectUser(eq(PROVIDER_USER_INFO));

        Try<String> result = accountManager.createUserWithAccountAndGroups(PROVIDER_USER_INFO);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isEqualTo(expected);
        verify(accountManager).createAccount(eq(PROVIDER_USER_INFO));
        verify(accountManager).createProjectUser(eq(PROVIDER_USER_INFO));
        verify(accountManager).createUserWithAccountAndGroups(eq(PROVIDER_USER_INFO));
        verifyNoMoreInteractions(accountManager);

        verify(notificationClient).notify(any(), any(), any(), any(), any(), any());
    }

    @Test
    public void createUserWithAccountAndGroups_ok_when_all_is_well() {
        doReturn(Try.success(Unit.UNIT)).when(accountManager).createAccount(eq(PROVIDER_USER_INFO));
        doReturn(Try.success(user)).when(accountManager).createProjectUser(eq(PROVIDER_USER_INFO));

        Try<String> result = accountManager.createUserWithAccountAndGroups(PROVIDER_USER_INFO);

        if (result.isFailure()) {
            LOGGER.error(result.getCause().getMessage(), result.getCause());
        }
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo(user.getRole().getName());
        verify(accountManager).createAccount(eq(PROVIDER_USER_INFO));
        verify(accountManager).createProjectUser(eq(PROVIDER_USER_INFO));
        //        verify(accountManager)
        //            .configureAccessGroups(eq(PROVIDER_USER_INFO), eq(List.ofAll(ACCESS_SETTINGS.getDefaultGroups())));
        verify(accountManager).createUserWithAccountAndGroups(eq(PROVIDER_USER_INFO));
        verifyNoMoreInteractions(accountManager);
    }
}
