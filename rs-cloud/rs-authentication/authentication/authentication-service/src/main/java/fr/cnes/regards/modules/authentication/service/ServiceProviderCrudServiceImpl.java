package fr.cnes.regards.modules.authentication.service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.authentication.domain.data.ServiceProvider;
import fr.cnes.regards.modules.authentication.domain.repository.IServiceProviderRepository;
import fr.cnes.regards.modules.authentication.domain.service.IServiceProviderCrudService;
import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.OpenIdConnectPlugin;
import io.vavr.control.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@MultitenantTransactional
public class ServiceProviderCrudServiceImpl implements IServiceProviderCrudService {

    private final IServiceProviderRepository repository;

    private final IPluginService pluginService;

    @Autowired
    public ServiceProviderCrudServiceImpl(IServiceProviderRepository repository, IPluginService pluginService) {
        this.repository = repository;
        this.pluginService = pluginService;
    }

    @Override
    public Try<Page<ServiceProvider>> findAll(Pageable pageable) {
        return Try.of(() -> repository.findAll(pageable));
    }

    @Override
    public Try<ServiceProvider> findByName(String name) {
        return repository.findByName(name).toTry();
    }

    @Override
    public Try<ServiceProvider> save(final ServiceProvider serviceProvider) {
        return Try.of(() ->  {
            PluginConfiguration configuration = serviceProvider.getConfiguration();
            String name = serviceProvider.getName();
            configuration.setBusinessId(name);
            configuration.setLabel(name);
            configuration = pluginService.savePluginConfiguration(configuration);
            return new ServiceProvider(
                name,
                serviceProvider.getAuthUrl(),
                serviceProvider.getLogoutUrl(),
                configuration
            );
        }).map(repository::save);
    }

    @Override
    public Try<ServiceProvider> update(String name, final ServiceProvider serviceProvider) {
        return repository.findByName(name).toTry()
            .mapTry(sp ->  {
                PluginConfiguration configuration = serviceProvider.getConfiguration();
                configuration = pluginService.updatePluginConfiguration(configuration);
                return new ServiceProvider(
                    name,
                    serviceProvider.getAuthUrl(),
                    serviceProvider.getLogoutUrl(),
                    configuration
                );
            }).map(repository::save);
    }

    @Override
    public Try<Unit> delete(String name) {
        return Try.of(() -> repository.delete(name))
            .map(u -> {
                try {
                    if (pluginService.exists(OpenIdConnectPlugin.ID)) {
                        pluginService.deletePluginConfiguration(name);
                    }
                } catch (ModuleException e) {
                    // ignored, the service provider has been deleted
                }
                return u;
            });
    }
}
