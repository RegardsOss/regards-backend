/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.jpa;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.sql.DataSource;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.cfg.Environment;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import fr.cnes.regards.microservices.core.configuration.common.MicroserviceConfiguration;
import fr.cnes.regards.microservices.core.dao.annotation.InstanceEntity;

/**
 *
 * Configuration class to define hibernate/jpa multitenancy databases strategy
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
@EnableConfigurationProperties(JpaProperties.class)
@EnableJpaRepositories(excludeFilters = {
        @ComponentScan.Filter(value = InstanceEntity.class, type = FilterType.ANNOTATION) }, basePackages = DaoUtils.PACKAGES_TO_SCAN, entityManagerFactoryRef = "projectsEntityManagerFactory", transactionManagerRef = "projectsJpaTransactionManager")
@ConditionalOnProperty("microservice.dao.enabled")
public class MultiTenancyJpaConfiguration {

    /**
     * JPA Persistence unit name. Used to separate multiples databases
     */
    private static final String PERSITENCE_UNIT_NAME = "projects";

    /**
     * Datasources pool
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
    public JpaTransactionManager projectsJpaTransactionManager() {
        JpaTransactionManager jtm = new JpaTransactionManager();
        jtm.setPersistenceUnitName("projects");
        return jtm;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean projectsEntityManagerFactory(EntityManagerFactoryBuilder builder) {
        // Use the first dataSource configuration to init the entityManagerFactory
        DataSource defaultDataSource = dataSources_.values().iterator().next();

        Map<String, Object> hibernateProps = new LinkedHashMap<>();
        hibernateProps.putAll(jpaProperties_.getHibernateProperties(defaultDataSource));

        hibernateProps.put(Environment.MULTI_TENANT, MultiTenancyStrategy.DATABASE);
        hibernateProps.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider_);
        hibernateProps.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantIdentifierResolver_);
        if (configuration_.getDao().getEmbedded()) {
            hibernateProps.put(Environment.DIALECT, DataSourcesConfiguration.EMBEDDED_HSQLDB_HIBERNATE_DIALECT);
        }
        else {
            hibernateProps.put(Environment.DIALECT, configuration_.getDao().getDialect());
        }

        // Find classpath for each Entity and not NonStandardEntity
        List<Class<?>> packages = DaoUtils.scanForJpaPackages(DaoUtils.PACKAGES_TO_SCAN, Entity.class,
                                                              InstanceEntity.class);

        if (packages.size() > 1) {
            return builder.dataSource(defaultDataSource).persistenceUnit(PERSITENCE_UNIT_NAME)
                    .packages(packages.toArray(new Class[packages.size()])).properties(hibernateProps).jta(false)
                    .build();
        }
        else {
            return builder.dataSource(defaultDataSource).persistenceUnit(PERSITENCE_UNIT_NAME).packages(packages.get(0))
                    .properties(hibernateProps).jta(false).build();
        }

    }
}
