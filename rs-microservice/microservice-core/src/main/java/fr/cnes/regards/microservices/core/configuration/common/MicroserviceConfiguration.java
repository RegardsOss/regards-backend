/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.configuration.common;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 *
 * POJO for microservice configuration
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
@ConfigurationProperties(prefix = "microservice")
@Primary
public class MicroserviceConfiguration {

    /**
     * Projects configurations
     */
    private final List<ProjectConfiguration> projects_ = new ArrayList<>();

    /**
     * Common DAO Configuration
     */
    @NestedConfigurationProperty
    private CommonDaoConfiguration dao_;

    public List<ProjectConfiguration> getProjects() {
        return this.projects_;
    }

    public CommonDaoConfiguration getDao() {
        return dao_;
    }

    public void setDao(CommonDaoConfiguration pDao) {
        dao_ = pDao;
    }

}
