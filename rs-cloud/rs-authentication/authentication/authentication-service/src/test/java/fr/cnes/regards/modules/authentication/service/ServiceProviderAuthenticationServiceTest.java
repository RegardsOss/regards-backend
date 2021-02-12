package fr.cnes.regards.modules.authentication.service;

import fr.cnes.regards.framework.gson.annotation.GsonDiscriminator;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.modules.authentication.domain.exception.serviceprovider.ServiceProviderPluginIllegalParameterException;
import fr.cnes.regards.modules.authentication.domain.plugin.IServiceProviderPlugin;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationInfo;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationParams;
import fr.cnes.regards.modules.authentication.domain.repository.IServiceProviderRepository;
import fr.cnes.regards.modules.authentication.domain.service.IUserAccountManager;
import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import io.vavr.Tuple;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

public class ServiceProviderAuthenticationServiceTest {

    private static final String TENANT = "default";

    private static final String PROVIDER_NAME = "foo";

    private static final ServiceProviderAuthenticationInfo.UserInfo PROVIDER_USER_INFO =
        new ServiceProviderAuthenticationInfo.UserInfo.Builder()
            .withEmail("email")
            .withFirstname("firstname")
            .withLastname("lastname")
            .addMetadata("meta", "data")
            .build();

    private static final Map<String, String> PROVIDER_AUTH_INFO =
        HashMap.of("token", "dummy");

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

        doNothing()
            .when(runtimeTenantResolver)
            .forceTenant(anyString());
        doReturn(TENANT)
            .when(runtimeTenantResolver)
            .getTenant();

