package fr.cnes.regards.modules.authentication.domain.service;

import fr.cnes.regards.modules.authentication.domain.data.ServiceProvider;
import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import io.vavr.control.Try;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IServiceProviderCrudService {

    Try<Page<ServiceProvider>> findAll(Pageable pageable);

    Try<ServiceProvider> findByName(String name);

    Try<ServiceProvider> save(ServiceProvider serviceProvider);

    Try<Unit> delete(String name);
}
