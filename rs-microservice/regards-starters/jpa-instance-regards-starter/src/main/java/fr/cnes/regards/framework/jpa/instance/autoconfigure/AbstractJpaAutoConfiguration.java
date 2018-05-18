/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.jpa.instance.autoconfigure;

import javax.sql.DataSource;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flywaydb.core.Flyway;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.cfg.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import com.google.gson.Gson;
import fr.cnes.regards.framework.jpa.exception.JpaException;
import fr.cnes.regards.framework.jpa.exception.MultiDataBasesException;
import fr.cnes.regards.framework.jpa.instance.properties.InstanceDaoProperties;
import fr.cnes.regards.framework.jpa.json.GsonUtil;
import fr.cnes.regards.framework.jpa.utils.DaoUtils;
import fr.cnes.regards.framework.jpa.utils.DataSourceHelper;
import fr.cnes.regards.framework.jpa.utils.FlywayDatasourceSchemaHelper;
import fr.cnes.regards.framework.jpa.utils.Hbm2ddlDatasourceSchemaHelper;
import fr.cnes.regards.framework.jpa.utils.IDatasourceSchemaHelper;
import fr.cnes.regards.framework.jpa.utils.MigrationTool;

/**
 *
 * Class AbstractJpaAutoConfiguration
 *
 * Configuration class to define hibernate/jpa instance database strategy
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public abstract class AbstractJpaAutoConfiguration {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJpaAutoConfiguration.class);

    /**
     * JPA Persistence unit name. Used to separate multiples databases
     */
    private static final String PERSITENCE_UNIT_NAME = "instance";

    /**
     * {@link IDatasourceSchemaHelper} instance bean
     */
    private static final String DATASOURCE_SCHEMA_HELPER_BEAN_NAME = "instanceDataSourceSchemaHelper";

    /**
     * Current microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    @Value("${spring.jpa.hibernate.naming.implicit-strategy:org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl}")
    private String implicitNamingStrategyName;

    @Value("${spring.jpa.hibernate.naming.physical-strategy:org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl}")
    private String physicalNamingStrategyName;

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

    /**
     * Instance datasource
     */
    @Autowired
    @Qualifier("instanceDataSource")
    private DataSource instanceDataSource;

    /**
     * Transaction manager builder
     */
    @Autowired
    private EntityManagerFactoryBuilder builder;

    /**
     * Constructor. Check for classpath errors.
     * @throws MultiDataBasesException
     */
    public AbstractJpaAutoConfiguration() throws MultiDataBasesException {
        DaoUtils.checkClassPath(DaoUtils.ROOT_PACKAGE);
    }

    /*
     * This bean is not used at the moment but prevent flyway auto configuration in a single point
     */
    @Bean
    @ConditionalOnMissingBean
    public Flyway flyway() {
        return new Flyway();
    }

    /**
     * Use schema helper to migrate database schema.
     * {@link IDatasourceSchemaHelper#migrate()} is called immediatly after bean creation on instance datasource.
     *
     * @return {@link IDatasourceSchemaHelper}
     * @throws JpaException if error occurs!
     */
    @Bean(initMethod = "migrate", name = DATASOURCE_SCHEMA_HELPER_BEAN_NAME)
    public IDatasourceSchemaHelper datasourceSchemaHelper() throws JpaException {

        Map<String, Object> hibernateProperties = getHibernateProperties();

        if (MigrationTool.HBM2DDL.equals(daoProperties.getMigrationTool())) {
            Hbm2ddlDatasourceSchemaHelper helper = new Hbm2ddlDatasourceSchemaHelper(hibernateProperties,
                    getEntityAnnotationScan(), null);
            helper.setDataSource(instanceDataSource);
            // Set output file, may be null.
            helper.setOutputFile(daoProperties.getOutputFile());
            return helper;
        } else {
            FlywayDatasourceSchemaHelper helper = new FlywayDatasourceSchemaHelper(hibernateProperties);
            helper.setDataSource(instanceDataSource);
            return helper;
        }
    }

    /**
     *
     * Create TransactionManager for instance datasource
     *
     * @param pBuilder
     *            EntityManagerFactoryBuilder
     * @return PlatformTransactionManager
     * @throws JpaException if error occurs
     * @since 1.0-SNAPSHOT
     */
    @Bean(name = InstanceDaoProperties.INSTANCE_TRANSACTION_MANAGER)
    public PlatformTransactionManager instanceJpaTransactionManager() throws JpaException {
        final JpaTransactionManager jtm = new JpaTransactionManager();
        jtm.setEntityManagerFactory(instanceEntityManagerFactory().getObject());
        return jtm;
    }

    /**
     * Create EntityManagerFactory for instance datasource
     * @return LocalContainerEntityManagerFactoryBean
     * @throws JpaException if error occurs!
     */
    @Bean
    @Primary
    @DependsOn(DATASOURCE_SCHEMA_HELPER_BEAN_NAME)
    public LocalContainerEntityManagerFactoryBean instanceEntityManagerFactory() throws JpaException {

        // Init with common properties
        final Map<String, Object> hibernateProps = getHibernateProperties();

        hibernateProps.put(Environment.MULTI_TENANT, MultiTenancyStrategy.NONE);
        hibernateProps.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, null);
        hibernateProps.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, null);

        final Set<String> packagesToScan = DaoUtils.findPackagesForJpa(DaoUtils.ROOT_PACKAGE);
        List<Class<?>> packages;
        packages = DaoUtils.scanPackagesForJpa(getEntityAnnotationScan(), null, packagesToScan);

        return builder.dataSource(instanceDataSource).persistenceUnit(PERSITENCE_UNIT_NAME)
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

    public abstract Class<? extends Annotation> getEntityAnnotationScan();

    /**
     * Compute database properties
     *
     * @return database properties
     * @throws JpaException if error occurs!
     */
    private Map<String, Object> getHibernateProperties() throws JpaException {
        Map<String, Object> dbProperties = new HashMap<>();

        // Add Spring JPA hibernate properties
        // Schema must be retrieved here if managed with property :
        // spring.jpa.properties.hibernate.default_schema
        // Before retrieving hibernate properties, set ddl auto to avoid the need of a datasource
        jpaProperties.getHibernate().setDdlAuto("none");
        dbProperties.putAll(jpaProperties.getHibernateProperties(null));
        // Remove hbm2ddl as schema update is done programmatically
        dbProperties.remove(Environment.HBM2DDL_AUTO);

        // Dialect
        String dialect = daoProperties.getDialect();
        if (daoProperties.getEmbedded()) {
            // Force dialect for embedded database
            dialect = DataSourceHelper.EMBEDDED_HSQLDB_HIBERNATE_DIALECT;
        }
        dbProperties.put(Environment.DIALECT, dialect);

        dbProperties.put(Environment.USE_NEW_ID_GENERATOR_MAPPINGS, true);

        try {
            PhysicalNamingStrategy hibernatePhysicalNamingStrategy = (PhysicalNamingStrategy) Class
                    .forName(physicalNamingStrategyName).newInstance();
            dbProperties.put(Environment.PHYSICAL_NAMING_STRATEGY, hibernatePhysicalNamingStrategy);
            final ImplicitNamingStrategy hibernateImplicitNamingStrategy = (ImplicitNamingStrategy) Class
                    .forName(implicitNamingStrategyName).newInstance();
            dbProperties.put(Environment.IMPLICIT_NAMING_STRATEGY, hibernateImplicitNamingStrategy);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            LOGGER.error("Error occurs with naming strategy", e);
            throw new JpaException(e);
        }
        return dbProperties;
    }

}
