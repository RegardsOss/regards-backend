/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
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
@ComponentScan(
        basePackages = { "fr.cnes.regards.framework.jpa.multitenant", "fr.cnes.regards.framework.security.utils" })
@EnableAutoConfiguration
@PropertySource("classpath:dao-transaction.properties")
public class DaoTransactionTestConfiguration {

    @Bean
    public IProjectsClient projectClient() {
        return new ProjectClientStub();
    }

}
