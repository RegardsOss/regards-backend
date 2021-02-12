package fr.cnes.regards.modules.authentication.dao.repository;

import com.google.common.annotations.VisibleForTesting;
import fr.cnes.regards.modules.authentication.dao.entity.mapping.DomainEntityMapper;
import fr.cnes.regards.modules.authentication.domain.data.ServiceProvider;
import fr.cnes.regards.modules.authentication.domain.repository.IServiceProviderRepository;
import fr.cnes.regards.modules.authentication.domain.utils.fp.Unit;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ServiceProviderRepositoryImpl implements IServiceProviderRepository {

    private final IServiceProviderEntityRepository delegate;

    private final DomainEntityMapper mapper;

    @Autowired
    public ServiceProviderRepositoryImpl(
            IServiceProviderEntityRepository delegate,
            DomainEntityMapper mapper
    ) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public Option<ServiceProvider> findByName(String name) {
        return Option.ofOptional(delegate.findOneByName(name))
            .map(mapper::toDomain);
    }

    @Override
    public Page<ServiceProvider> findAll(Pageable pageable) {
        return delegate.findAll(pageable)
            .map(mapper::toDomain);
    }

    @Override
    public ServiceProvider save(ServiceProvider serviceProvider) {
        return
            mapper.toDomain(
                delegate.save(
                    mapper.toEntity(serviceProvider)
                )
            );
    }

    @Override
    public Unit delete(String name) {
        delegate.deleteByName(name);
        return Unit.UNIT;
    }

    @VisibleForTesting
    @Override
    public void deleteAll() {
        delegate.deleteAll();
    }
}