        service = spy(new ServiceProviderAuthenticationServiceImpl(
            repository,
            pluginService,
            runtimeTenantResolver,
            userAccountManager,
            jwtService
        ));
    }

    @Test
    public void authenticate_fails_when_getPlugin_fails() {
        RuntimeException expected = new RuntimeException("Expected");

        doReturn(Try.failure(expected))
            .when(service).getPlugin(PROVIDER_NAME);

        Try<String> token = service.authenticate(PROVIDER_NAME, new ServiceProviderAuthenticationParamsMock());

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
            @Override public Try<ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfoMock>> authenticate(ServiceProviderAuthenticationParamsMock params) { return null; }
            @Override public Try<Unit> deauthenticate(Map<String, Object> jwtClaims) { return null; }
        })).when(service).getPlugin(PROVIDER_NAME);

        Try<String> token = service.authenticate(PROVIDER_NAME, new ServiceProviderAuthenticationParamsMock2());

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
            public Try<ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfoMock>> authenticate(ServiceProviderAuthenticationParamsMock params) {
                throw expected;
            }
            @Override public Try<Unit> deauthenticate(Map<String, Object> jwtClaims) { return null; }
        })).when(service).getPlugin(PROVIDER_NAME);

        Try<String> token = service.authenticate(PROVIDER_NAME, new ServiceProviderAuthenticationParamsMock());

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
            public Try<ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfoMock>> authenticate(ServiceProviderAuthenticationParamsMock params) {
                return Try.success(new ServiceProviderAuthenticationInfo<>(PROVIDER_USER_INFO, ignored));
            }
            @Override public Try<Unit> deauthenticate(Map<String, Object> jwtClaims) { return null; }
        })).when(service).getPlugin(PROVIDER_NAME);
        when(userAccountManager.createUserWithAccountAndGroups(PROVIDER_NAME, PROVIDER_USER_INFO))
            .thenReturn(Try.failure(expected));

        Try<String> token = service.authenticate(PROVIDER_NAME, new ServiceProviderAuthenticationParamsMock());

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
            public Try<ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfoMock>> authenticate(ServiceProviderAuthenticationParamsMock params) {
                ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfoMock> authInfo =
                    new ServiceProviderAuthenticationInfo<>(
                        PROVIDER_USER_INFO,
                        new ServiceProviderAuthenticationInfoMock()
                    );
                return Try.success(authInfo);
            }
            @Override public Try<Unit> deauthenticate(Map<String, Object> jwtClaims) { return null; }
        })).when(service).getPlugin(PROVIDER_NAME);
        when(userAccountManager.createUserWithAccountAndGroups(PROVIDER_NAME, PROVIDER_USER_INFO))
            .thenReturn(Try.success(Tuple.of(PROVIDER_USER_INFO, DefaultRole.REGISTERED_USER.toString())));
        when(jwtService.generateToken(eq(TENANT), anyString(), anyString(), anyString(), any()))
            .thenReturn("token");

        Try<String> token = service.authenticate(PROVIDER_NAME, new ServiceProviderAuthenticationParamsMock());

        assertThat(token.isSuccess()).isTrue();
        assertThat(token.get()).isEqualTo("token");

        ArgumentCaptor<String> tenantArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> emailArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> roleArgumentCaptor = ArgumentCaptor.forClass(String.class);
        //noinspection unchecked
        ArgumentCaptor<java.util.Map<String, Object>> metadataArgumentCaptor = ArgumentCaptor.forClass(java.util.Map.class);
        verify(jwtService).generateToken(
            tenantArgumentCaptor.capture(),
            userArgumentCaptor.capture(),
            emailArgumentCaptor.capture(),
            roleArgumentCaptor.capture(),
            metadataArgumentCaptor.capture()
        );
        assertThat(tenantArgumentCaptor.getValue()).isEqualTo(TENANT);
        assertThat(userArgumentCaptor.getValue()).isEqualTo(PROVIDER_USER_INFO.getEmail());
        assertThat(emailArgumentCaptor.getValue()).isEqualTo(PROVIDER_USER_INFO.getEmail());
        assertThat(roleArgumentCaptor.getValue()).isEqualTo(DefaultRole.REGISTERED_USER.toString());
        assertThat(metadataArgumentCaptor.getValue())
            .containsAllEntriesOf(PROVIDER_USER_INFO.getMetadata().toJavaMap());

        // ensure any authentication info returned by the plugin is stored into the token
        assertThat(metadataArgumentCaptor.getValue())
            .containsAllEntriesOf(PROVIDER_AUTH_INFO.toJavaMap());
    }

    @Test
    public void deauthenticate_fails_when_getPlugin_fails() {
        RuntimeException expected = new RuntimeException("Expected");

        doReturn(Try.failure(expected))
            .when(service).getPlugin(PROVIDER_NAME);

        Try<Unit> result = service.deauthenticate(PROVIDER_NAME);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isEqualTo(expected);
    }

    @Test
    public void deauthenticate_fails_when_plugin_deauthentication_fails() {
        RuntimeException expected = new RuntimeException("Expected");

        doReturn(Try.success(new IServiceProviderPlugin<ServiceProviderAuthenticationParamsMock, ServiceProviderAuthenticationInfoMock>() {
            @Override public Class<ServiceProviderAuthenticationParamsMock> getAuthenticationParamsType() { return null; }
            @Override public Try<ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfoMock>> authenticate(ServiceProviderAuthenticationParamsMock params) { return null; }
            @Override
            public Try<Unit> deauthenticate(Map<String, Object> jwtClaims) {
                throw expected;
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
            @Override public Class<ServiceProviderAuthenticationParamsMock> getAuthenticationParamsType() { return null; }
            @Override public Try<ServiceProviderAuthenticationInfo<ServiceProviderAuthenticationInfoMock>> authenticate(ServiceProviderAuthenticationParamsMock params) { return null; }
            @Override
            public Try<Unit> deauthenticate(Map<String, Object> jwtClaims) {
                return Try.success(Unit.UNIT);
            }
        })).when(service).getPlugin(PROVIDER_NAME);

        Try<Unit> result = service.deauthenticate(PROVIDER_NAME);

        assertThat(result.isSuccess()).isTrue();
    }

    @GsonDiscriminator(ServiceProviderAuthenticationParamsMock.ID)
    private static class ServiceProviderAuthenticationParamsMock extends ServiceProviderAuthenticationParams {

        public static final String ID = "mock_1";

        public ServiceProviderAuthenticationParamsMock() {
            super(ID);
        }
    }

    private static class ServiceProviderAuthenticationInfoMock extends ServiceProviderAuthenticationInfo.AuthenticationInfo {
        @Override
        public Map<String, String> getAuthenticationInfo() {
            return PROVIDER_AUTH_INFO;
        }
    }

    @GsonDiscriminator(ServiceProviderAuthenticationParamsMock2.ID)
    private static class ServiceProviderAuthenticationParamsMock2 extends ServiceProviderAuthenticationParams {

        public static final String ID = "mock_2";

        public ServiceProviderAuthenticationParamsMock2() {
            super(ID);
        }
    }
}
