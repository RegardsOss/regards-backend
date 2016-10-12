/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.instance.autoconfigure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.cfg.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.configuration.MicroserviceConfiguration;
import fr.cnes.regards.framework.jpa.utils.DaoUtils;
import fr.cnes.regards.framework.jpa.utils.DataSourceHelper;

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
@ConditionalOnProperty(prefix = "regards.jpa", name = "instance.enabled", matchIfMissing = true)
public class InstanceJpaAutoConfiguration {

    /**
     * JPA Persistence unit name. Used to separate multiples databases
     */
    private static final String PERSITENCE_UNIT_NAME = "instance";

    /**
     * Microservice global configuration
     */
    @Autowired
    private MicroserviceConfiguration configuration;

    /**
     * JPA Properties
     */
    @Autowired
    private JpaProperties jpaProperties;

    /**
     * Instance datasource
     */
    @Autowired
    @Qualifier("instanceDataSource")
    private DataSource instanceDataSource;

    /**
     *
     * Create TransactionManager for instance datasource
     *
     * @param pBuilder
     *            EntityManagerFactoryBuilder
     * @return PlatformTransactionManager
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public PlatformTransactionManager instanceJpaTransactionManager(EntityManagerFactoryBuilder pBuilder) {
        final JpaTransactionManager jtm = new JpaTransactionManager();
        jtm.setEntityManagerFactory(instanceEntityManagerFactory(pBuilder).getObject());
        return jtm;
    }

    /**
     *
     * Create EntityManagerFactory for instance datasource
     *
     * @param pBuilder
     *            EntityManagerFactoryBuilder
     * @return LocalContainerEntityManagerFactoryBean
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean instanceEntityManagerFactory(EntityManagerFactoryBuilder pBuilder) {

        final Map<String, Object> hibernateProps = new LinkedHashMap<>();
        hibernateProps.putAll(jpaProperties.getHibernateProperties(instanceDataSource));

        if (configuration.getDao().getEmbedded()) {
            hibernateProps.put(Environment.DIALECT, DataSourceHelper.EMBEDDED_HSQLDB_HIBERNATE_DIALECT);
        } else {
            hibernateProps.put(Environment.DIALECT, configuration.getDao().getDialect());
        }
        hibernateProps.put(Environment.MULTI_TENANT, MultiTenancyStrategy.NONE);
        hibernateProps.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, null);
        hibernateProps.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, null);

        final List<Class<?>> packages = DaoUtils.scanForJpaPackages(DaoUtils.PACKAGES_TO_SCAN, InstanceEntity.class,
                                                                    null);

        return pBuilder.dataSource(instanceDataSource).persistenceUnit(PERSITENCE_UNIT_NAME)
                .packages(packages.toArray(new Class[packages.size()])).properties(hibernateProps).jta(false).build();

    }

    /**
     *
     * Default data source for persistence unit instance.
     *
     * @return datasource
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public DataSource instanceDataSource() {

        DataSource datasource = null;
        if (configuration.getDao().getEmbedded()) {
            final DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName(DataSourceHelper.EMBEDDED_HSQL_DRIVER_CLASS);
            dataSource.setUrl(DataSourceHelper.EMBEDDED_HSQL_URL + configuration.getDao().getEmbeddedPath()
                    + DataSourceHelper.EMBEDDED_URL_SEPARATOR + "instance" + DataSourceHelper.EMBEDDED_URL_SEPARATOR
                    + DataSourceHelper.EMBEDDED_URL_BASE_NAME);
            datasource = dataSource;
        } else {
            final DataSourceBuilder factory = DataSourceBuilder
                    .create(configuration.getDao().getInstance().getDatasource().getClassLoader())
                    .driverClassName(configuration.getDao().getDriverClassName())
                    .username(configuration.getDao().getInstance().getDatasource().getUsername())
                    .password(configuration.getDao().getInstance().getDatasource().getPassword())
                    .url(configuration.getDao().getInstance().getDatasource().getUrl());
            datasource = factory.build();
        }
        return datasource;
    }

}
