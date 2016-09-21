package fr.cnes.regards.microservices.core.configuration.common;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConfigurationProperties(prefix = "microservice")
@Primary
public class MicroserviceConfiguration {

    private final List<ProjectConfiguration> projects = new ArrayList<ProjectConfiguration>();

    @NestedConfigurationProperty
    private CommonDaoConfiguration dao;

    public List<ProjectConfiguration> getProjects() {
        return this.projects;
    }

    public CommonDaoConfiguration getDao() {
        return dao;
    }

    public void setDao(CommonDaoConfiguration pDao) {
        dao = pDao;
    }

}
