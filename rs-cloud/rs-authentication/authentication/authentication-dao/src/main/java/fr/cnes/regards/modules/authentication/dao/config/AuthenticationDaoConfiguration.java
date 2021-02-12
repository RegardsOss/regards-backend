package fr.cnes.regards.modules.authentication.dao.config;

import fr.cnes.regards.modules.authentication.dao.entity.mapping.DomainEntityMapperImpl;
import fr.cnes.regards.modules.authentication.dao.repository.ServiceProviderRepositoryImpl;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = {
    DomainEntityMapperImpl.class,
    ServiceProviderRepositoryImpl.class
})
public class AuthenticationDaoConfiguration {
}
