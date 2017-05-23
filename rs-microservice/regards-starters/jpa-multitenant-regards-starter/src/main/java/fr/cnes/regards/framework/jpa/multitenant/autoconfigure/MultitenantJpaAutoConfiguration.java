/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.cfg.Environment;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.autoconfigure.AmqpAutoConfiguration;
import fr.cnes.regards.framework.gson.autoconfigure.GsonAutoConfiguration;
import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.exception.MultiDataBasesException;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.jpa.multitenant.exception.JpaMultitenantException;
import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
import fr.cnes.regards.framework.jpa.multitenant.resolver.CurrentTenantIdentifierResolverImpl;
import fr.cnes.regards.framework.jpa.multitenant.resolver.DataSourceBasedMultiTenantConnectionProviderImpl;
import fr.cnes.regards.framework.jpa.multitenant.resolver.DefaultTenantConnectionResolver;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.framework.jpa.utils.DaoUtils;
import fr.cnes.regards.framework.jpa.utils.IDatasourceSchemaHelper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 *
 * Configuration class to define hibernate/jpa multitenancy databases strategy
 *
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 * @since 1.0-SNAPSHOT
 */
@Configuration
@EnableJpaRepositories(
        excludeFilters = { @ComponentScan.Filter(value = InstanceEntity.class, type = FilterType.ANNOTATION) },
        basePackages = DaoUtils.ROOT_PACKAGE, entityManagerFactoryRef = "multitenantsEntityManagerFactory",
        transactionManagerRef = MultitenantDaoProperties.MULTITENANT_TRANSACTION_MANAGER)
@EnableTransactionManagement
@EnableConfigurationProperties({ JpaProperties.class })
@AutoConfigureAfter({ GsonAutoConfiguration.class, AmqpAutoConfiguration.class })
@AutoConfigureBefore({ FlywayAutoConfiguration.class })
@ConditionalOnProperty(prefix = "regards.jpa", name = "multitenant.enabled", matchIfMissing = true)
public class MultitenantJpaAutoConfiguration {

    /**
     * JPA Persistence unit name. Used to separate multiples databases.
     */
    public static final String PERSITENCE_UNIT_NAME = "multitenant";

    /**
     * Current microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     * Data sources pool
     */
    @Autowired
    @Qualifier(DataSourcesAutoConfiguration.DATA_SOURCE_BEAN_NAME)
    private Map<String, DataSource> dataSources;

    @Autowired
    @Qualifier(DataSourcesAutoConfiguration.DATASOURCE_SCHEMA_HELPER_BEAN_NAME)
    private IDatasourceSchemaHelper datasourceSchemaHelper;

    /**
     * Multitenant connection provider
     */
    @Autowired
    private MultiTenantConnectionProvider multiTenantConnectionProvider;

    /**
     * Tenant resolver.
     */
    @Autowired
    private CurrentTenantIdentifierResolver currentTenantIdentifierResolver;

    /**
     * Transaction manager builder
     */
    @Autowired
    private EntityManagerFactoryBuilder builder;

    /**
     *
     * Constructor. Check for classpath errors.
     *
     * @throws MultiDataBasesException
     *
     * @since 1.0-SNAPSHOT
     */
    public MultitenantJpaAutoConfiguration() throws MultiDataBasesException {
        DaoUtils.checkClassPath(DaoUtils.ROOT_PACKAGE);
    }

    /**
     * This bean is not used at the moment but prevent flyway auto configuration in a single point
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public Flyway flyway() {
        return new Flyway();
    }

    /**
     *
     * Create the tenant resolver. Select the tenant when a connection is needed.
     *
     * @return {@link CurrentTenantIdentifierResolverImpl}
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public CurrentTenantIdentifierResolver currentTenantIdentifierResolver(
            IRuntimeTenantResolver pThreadTenantResolver) {
        this.currentTenantIdentifierResolver = new CurrentTenantIdentifierResolverImpl(pThreadTenantResolver);
        return currentTenantIdentifierResolver;
    }

    /**
     *
     * Create the connection provider. Used to select datasource for a given tenant
     *
     * @return {@link DataSourceBasedMultiTenantConnectionProviderImpl}
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public AbstractDataSourceBasedMultiTenantConnectionProviderImpl connectionProvider() {
        return new DataSourceBasedMultiTenantConnectionProviderImpl(dataSources);
    }

    /**
     * Create Transaction manager for multitenancy projects datasources
     *
     * @return {@link PlatformTransactionManager}
     * @throws JpaMultitenantException
     */
    @Bean(name = MultitenantDaoProperties.MULTITENANT_TRANSACTION_MANAGER)
    public PlatformTransactionManager multitenantsJpaTransactionManager() throws JpaMultitenantException {
        final JpaTransactionManager jtm = new JpaTransactionManager();
        jtm.setEntityManagerFactory(multitenantsEntityManagerFactory().getObject());
        return jtm;
    }

    /**
     * Create EntityManagerFactory for multitenancy datasources
     *
     * @return {@link LocalContainerEntityManagerFactoryBean}
     * @throws JpaMultitenantException
     */
    @Bean(name = "multitenantsEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean multitenantsEntityManagerFactory() throws JpaMultitenantException {
        // Use the first dataSource configuration to init the entityManagerFactory
        if (dataSources.isEmpty()) {
            throw new ApplicationContextException("No datasource defined. JPA is not able to start."
                    + " You should define a datasource in the application.properties of the current microservice");
        }
        final DataSource defaultDataSource = dataSources.values().iterator().next();

        // Init with common properties
        final Map<String, Object> hibernateProps = datasourceSchemaHelper.getHibernateProperties();

        // Add multitenant properties
        hibernateProps.put(Environment.MULTI_TENANT, MultiTenancyStrategy.DATABASE);
        hibernateProps.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
        hibernateProps.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantIdentifierResolver);

        // Find classpath for each Entity and not NonStandardEntity
        final Set<String> packagesToScan = DaoUtils.findPackagesForJpa(DaoUtils.ROOT_PACKAGE);
        final List<Class<?>> packages = DaoUtils.scanPackagesForJpa(Entity.class, InstanceEntity.class, packagesToScan);

        return builder.dataSource(defaultDataSource).persistenceUnit(PERSITENCE_UNIT_NAME)
                .packages(packages.toArray(new Class[packages.size()])).properties(hibernateProps).jta(false).build();
    }

    /**
     *
     * Create a default TenantConnection resolver if none defined.
     *
     * @return ITenantConnectionResolver
     * @since 1.0-SNAPSHOT
     */
    @Bean
    @ConditionalOnMissingBean
    public ITenantConnectionResolver defaultTenantConnectionResolver() {
        return new DefaultTenantConnectionResolver();
    }

    /**
     * this bean allow us to set <b>our</b> instance of Gson, customized for the serialization of any data as jsonb into
     * the database
     *
     * @param pGson
     * @return
     */
    @Bean
    public Void setGsonIntoGsonUtil(Gson pGson) {
        GsonUtil.setGson(pGson);
        return null;
    }
}
