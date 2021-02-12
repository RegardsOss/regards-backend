package fr.cnes.regards.modules.authentication.dao.entity.mapping;

import fr.cnes.regards.modules.authentication.dao.entity.ServiceProviderEntity;
import fr.cnes.regards.modules.authentication.domain.data.ServiceProvider;
import org.springframework.stereotype.Component;

@Component
public class DomainEntityMapperImpl implements DomainEntityMapper {

    @Override
    public ServiceProviderEntity toEntity(ServiceProvider serviceProvider) {
        return new ServiceProviderEntity(
            serviceProvider.getName(),
            serviceProvider.getAuthUrl(),
            serviceProvider.getConfiguration()
        );
    }

    @Override
    public ServiceProvider toDomain(ServiceProviderEntity serviceProvider) {
        return new ServiceProvider(
            serviceProvider.getName(),
            serviceProvider.getAuthUrl(),
            serviceProvider.getConfiguration()
        );
    }
}
