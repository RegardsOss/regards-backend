package fr.cnes.regards.modules.authentication.dao.entity.mapping;

import fr.cnes.regards.modules.authentication.dao.entity.ServiceProviderEntity;
import fr.cnes.regards.modules.authentication.domain.data.ServiceProvider;

public interface DomainEntityMapper {

    ServiceProviderEntity toEntity(ServiceProvider serviceProvider);

    ServiceProvider toDomain(ServiceProviderEntity serviceProvider);
}

