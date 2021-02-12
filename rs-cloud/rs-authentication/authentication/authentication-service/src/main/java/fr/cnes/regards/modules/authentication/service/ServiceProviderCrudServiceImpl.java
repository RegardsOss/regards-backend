package fr.cnes.regards.modules.authentication.service;

import fr.cnes.regards.modules.authentication.domain.data.ServiceProvider;
import fr.cnes.regards.modules.authentication.domain.repository.IServiceProviderRepository;
import fr.cnes.regards.modules.authentication.domain.service.IServiceProviderCrudService;
import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import io.vavr.control.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ServiceProviderCrudServiceImpl implements IServiceProviderCrudService {

    private final IServiceProviderRepository repository;

    @Autowired
    public ServiceProviderCrudServiceImpl(IServiceProviderRepository repository) {
        this.repository = repository;
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
    public Try<ServiceProvider> save(ServiceProvider serviceProvider) {
        return Try.of(() -> repository.save(serviceProvider));
    }

    @Override
    public Try<Unit> delete(String name) {
        return Try.of(() -> repository.delete(name));
    }
}
