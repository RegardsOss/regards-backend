package fr.cnes.regards.modules.authentication.service;

import fr.cnes.regards.framework.encryption.IEncryptionService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.authentication.domain.data.ServiceProvider;
import fr.cnes.regards.modules.authentication.domain.repository.IServiceProviderRepository;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.OpenIdConnectPlugin;
import io.vavr.control.Try;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = {
    "spring.jpa.properties.hibernate.default_schema=authentication_service_provider_tests", })
public class ServiceProviderCrudServiceIT extends AbstractRegardsTransactionalIT {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private IServiceProviderRepository repository;

    private IServiceProviderRepository repositoryDelegate;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    @InjectMocks
    private ServiceProviderCrudServiceImpl service;

    @Autowired
    private IEncryptionService encryptionService;

    @Autowired
    private IPluginService pluginService;

    @Before
    public void setUp() throws ModuleException {
        MockitoAnnotations.initMocks(this);
        repositoryDelegate = repository;
        repository = Mockito.mock(IServiceProviderRepository.class, AdditionalAnswers.delegatesTo(repository));
        ReflectionTestUtils.setField(service, "repository", repository);

        runtimeTenantResolver.forceTenant(getDefaultTenant());

        repository.deleteAll();
        if (pluginService.exists(OpenIdConnectPlugin.ID)) {
            pluginService.deletePluginConfiguration(OpenIdConnectPlugin.ID);
        }

        Mockito.clearInvocations(repository);
    }

    @Test
    public void findAll_ok_when_repository_is_success() {
        ServiceProvider stub = getServiceProvider();
        List<ServiceProvider> expected = Collections.singletonList(repository.save(stub));

        Try<Page<ServiceProvider>> actual = service.findAll(Pageable.unpaged());

        assertThat(actual.isSuccess()).isTrue();
        assertThat(actual.get().getContent()).isEqualTo(expected);
    }

    @Test
    public void findAll_nok_when_repository_is_failure() {
        Exception expected = new RuntimeException("Expected.");
        when(repository.findAll(any())).thenThrow(expected);

        Try<Page<ServiceProvider>> actual = service.findAll(null);

        assertThat(actual.isFailure()).isTrue();
        assertThat(actual.getCause()).isEqualTo(expected);
    }

    @Test
    public void findByName_ok_when_repository_finds_entity() {
        ServiceProvider stub = getServiceProvider();
        ServiceProvider expected = repository.save(stub);

        Try<ServiceProvider> actual = service.findByName(stub.getName());

        assertThat(actual.isSuccess()).isTrue();
        assertThat(actual.get()).isEqualTo(expected);
    }

    @Test
    public void findByName_nok_when_entity_not_found() {
        ServiceProvider stub = getServiceProvider();
        Try<ServiceProvider> actual = service.findByName(stub.getName());

        assertThat(actual.isFailure()).isTrue();
        assertThat(actual.getCause()).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    public void save_ok_when_repository_is_success() {
        ServiceProvider stub = getServiceProvider();
        Try<ServiceProvider> actual = service.save(stub);

        assertThat(actual.isSuccess()).isTrue();
        assertThat(actual.get()).isEqualTo(stub);
    }

    @Test
    public void save_nok_when_repository_is_failure() {
        ServiceProvider stub = getServiceProvider();
        Exception expected = new RuntimeException("Expected.");
        doThrow(expected).when(repository).save(any());

        Try<ServiceProvider> actual = service.save(stub);

        assertThat(actual.isFailure()).isTrue();
        assertThat(actual.getCause()).isEqualTo(expected);
    }

    @Test
    public void update_ok_when_repository_is_success() {
        ServiceProvider stub = getServiceProvider();
        ServiceProvider saved = service.save(stub).get();

        ServiceProvider toUpdate = new ServiceProvider(saved.getName(),
                                                       "https://chronos.fr/sso/auth.do",
                                                       "https://chronos.fr/sso/logout.do",
                                                       saved.getConfiguration(),
                                                       "descriptionFr",
                                                       "descriptionEn");

        Try<ServiceProvider> updated = service.update(saved.getName(), toUpdate);

        assertThat(updated.isSuccess()).isTrue();
        assertThat(updated.get()).isEqualTo(toUpdate);
    }

    private ServiceProvider getServiceProvider() {
        try {
            // Set all parameters
            Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(OpenIdConnectPlugin.OPENID_CLIENT_ID,
                                                                               "I"),
                                                            IPluginParam.build(OpenIdConnectPlugin.OPENID_CLIENT_SECRET,
                                                                               "Don't"),
                                                            IPluginParam.build(OpenIdConnectPlugin.OPENID_REDIRECT_URI,
                                                                               "Feel"),
                                                            IPluginParam.build(OpenIdConnectPlugin.OPENID_TOKEN_ENDPOINT,
                                                                               "Feel"),
                                                            IPluginParam.build(OpenIdConnectPlugin.OPENID_USER_INFO_ENDPOINT,
                                                                               "Like"),
                                                            IPluginParam.build(OpenIdConnectPlugin.OPENID_USER_INFO_EMAIL_MAPPING,
                                                                               "Dancin'"),
                                                            IPluginParam.build(OpenIdConnectPlugin.OPENID_USER_INFO_FIRSTNAME_MAPPING,
                                                                               "Dancin'"),
                                                            IPluginParam.build(OpenIdConnectPlugin.OPENID_USER_INFO_LASTNAME_MAPPING,
                                                                               "Dancin'"),
                                                            IPluginParam.build(OpenIdConnectPlugin.OPENID_REVOKE_ENDPOINT,
                                                                               "When the old Joanna plays"));

            PluginConfiguration conf = PluginConfiguration.build(OpenIdConnectPlugin.class,
                                                                 OpenIdConnectPlugin.ID,
                                                                 parameters);
            conf.setBusinessId(OpenIdConnectPlugin.ID);
            conf.setVersion(OpenIdConnectPlugin.VERSION);
            return new ServiceProvider(OpenIdConnectPlugin.ID,
                                       "https://sso.theia-land.fr/login",
                                       "https://sso.theia-land.fr/logout",
                                       conf,
                                       "descriptionFr",
                                       "descriptionEn");
        } catch (Exception e) {
            Assert.fail();
            return null; // never reached, dummy
        }
    }
}
