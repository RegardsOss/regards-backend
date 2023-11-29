package fr.cnes.regards.modules.authentication.service;

import fr.cnes.regards.framework.gson.annotation.GsonDiscriminator;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.modules.authentication.domain.data.Authentication;
import fr.cnes.regards.modules.authentication.domain.data.ServiceProvider;
import fr.cnes.regards.modules.authentication.domain.exception.serviceprovider.ServiceProviderPluginIllegalParameterException;
import fr.cnes.regards.modules.authentication.domain.plugin.IServiceProviderPlugin;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationInfo;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationParams;
import fr.cnes.regards.modules.authentication.domain.repository.IServiceProviderRepository;
import fr.cnes.regards.modules.authentication.domain.service.IUserAccountManager;
import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

public class ServiceProviderAuthenticationServiceTest {

    private static final String TENANT = "default";

    private static final String PROVIDER_NAME = "foo";

    private static final ServiceProviderAuthenticationInfo.UserInfo PROVIDER_USER_INFO = new ServiceProviderAuthenticationInfo.UserInfo.Builder().withEmail(
        "email").withFirstname("firstname").withLastname("lastname").addMetadata("meta", "data").build();

    private static final Map<String, String> PROVIDER_AUTH_INFO = HashMap.of("token", "dummy");

    @Mock
    private IServiceProviderRepository repository;

    @Mock
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Mock
    private IPluginService pluginService;

    @Mock
    private IUserAccountManager userAccountManager;

    @Mock
    private JWTService jwtService;

    private ServiceProviderAuthenticationServiceImpl service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doNothing().when(runtimeTenantResolver).forceTenant(anyString());
        doReturn(TENANT).when(runtimeTenantResolver).getTenant();

