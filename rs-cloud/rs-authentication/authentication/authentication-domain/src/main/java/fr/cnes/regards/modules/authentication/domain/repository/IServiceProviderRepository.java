package fr.cnes.regards.modules.authentication.domain.repository;

import com.google.common.annotations.VisibleForTesting;
import fr.cnes.regards.modules.authentication.domain.data.ServiceProvider;
import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import io.vavr.control.Option;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IServiceProviderRepository {

    Option<ServiceProvider> findByName(String name);

    Page<ServiceProvider> findAll(Pageable pageable);

    ServiceProvider save(ServiceProvider serviceProvider);

    Unit delete(String name);

    @VisibleForTesting
    void deleteAll();
}
