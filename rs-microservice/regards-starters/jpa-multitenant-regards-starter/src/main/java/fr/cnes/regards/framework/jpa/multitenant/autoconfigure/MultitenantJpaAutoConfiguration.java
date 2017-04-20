/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.sql.DataSource;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.cfg.Environment;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.autoconfigure.AmqpAutoConfiguration;
import fr.cnes.regards.framework.gson.autoconfigure.GsonAutoConfiguration;
import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.exception.MultiDataBasesException;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.jpa.multitenant.event.MultitenantJpaEventHandler;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionDiscarded;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
import fr.cnes.regards.framework.jpa.multitenant.resolver.CurrentTenantIdentifierResolverImpl;
import fr.cnes.regards.framework.jpa.multitenant.resolver.DataSourceBasedMultiTenantConnectionProviderImpl;
import fr.cnes.regards.framework.jpa.multitenant.resolver.DefaultTenantConnectionResolver;
import fr.cnes.regards.framework.jpa.multitenant.resolver.ITenantConnectionResolver;
import fr.cnes.regards.framework.jpa.utils.DaoUtils;
import fr.cnes.regards.framework.jpa.utils.DataSourceHelper;
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
@EnableJpaRepositories(excludeFilters = {
        @ComponentScan.Filter(value = InstanceEntity.class, type = FilterType.ANNOTATION) }, basePackages = DaoUtils.ROOT_PACKAGE, entityManagerFactoryRef = "multitenantsEntityManagerFactory", transactionManagerRef = MultitenantDaoProperties.MULTITENANT_TRANSACTION_MANAGER)
@EnableTransactionManagement
@EnableConfigurationProperties({ JpaProperties.class })
@AutoConfigureAfter(value = { GsonAutoConfiguration.class, AmqpAutoConfiguration.class })
@ConditionalOnProperty(prefix = "regards.jpa", name = "multitenant.enabled", matchIfMissing = true)
public class MultitenantJpaAutoConfiguration {

    /**
     * JPA Persistence unit name. Used to separate multiples databases.
     */
    private static final String PERSITENCE_UNIT_NAME = "multitenant";

    /**
     * Current microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     * Database schema name
     */
    @Value("${spring.jpa.properties.hibernate.default_schema}")
    private String schemaName;

    /**
     * Data sources pool
     */
    @Autowired
    @Qualifier(DataSourcesAutoConfiguration.DATA_SOURCE_BEAN_NAME)
    private Map<String, DataSource> dataSources;

    /**
     * Microservice global configuration
     */
    @Autowired
    private MultitenantDaoProperties configuration;

    @Value("${spring.jpa.hibernate.naming.implicit-strategy:org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl}")
    private String implicitNamingStrategyName;

    @Value("${spring.jpa.hibernate.naming.physical-strategy:org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl}")
    private String physicalNamingStrategyName;

    /**
     * JPA Configuration
     */
    @Autowired
    private JpaProperties jpaProperties;

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
     * Init JPA event handler manager
     *
     * @param instanceSubscriber
     *            to subscribe to tenant connection events
     * @param instancePublisher
     *            to publish {@link TenantConnectionReady} or {@link TenantConnectionDiscarded} events
     * @param multitenantResolver
     *            to resolve tenant
     * @return JPA event handler
     */
    @Bean
    public MultitenantJpaEventHandler multitenantJpaEventHandler(IInstanceSubscriber instanceSubscriber,
            IInstancePublisher instancePublisher, ITenantConnectionResolver multitenantResolver) {
        return new MultitenantJpaEventHandler(microserviceName, schemaName, dataSources, configuration,
                implicitNamingStrategyName, physicalNamingStrategyName, instanceSubscriber, instancePublisher,
                multitenantResolver);
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
     *
     * Create Transaction manager for multitenancy projects datasources
     *
     * @param pBuilder
     *            EntityManagerFactoryBuilder
     * @return PlatformTransactionManager
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @since 1.0-SNAPSHOT
     */
    @Bean(name = MultitenantDaoProperties.MULTITENANT_TRANSACTION_MANAGER)
    public PlatformTransactionManager multitenantsJpaTransactionManager(final EntityManagerFactoryBuilder pBuilder)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        final JpaTransactionManager jtm = new JpaTransactionManager();
        jtm.setEntityManagerFactory(multitenantsEntityManagerFactory(pBuilder).getObject());
        return jtm;
    }

    /**
     *
     * Create EntityManagerFactory for multitenancy datasources
     *
     * @param pBuilder
     *            EntityManagerFactoryBuilder
     * @return LocalContainerEntityManagerFactoryBean
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @since 1.0-SNAPSHOT
     */
    @Bean(name = "multitenantsEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean multitenantsEntityManagerFactory(
            final EntityManagerFactoryBuilder pBuilder)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        // Use the first dataSource configuration to init the entityManagerFactory
        if (dataSources.isEmpty()) {
            throw new ApplicationContextException("No datasource defined. JPA is not able to start."
                    + " You should define a datasource in the application.properties of the current microservice");
        }
        final DataSource defaultDataSource = dataSources.values().iterator().next();

        final Map<String, Object> hibernateProps = new LinkedHashMap<>();
        hibernateProps.putAll(jpaProperties.getHibernateProperties(defaultDataSource));

        hibernateProps.put(Environment.MULTI_TENANT, MultiTenancyStrategy.DATABASE);

        final PhysicalNamingStrategy hibernatePhysicalNamingStrategy = (PhysicalNamingStrategy) Class
                .forName(physicalNamingStrategyName).newInstance();
        hibernateProps.put(Environment.PHYSICAL_NAMING_STRATEGY, hibernatePhysicalNamingStrategy);
        final ImplicitNamingStrategy hibernateImplicitNamingStrategy = (ImplicitNamingStrategy) Class
                .forName(implicitNamingStrategyName).newInstance();
        hibernateProps.put(Environment.IMPLICIT_NAMING_STRATEGY, hibernateImplicitNamingStrategy);
        hibernateProps.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
        hibernateProps.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantIdentifierResolver);
        hibernateProps.put(Environment.HBM2DDL_AUTO, "update");
        hibernateProps.put(Environment.USE_NEW_ID_GENERATOR_MAPPINGS, "true");

        if (configuration.getEmbedded()) {
            hibernateProps.put(Environment.DIALECT, DataSourceHelper.EMBEDDED_HSQLDB_HIBERNATE_DIALECT);
        } else {
            hibernateProps.put(Environment.DIALECT, configuration.getDialect());
        }

        // Find classpath for each Entity and not NonStandardEntity
        final Set<String> packagesToScan = DaoUtils.findPackagesForJpa(DaoUtils.ROOT_PACKAGE);
        final List<Class<?>> packages = DaoUtils.scanPackagesForJpa(Entity.class, InstanceEntity.class, packagesToScan);

        return pBuilder.dataSource(defaultDataSource).persistenceUnit(PERSITENCE_UNIT_NAME)
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
