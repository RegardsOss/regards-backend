package fr.cnes.regards.modules.authentication.service;

import com.google.gson.Gson;
import fr.cnes.regards.framework.encryption.IEncryptionService;
import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.modules.plugins.service.PluginMetadataNotFoundRuntimeException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.authentication.domain.data.ServiceProvider;
import fr.cnes.regards.modules.authentication.domain.plugin.IServiceProviderPlugin;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationInfo;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationParams;
import fr.cnes.regards.modules.authentication.domain.repository.IServiceProviderRepository;
import fr.cnes.regards.modules.authentication.domain.service.IUserAccountManager;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.OpenIdConnectPlugin;
import io.vavr.control.Try;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.spy;

@TestPropertySource(
    properties = {
        "spring.jpa.properties.hibernate.default_schema=authentication_service_provider_tests",
    }
)
public class ServiceProviderAuthenticationServiceIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private IEncryptionService encryptionService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private IServiceProviderRepository repository;

    private IServiceProviderRepository repositoryDelegate;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IPluginService pluginService;

    private IPluginService pluginServiceDelegate;

    @Mock
    private IUserAccountManager userAccountManager;

    private ServiceProviderAuthenticationServiceImpl service;

    @Autowired
    private Gson gson;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        repositoryDelegate = repository;
        repository =
            Mockito.mock(IServiceProviderRepository.class, AdditionalAnswers.delegatesTo(repository));

        pluginServiceDelegate = pluginService;
        pluginService =
            Mockito.mock(IPluginService.class, AdditionalAnswers.delegatesTo(pluginService));

        runtimeTenantResolver.forceTenant(getDefaultTenant());

        service = spy(new ServiceProviderAuthenticationServiceImpl(
            repository,
            pluginService,
            runtimeTenantResolver,
            userAccountManager,
            jwtService
        ));
    }

    @Test
    public void testSomething() {
        JWTAuthentication auth = new JWTAuthentication("plop");
        System.out.println(gson.toJson(auth));
    }

    @After
    public void tearDown() {
        repository.deleteAll();
    }

    @Test
    public void getPlugin_ok() throws ModuleException {
        ServiceProvider sp = repository.save(makeServiceProviderOk());
        System.out.println(gson.toJson(sp));

        Try<IServiceProviderPlugin<ServiceProviderAuthenticationParams, ServiceProviderAuthenticationInfo.AuthenticationInfo>> plugin = service.getPlugin(sp.getName());
        assertThat(plugin.isSuccess()).isTrue();
        assertThat(plugin.get()).isInstanceOf(OpenIdConnectPlugin.class);
    }

    @Test
    public void getPlugin_nok() {
        ServiceProvider sp = repository.save(makeServiceProviderNok());
        Try<IServiceProviderPlugin<ServiceProviderAuthenticationParams, ServiceProviderAuthenticationInfo.AuthenticationInfo>> plugin = service.getPlugin(sp.getName());
        assertThat(plugin.isFailure()).isTrue();
        assertThat(plugin.getCause()).isInstanceOf(PluginMetadataNotFoundRuntimeException.class);
    }

    private ServiceProvider makeServiceProviderOk() throws EncryptionException {
        PluginConfiguration pluginConf = new PluginConfiguration(
            "THEIA",
            OpenIdConnectPlugin.ID
        );
        pluginConf.setVersion(OpenIdConnectPlugin.VERSION);
        Set<IPluginParam> parameters = IPluginParam
            .set(
                IPluginParam.build(OpenIdConnectPlugin.OPENID_CLIENT_ID, "I"),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_CLIENT_SECRET, encryptionService.encrypt("Don't")),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_TOKEN_ENDPOINT, "Feel"),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_USER_INFO_ENDPOINT, "Like"),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_USER_INFO_EMAIL_MAPPING, "Dancin'"),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_USER_INFO_FIRSTNAME_MAPPING, "Dancin'"),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_USER_INFO_LASTNAME_MAPPING, "Dancin'"),
                IPluginParam.build(OpenIdConnectPlugin.OPENID_REVOKE_ENDPOINT, "Rather be home with no-one if I can't get down with you-ou-ou")
            );
        pluginConf.setParameters(parameters);
        return new ServiceProvider(
            "THEIA",
            "https://sso.theia-land.fr/login",
            pluginConf
        );
    }

    private ServiceProvider makeServiceProviderNok() {
        PluginConfiguration pluginConf = new PluginConfiguration(
            "I've",
            "been"
        );
        pluginConf.setVersion("through");
        Set<IPluginParam> parameters = IPluginParam
            .set(
                IPluginParam.build("the", "desert")
            );
        pluginConf.setParameters(parameters);
        return new ServiceProvider(
            "On a horse",
            "with no name",
            pluginConf
        ); // La, la, la, la, la, la
    }
}
