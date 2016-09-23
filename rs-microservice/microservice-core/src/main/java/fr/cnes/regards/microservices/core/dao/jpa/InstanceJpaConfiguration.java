/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.jpa;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.cfg.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

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
@EnableConfigurationProperties(JpaProperties.class)
@EnableJpaRepositories(includeFilters = {
        @ComponentScan.Filter(value = InstanceEntity.class, type = FilterType.ANNOTATION) }, basePackages = InstanceJpaConfiguration.PACKAGES_TO_SCAN, entityManagerFactoryRef = "instanceEntityManagerFactory", transactionManagerRef = "instanceJpaTransactionManager")
@ConditionalOnProperty("microservice.dao.instance.enabled")
public class InstanceJpaConfiguration {

    public static final String PACKAGES_TO_SCAN = "fr.cnes.regards";

    private static final Logger LOG = LoggerFactory.getLogger(InstanceJpaConfiguration.class);

    @Autowired
    private MicroserviceConfiguration configuration_;

    @Autowired
    private JpaProperties jpaProperties_;

    @Bean
    public JpaTransactionManager instanceJpaTransactionManager() {
        JpaTransactionManager jtm = new JpaTransactionManager();
        jtm.setPersistenceUnitName("instance");
        return jtm;
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean instanceEntityManagerFactory(EntityManagerFactoryBuilder builder) {

        Map<String, Object> hibernateProps = new LinkedHashMap<>();
        hibernateProps.putAll(jpaProperties_.getHibernateProperties(instanceDataSource()));

        if (configuration_.getDao().getEmbedded()) {
            hibernateProps.put(Environment.DIALECT, DataSourceConfig.EMBEDDED_HSQLDB_HIBERNATE_DIALECT);
        }
        else {
            hibernateProps.put(Environment.DIALECT, configuration_.getDao().getDialect());
        }
        hibernateProps.put(Environment.MULTI_TENANT, MultiTenancyStrategy.NONE);
        hibernateProps.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, null);
        hibernateProps.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, null);

        List<String> packages = new ArrayList<>();
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(InstanceEntity.class));
        for (BeanDefinition def : scanner.findCandidateComponents(PACKAGES_TO_SCAN)) {
            try {
                packages.add(Class.forName(def.getBeanClassName()).getPackage().getName());
            }
            catch (ClassNotFoundException e) {
                LOG.error("Error adding entity " + def.getBeanClassName() + " for hibernate database update");
                LOG.error(e.getMessage(), e);
            }
        }

        if (packages.size() > 1) {
            return builder.dataSource(instanceDataSource()).persistenceUnit("instance")
                    .packages(new String[packages.size()]).properties(hibernateProps).jta(false).build();
        }
        else {
            return builder.dataSource(instanceDataSource()).persistenceUnit("instance").packages(packages.get(0))
                    .properties(hibernateProps).jta(false).build();
        }

    }

    @Bean
    public DataSource instanceDataSource() {

        if (configuration_.getDao().getEmbedded()) {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName(DataSourceConfig.EMBEDDED_HSQL_DRIVER_CLASS);
            dataSource.setUrl(DataSourceConfig.EMBEDDED_HSQL_URL + configuration_.getDao().getEmbeddedPath()
                    + "/instance/applicationdb");
            return dataSource;
        }
        else {
            DataSourceBuilder factory = DataSourceBuilder
                    .create(configuration_.getDao().getInstance().getDatasource().getClassLoader())
                    .driverClassName(configuration_.getDao().getDriverClassName())
                    .username(configuration_.getDao().getInstance().getDatasource().getUsername())
                    .password(configuration_.getDao().getInstance().getDatasource().getPassword())
                    .url(configuration_.getDao().getInstance().getDatasource().getUrl());
            return factory.build();
        }
    }
}