        service = spy(new ServiceProviderAuthenticationServiceImpl(repository,
                                                                   pluginService,
                                                                   runtimeTenantResolver,
                                                                   userAccountManager,
                                                                   jwtService));
    }

    @Test
    public void authenticate_fails_when_getPlugin_fails() {
        RuntimeException expected = new RuntimeException("Expected");

        doReturn(Try.failure(expected)).when(service).getPlugin(PROVIDER_NAME);

        Try<Authentication> token = service.authenticate(PROVIDER_NAME, new ServiceProviderAuthenticationParamsMock());

        assertThat(token.isFailure()).isTrue();
        assertThat(token.getCause()).isEqualTo(expected);
    }

    @Test
    public void authenticate_fails_when_plugin_params_dont_match_authentication_call_params() {
        doReturn(Try.success(new IServiceProviderPlugin<ServiceProviderAuthenticationParamsMock, ServiceProviderAuthenticationInfoMock>() {

            @Override
            public Class<ServiceProviderAuthenticationParamsMock> getAuthenticationParamsType() {
                return ServiceProviderAuthenticationParamsMock.class;
            }

            @Override
            public Try<ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfoMock>> authenticate(
                ServiceProviderAuthenticationParamsMock params) {
                return null;
            }

            @Override
            public Try<Unit> deauthenticate(Map<String, Object> jwtClaims) {
                return null;
            }

            @Override
            public Try<ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfoMock>> verify(String token) {
                return null;
            }
        })).when(service).getPlugin(PROVIDER_NAME);

        Try<Authentication> token = service.authenticate(PROVIDER_NAME, new ServiceProviderAuthenticationParamsMock2());

        assertThat(token.isFailure()).isTrue();
        assertThat(token.getCause()).isInstanceOf(ServiceProviderPluginIllegalParameterException.class);
    }

    @Test
    public void authenticate_fails_when_plugin_authentication_fails() {
        RuntimeException expected = new RuntimeException("Expected");

        doReturn(Try.success(new IServiceProviderPlugin<ServiceProviderAuthenticationParamsMock, ServiceProviderAuthenticationInfoMock>() {

            @Override
            public Class<ServiceProviderAuthenticationParamsMock> getAuthenticationParamsType() {
                return ServiceProviderAuthenticationParamsMock.class;
            }

            @Override
            public Try<ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfoMock>> authenticate(
                ServiceProviderAuthenticationParamsMock params) {
                throw expected;
            }

            @Override
            public Try<Unit> deauthenticate(Map<String, Object> jwtClaims) {
                return null;
            }

            @Override
            public Try<ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfoMock>> verify(String token) {
                return null;
            }
        })).when(service).getPlugin(PROVIDER_NAME);

        Try<Authentication> token = service.authenticate(PROVIDER_NAME, new ServiceProviderAuthenticationParamsMock());

        assertThat(token.isFailure()).isTrue();
        assertThat(token.getCause()).isEqualTo(expected);
    }

    @Test
    public void authenticate_fails_when_user_creation_fails() {
        RuntimeException expected = new RuntimeException("Expected");
        ServiceProviderAuthenticationInfoMock ignored = null;

        doReturn(Try.success(new IServiceProviderPlugin<ServiceProviderAuthenticationParamsMock, ServiceProviderAuthenticationInfoMock>() {

            @Override
            public Class<ServiceProviderAuthenticationParamsMock> getAuthenticationParamsType() {
                return ServiceProviderAuthenticationParamsMock.class;
            }

            @Override
            public Try<ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfoMock>> authenticate(
                ServiceProviderAuthenticationParamsMock params) {
                return Try.success(new ServiceProviderAuthenticationInfo<>(PROVIDER_USER_INFO, ignored));
            }

            @Override
            public Try<Unit> deauthenticate(Map<String, Object> jwtClaims) {
                return null;
            }

            @Override
            public Try<ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfoMock>> verify(String token) {
                return null;
            }
        })).when(service).getPlugin(PROVIDER_NAME);
        when(userAccountManager.createUserWithAccountAndGroups(PROVIDER_USER_INFO,
                                                               PROVIDER_NAME)).thenReturn(Try.failure(expected));

        Try<Authentication> token = service.authenticate(PROVIDER_NAME, new ServiceProviderAuthenticationParamsMock());

        assertThat(token.isFailure()).isTrue();
        assertThat(token.getCause()).isEqualTo(expected);
    }

    @Test
    public void authenticate_succeeds_when_all_is_well() {
        doReturn(Try.success(new IServiceProviderPlugin<ServiceProviderAuthenticationParamsMock, ServiceProviderAuthenticationInfoMock>() {

            @Override
            public Class<ServiceProviderAuthenticationParamsMock> getAuthenticationParamsType() {
                return ServiceProviderAuthenticationParamsMock.class;
            }

            @Override
            public Try<ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfoMock>> authenticate(
                ServiceProviderAuthenticationParamsMock params) {
                ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfoMock> authInfo = new ServiceProviderAuthenticationInfo<>(
                    PROVIDER_USER_INFO,
                    new ServiceProviderAuthenticationInfoMock());
                return Try.success(authInfo);
            }

            @Override
            public Try<Unit> deauthenticate(Map<String, Object> jwtClaims) {
                return null;
            }

            @Override
            public Try<ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfoMock>> verify(String token) {
                return null;
            }
        })).when(service).getPlugin(PROVIDER_NAME);
        when(userAccountManager.createUserWithAccountAndGroups(PROVIDER_USER_INFO,
                                                               PROVIDER_NAME)).thenReturn(Try.success(DefaultRole.REGISTERED_USER.toString()));
        OffsetDateTime expirationDate = OffsetDateTime.now();
        when(jwtService.getExpirationDate(any())).thenReturn(expirationDate);
        when(jwtService.generateToken(eq(TENANT),
                                      anyString(),
                                      anyString(),
                                      anyString(),
                                      eq(expirationDate),
                                      any())).thenReturn("token");

        Try<Authentication> token = service.authenticate(PROVIDER_NAME, new ServiceProviderAuthenticationParamsMock());

        assertThat(token.isSuccess()).isTrue();
        assertThat(token.get().getAccessToken()).isEqualTo("token");

        ArgumentCaptor<String> tenantArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> emailArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> roleArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OffsetDateTime> expirationDateArgumentCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        //noinspection unchecked
        ArgumentCaptor<java.util.Map<String, Object>> metadataArgumentCaptor = ArgumentCaptor.forClass(java.util.Map.class);
        verify(jwtService).generateToken(tenantArgumentCaptor.capture(),
                                         userArgumentCaptor.capture(),
                                         emailArgumentCaptor.capture(),
                                         roleArgumentCaptor.capture(),
                                         expirationDateArgumentCaptor.capture(),
                                         metadataArgumentCaptor.capture());
        assertThat(tenantArgumentCaptor.getValue()).isEqualTo(TENANT);
        assertThat(userArgumentCaptor.getValue()).isEqualTo(PROVIDER_USER_INFO.getEmail());
        assertThat(emailArgumentCaptor.getValue()).isEqualTo(PROVIDER_USER_INFO.getEmail());
        assertThat(roleArgumentCaptor.getValue()).isEqualTo(DefaultRole.REGISTERED_USER.toString());
        assertThat(metadataArgumentCaptor.getValue()).containsAllEntriesOf(PROVIDER_USER_INFO.getMetadata()
                                                                                             .toJavaMap());

        // ensure any authentication info returned by the plugin is stored into the token
        assertThat(metadataArgumentCaptor.getValue()).containsAllEntriesOf(PROVIDER_AUTH_INFO.toJavaMap());
    }

    @Test
    public void deauthenticate_fails_when_getPlugin_fails() {
        RuntimeException expected = new RuntimeException("Expected");

        doReturn(Try.failure(expected)).when(service).getPlugin(PROVIDER_NAME);

        Try<Unit> result = service.deauthenticate(PROVIDER_NAME);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isEqualTo(expected);
    }

    @Test
    public void deauthenticate_fails_when_plugin_deauthentication_fails() {
        RuntimeException expected = new RuntimeException("Expected");

        doReturn(Try.success(new IServiceProviderPlugin<ServiceProviderAuthenticationParamsMock, ServiceProviderAuthenticationInfoMock>() {

            @Override
            public Class<ServiceProviderAuthenticationParamsMock> getAuthenticationParamsType() {
                return null;
            }

            @Override
            public Try<ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfoMock>> authenticate(
                ServiceProviderAuthenticationParamsMock params) {
                return null;
            }

            @Override
            public Try<Unit> deauthenticate(Map<String, Object> jwtClaims) {
                throw expected;
            }

            @Override
            public Try<ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfoMock>> verify(String token) {
                return null;
            }
        })).when(service).getPlugin(PROVIDER_NAME);
        JWTAuthentication stubAuthentication = new JWTAuthentication(null);
        SecurityContextHolder.getContext().setAuthentication(stubAuthentication);

        Try<Unit> result = service.deauthenticate(PROVIDER_NAME);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isEqualTo(expected);
    }

    @Test
    public void deauthenticate_succeeds_when_all_is_well() {
        doReturn(Try.success(new IServiceProviderPlugin<ServiceProviderAuthenticationParamsMock, ServiceProviderAuthenticationInfoMock>() {

            @Override
            public Class<ServiceProviderAuthenticationParamsMock> getAuthenticationParamsType() {
                return null;
            }

            @Override
            public Try<ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfoMock>> authenticate(
                ServiceProviderAuthenticationParamsMock params) {
                return null;
            }

            @Override
            public Try<Unit> deauthenticate(Map<String, Object> jwtClaims) {
                return Try.success(Unit.UNIT);
            }

            @Override
            public Try<ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfoMock>> verify(String token) {
                return null;
            }
        })).when(service).getPlugin(PROVIDER_NAME);

        Try<Unit> result = service.deauthenticate(PROVIDER_NAME);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    public void verify_fails_when_no_service_provider_found() {
        when(repository.findAll()).thenReturn(List.empty());

        Try<Authentication> result = service.verifyAndAuthenticate("token");

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isExactlyInstanceOf(InsufficientAuthenticationException.class);
    }

    @Test
    public void verify_fails_when_no_service_provider_plugin_found() {
        when(repository.findAll()).thenReturn(List.of(new ServiceProvider(PROVIDER_NAME,
                                                                          null,
                                                                          null,
                                                                          null,
                                                                          null,
                                                                          null)));

        RuntimeException expected = new RuntimeException("Expected");
        doReturn(Try.failure(expected)).when(service).getPlugin(PROVIDER_NAME);

        Try<Authentication> result = service.verifyAndAuthenticate("token");

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isExactlyInstanceOf(InsufficientAuthenticationException.class);
    }

    @Test
    public void verify_evaluates_all_service_providers_and_short_circuits() {
        String providerName_1 = UUID.randomUUID().toString();
        String providerName_2 = UUID.randomUUID().toString();
        String providerName_3 = UUID.randomUUID().toString();
        when(repository.findAll()).thenReturn(List.of(new ServiceProvider(providerName_1, null, null, null, null, null),
                                                      new ServiceProvider(providerName_2, null, null, null, null, null),
                                                      new ServiceProvider(providerName_3,
                                                                          null,
                                                                          null,
                                                                          null,
                                                                          null,
                                                                          null)));

        RuntimeException expected = new RuntimeException("Expected");

        // Mock plugin 1 verify method to fail
        IServiceProviderPlugin<ServiceProviderAuthenticationParamsMock, ServiceProviderAuthenticationInfoMock> plugin_1 = mock(
            IServiceProviderPlugin.class);
        when(plugin_1.verify(anyString())).thenReturn(Try.failure(expected));
        doReturn(Try.success(plugin_1)).when(service).getPlugin(providerName_1);

        // Mock plugin 2 verify method to succeed
        IServiceProviderPlugin<ServiceProviderAuthenticationParamsMock, ServiceProviderAuthenticationInfoMock> plugin_2 = mock(
            IServiceProviderPlugin.class);
        when(plugin_2.verify(anyString())).thenReturn(Try.success(new ServiceProviderAuthenticationInfo<>(
            PROVIDER_USER_INFO,
            new ServiceProviderAuthenticationInfoMock())));
        doReturn(Try.success(plugin_2)).when(service).getPlugin(providerName_2);

        // Mock plugin 3 which should never be called
        IServiceProviderPlugin<ServiceProviderAuthenticationParamsMock, ServiceProviderAuthenticationInfoMock> plugin_3 = mock(
            IServiceProviderPlugin.class);
        doReturn(Try.success(plugin_3)).when(service).getPlugin(providerName_3);

        when(userAccountManager.createUserWithAccountAndGroups(eq(PROVIDER_USER_INFO), any())).thenReturn(Try.success(
            DefaultRole.REGISTERED_USER.toString()));
        OffsetDateTime expirationDate = OffsetDateTime.now();
        when(jwtService.getExpirationDate(any())).thenReturn(expirationDate);
        when(jwtService.generateToken(eq(TENANT),
                                      anyString(),
                                      anyString(),
                                      anyString(),
                                      eq(expirationDate),
                                      any())).thenReturn("token");

        Try<Authentication> token = service.verifyAndAuthenticate("token");

        verify(plugin_1).verify("token");
        verify(plugin_2).verify("token");
        verifyNoInteractions(plugin_3);

        assertThat(token.isSuccess()).isTrue();
        assertThat(token.get().getAccessToken()).isEqualTo("token");

        ArgumentCaptor<String> tenantArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> emailArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> roleArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OffsetDateTime> expirationDateArgumentCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        //noinspection unchecked
        ArgumentCaptor<java.util.Map<String, Object>> metadataArgumentCaptor = ArgumentCaptor.forClass(java.util.Map.class);
        verify(jwtService).generateToken(tenantArgumentCaptor.capture(),
                                         userArgumentCaptor.capture(),
                                         emailArgumentCaptor.capture(),
                                         roleArgumentCaptor.capture(),
                                         expirationDateArgumentCaptor.capture(),
                                         metadataArgumentCaptor.capture());
        assertThat(tenantArgumentCaptor.getValue()).isEqualTo(TENANT);
        assertThat(userArgumentCaptor.getValue()).isEqualTo(PROVIDER_USER_INFO.getEmail());
        assertThat(emailArgumentCaptor.getValue()).isEqualTo(PROVIDER_USER_INFO.getEmail());
        assertThat(roleArgumentCaptor.getValue()).isEqualTo(DefaultRole.REGISTERED_USER.toString());
        assertThat(metadataArgumentCaptor.getValue()).containsAllEntriesOf(PROVIDER_USER_INFO.getMetadata()
                                                                                             .toJavaMap());

        // ensure any authentication info returned by the plugin is stored into the token
        assertThat(metadataArgumentCaptor.getValue()).containsAllEntriesOf(PROVIDER_AUTH_INFO.toJavaMap());
    }

    @GsonDiscriminator("mock_1")
    private static class ServiceProviderAuthenticationParamsMock extends ServiceProviderAuthenticationParams {

    }

    private static class ServiceProviderAuthenticationInfoMock
        extends ServiceProviderAuthenticationInfo.AuthenticationInfo {

        @Override
        public Map<String, String> getAuthenticationInfo() {
            return PROVIDER_AUTH_INFO;
        }
    }

    @GsonDiscriminator("mock_2")
    private static class ServiceProviderAuthenticationParamsMock2 extends ServiceProviderAuthenticationParams {

    }
}
