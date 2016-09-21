/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.jpa;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.cfg.Environment;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import fr.cnes.regards.microservices.core.configuration.common.MicroserviceConfiguration;

/**
 *
 * Configuration class to define hibernate/jpa multitenancy strategy
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
@EnableConfigurationProperties(JpaProperties.class)
public class MultiTenancyJpaConfiguration {

    private static final String PACKAGES_TO_SCAN = "fr.cnes.regards";

    /**
     * Default datasource to connect
     */
    @Autowired
    @Qualifier("dataSources")
    private Map<String, DataSource> dataSources_;

    @Autowired
    private MicroserviceConfiguration configuration_;

    @Autowired
    private JpaProperties jpaProperties_;

    @Autowired
    private MultiTenantConnectionProvider multiTenantConnectionProvider_;

    /**
     * Tenant resolver.
     */
    @Autowired
    private CurrentTenantIdentifierResolver currentTenantIdentifierResolver_;

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder) {
        // Use the first dataSource configuration to init the entityManagerFactory
        if ((dataSources_ != null) && !dataSources_.isEmpty()) {
            DataSource defaultDataSource = dataSources_.values().iterator().next();

            Map<String, Object> hibernateProps = new LinkedHashMap<>();
            hibernateProps.putAll(jpaProperties_.getHibernateProperties(defaultDataSource));

            hibernateProps.put(Environment.MULTI_TENANT, MultiTenancyStrategy.DATABASE);
            hibernateProps.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider_);
            hibernateProps.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantIdentifierResolver_);
            hibernateProps.put(Environment.DIALECT, configuration_.getDao().getDialect());

            return builder.dataSource(defaultDataSource).packages(PACKAGES_TO_SCAN).properties(hibernateProps)
                    .jta(false).build();
        }
        else {
            return null;
        }
    }
}
