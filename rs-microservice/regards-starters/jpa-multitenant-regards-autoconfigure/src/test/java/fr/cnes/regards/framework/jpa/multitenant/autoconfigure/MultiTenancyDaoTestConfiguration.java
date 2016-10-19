/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.stub.ProjectClientStub;
import fr.cnes.regards.modules.project.client.IProjectsClient;

/**
 *
 * Class MultiTenancyDaoTestConfiguration
 *
 * Configuration class for DAO unit tests
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@EnableAutoConfiguration
@PropertySource("classpath:dao.properties")
public class MultiTenancyDaoTestConfiguration {

    @Bean
    public IProjectsClient projectClient() {
        return new ProjectClientStub();
    }

}
