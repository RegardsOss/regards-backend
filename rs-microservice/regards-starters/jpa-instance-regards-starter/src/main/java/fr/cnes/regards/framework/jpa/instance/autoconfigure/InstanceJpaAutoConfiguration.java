/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.instance.autoconfigure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.sql.DataSource;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.cfg.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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

import com.google.gson.Gson;

import fr.cnes.regards.framework.gson.autoconfigure.GsonAutoConfiguration;
import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.exception.MultiDataBasesException;
import fr.cnes.regards.framework.jpa.instance.properties.InstanceDaoProperties;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.jpa.utils.DaoUtils;
import fr.cnes.regards.framework.jpa.utils.DataSourceHelper;

/**
 *
 * Class InstanceJpaAutoConfiguration
 *
 * Configuration class to define hibernate/jpa instance database strategy
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Configuration
@EnableJpaRepositories(
        includeFilters = { @ComponentScan.Filter(value = InstanceEntity.class, type = FilterType.ANNOTATION) },
        basePackages = DaoUtils.ROOT_PACKAGE, entityManagerFactoryRef = "instanceEntityManagerFactory",
        transactionManagerRef = InstanceDaoProperties.INSTANCE_TRANSACTION_MANAGER)
@EnableTransactionManagement
@EnableConfigurationProperties(InstanceDaoProperties.class)
@ConditionalOnProperty(prefix = "regards.jpa", name = "instance.enabled", matchIfMissing = true)
@AutoConfigureAfter(value = { GsonAutoConfiguration.class })
public class InstanceJpaAutoConfiguration {

    /**
     * Class logger
     */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceJpaAutoConfiguration.class);

    /**
     * JPA Persistence unit name. Used to separate multiples databases
     */
    private static final String PERSITENCE_UNIT_NAME = "instance";

    /**
     * Microservice global configuration
     */
    @Autowired
    private InstanceDaoProperties daoProperties;

    /**
     * JPA Properties
     */
    @Autowired
    private JpaProperties jpaProperties;

    @Value("${desableInstanceEntityAnnotation:false}")
    private boolean desableInstanceEntityAnnotation;

    /**
     * Instance datasource
     */
    @Autowired
    @Qualifier("instanceDataSource")
    private DataSource instanceDataSource;

    /**
     *
     * Constructor. Check for classpath errors.
     *
     * @throws MultiDataBasesException
     *
     * @since 1.0-SNAPSHOT
     */
    public InstanceJpaAutoConfiguration() throws MultiDataBasesException {
        DaoUtils.checkClassPath(DaoUtils.ROOT_PACKAGE);
    }

    /**
     *
     * Create TransactionManager for instance datasource
     *
     * @param pBuilder
     *            EntityManagerFactoryBuilder
     * @return PlatformTransactionManager
     * @since 1.0-SNAPSHOT
     */
    @Bean(name = InstanceDaoProperties.INSTANCE_TRANSACTION_MANAGER)
    public PlatformTransactionManager instanceJpaTransactionManager(final EntityManagerFactoryBuilder pBuilder) {
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
    public LocalContainerEntityManagerFactoryBean instanceEntityManagerFactory(
            final EntityManagerFactoryBuilder pBuilder) {

        final Map<String, Object> hibernateProps = new LinkedHashMap<>();
        hibernateProps.putAll(jpaProperties.getHibernateProperties(instanceDataSource));

        if (daoProperties.getEmbedded()) {
            hibernateProps.put(Environment.DIALECT, DataSourceHelper.EMBEDDED_HSQLDB_HIBERNATE_DIALECT);
        } else {
            hibernateProps.put(Environment.DIALECT, daoProperties.getDialect());
        }
        hibernateProps.put(Environment.MULTI_TENANT, MultiTenancyStrategy.NONE);
        hibernateProps.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, null);
        hibernateProps.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, null);
        hibernateProps.put(Environment.HBM2DDL_AUTO, "update");
        hibernateProps.put(DataSourceHelper.HIBERNATE_ID_GENERATOR_PROP, "true");

        final Set<String> packagesToScan = DaoUtils.findPackagesForJpa(DaoUtils.ROOT_PACKAGE);
        List<Class<?>> packages;
        if (desableInstanceEntityAnnotation) {
            packages = DaoUtils.scanPackagesForJpa(InstanceEntity.class, null, packagesToScan);
        } else {
            packages = DaoUtils.scanPackagesForJpa(Entity.class, null, packagesToScan);
        }

        return pBuilder.dataSource(instanceDataSource).persistenceUnit(PERSITENCE_UNIT_NAME)
                .packages(packages.toArray(new Class[packages.size()])).properties(hibernateProps).jta(false).build();

    }

    /**
     * this bean allow us to set <b>our</b> instance of Gson, customized for the serialization of any data as jsonb into
     * the database
     *
     * @param pGson
     * @return
     */
    @Bean
    public Void setGsonIntoGsonUtil(final Gson pGson) {
        GsonUtil.setGson(pGson);
        return null;
    }

}
