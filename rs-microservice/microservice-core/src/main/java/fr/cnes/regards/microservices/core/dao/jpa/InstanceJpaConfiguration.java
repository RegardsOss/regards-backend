/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.jpa;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.cfg.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import fr.cnes.regards.microservices.core.configuration.common.MicroserviceConfiguration;
import fr.cnes.regards.microservices.core.dao.annotation.InstanceEntity;

/**
 *
 * Configuration class to define hibernate/jpa instance database strategy
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
@EnableJpaRepositories(includeFilters = {
        @ComponentScan.Filter(value = InstanceEntity.class, type = FilterType.ANNOTATION) }, basePackages = DaoUtils.PACKAGES_TO_SCAN, entityManagerFactoryRef = "instanceEntityManagerFactory", transactionManagerRef = "instanceJpaTransactionManager")
@EnableTransactionManagement
@ConditionalOnProperty("microservice.dao.instance.enabled")
public class InstanceJpaConfiguration {

    /**
     * JPA Persistence unit name. Used to separate multiples databases
     */
    private static final String PERSITENCE_UNIT_NAME = "instance";

    @Autowired
    private MicroserviceConfiguration configuration_;

    @Autowired
    private JpaProperties jpaProperties_;

    @Autowired
    @Qualifier("instanceDataSource")
    private DataSource instanceDataSource_;

    @Bean
    public PlatformTransactionManager instanceJpaTransactionManager(EntityManagerFactoryBuilder builder) {
        JpaTransactionManager jtm = new JpaTransactionManager();
        jtm.setEntityManagerFactory(instanceEntityManagerFactory(builder).getObject());
        return jtm;
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean instanceEntityManagerFactory(EntityManagerFactoryBuilder builder) {

        Map<String, Object> hibernateProps = new LinkedHashMap<>();
        hibernateProps.putAll(jpaProperties_.getHibernateProperties(instanceDataSource_));

        if (configuration_.getDao().getEmbedded()) {
            hibernateProps.put(Environment.DIALECT, DataSourcesConfiguration.EMBEDDED_HSQLDB_HIBERNATE_DIALECT);
        }
        else {
            hibernateProps.put(Environment.DIALECT, configuration_.getDao().getDialect());
        }
        hibernateProps.put(Environment.MULTI_TENANT, MultiTenancyStrategy.NONE);
        hibernateProps.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, null);
        hibernateProps.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, null);

        List<Class<?>> packages = DaoUtils.scanForJpaPackages(DaoUtils.PACKAGES_TO_SCAN, InstanceEntity.class, null);

        if (packages.size() > 1) {
            return builder.dataSource(instanceDataSource_).persistenceUnit(PERSITENCE_UNIT_NAME)
                    .packages(packages.toArray(new Class[packages.size()])).properties(hibernateProps).jta(false)
                    .build();
        }
        else {
            return builder.dataSource(instanceDataSource_).persistenceUnit(PERSITENCE_UNIT_NAME)
                    .packages(packages.get(0)).properties(hibernateProps).jta(false).build();
        }

    }

}
