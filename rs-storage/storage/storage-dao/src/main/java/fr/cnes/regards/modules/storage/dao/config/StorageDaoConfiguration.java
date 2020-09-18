package fr.cnes.regards.modules.storage.dao.config;

import fr.cnes.regards.modules.storage.dao.DownloadQuotaRepositoryImpl;
import fr.cnes.regards.modules.storage.dao.entity.mapping.DomainEntityMapperImpl;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = {
    DomainEntityMapperImpl.class,
    DownloadQuotaRepositoryImpl.class
})
public class StorageDaoConfiguration {
}
