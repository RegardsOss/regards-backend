package fr.cnes.regards.modules.authentication.service;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.authentication.domain.data.ServiceProvider;
import fr.cnes.regards.modules.authentication.domain.repository.IServiceProviderRepository;
import fr.cnes.regards.modules.authentication.service.ServiceProviderCrudServiceImpl;
import io.vavr.control.Try;
import org.junit.After;
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
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@TestPropertySource(
    properties = {
        "spring.jpa.properties.hibernate.default_schema=authentication_service_provider_tests",
    }
)
public class ServiceProviderCrudServiceIT extends AbstractRegardsTransactionalIT {

    public static final PluginConfiguration STUB_CONF;
    static {
        PluginConfiguration pluginConf = new PluginConfiguration(
            "THEIA",
            "THEIA"
        );
        pluginConf.setVersion(UUID.randomUUID().toString());
        STUB_CONF = pluginConf;
    }
    public static final ServiceProvider STUB = new ServiceProvider(
        "THEIA",
        "https://sso.theia-land.fr/login",
        STUB_CONF
    );

    @Autowired
    private IServiceProviderRepository repository;

    private IServiceProviderRepository repositoryDelegate;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired @InjectMocks
    private ServiceProviderCrudServiceImpl service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        repositoryDelegate = repository;
        repository =
            Mockito.mock(IServiceProviderRepository.class, AdditionalAnswers.delegatesTo(repository));
        ReflectionTestUtils.setField(service, "repository", repository);

        runtimeTenantResolver.forceTenant(getDefaultTenant());
    }

    @After
    public void tearDown() {
        repository.deleteAll();
        Mockito.clearInvocations(repository);
    }

    @Test
    public void findAll_ok_when_repository_is_success() {
        List<ServiceProvider> expected = Collections.singletonList(repository.save(STUB));

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
        ServiceProvider expected = repository.save(STUB);

        Try<ServiceProvider> actual = service.findByName(STUB.getName());

        assertThat(actual.isSuccess()).isTrue();
        assertThat(actual.get()).isEqualTo(expected);
    }

    @Test
    public void findByName_nok_when_entity_not_found() {
        Try<ServiceProvider> actual = service.findByName(STUB.getName());

        assertThat(actual.isFailure()).isTrue();
        assertThat(actual.getCause()).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    public void save_ok_when_repository_is_success() {
        ServiceProvider expected = STUB;
        Try<ServiceProvider> actual = service.save(STUB);

        assertThat(actual.isSuccess()).isTrue();
        assertThat(actual.get()).isEqualTo(expected);
    }

    @Test
    public void save_nok_when_repository_is_failure() {
        Exception expected = new RuntimeException("Expected.");
        when(repository.save(STUB)).thenThrow(expected);

        Try<ServiceProvider> actual = service.save(STUB);

        assertThat(actual.isFailure()).isTrue();
        assertThat(actual.getCause()).isEqualTo(expected);
    }
}
