/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.utils;

import javax.persistence.Entity;
import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.sql.SQLException;
import java.util.*;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.jpa.exception.JpaException;
import fr.cnes.regards.framework.jpa.utils.*;
import fr.cnes.regards.framework.modules.person.Person;

/**
 *
 * Test updating multiple schema. Just run migration tools
 *
 * @author Marc Sordi
 *
 */
@RunWith(SpringRunner.class)
@TestPropertySource("/multipleSchema.properties")
public class MultipleSchemaUpdate {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleSchemaUpdate.class);

    @Value("${multiple.schema.test.url}")
    private String url;

    @Value("${multiple.schema.test.userName}")
    private String userName;

    @Value("${multiple.schema.test.password}")
    private String password;

    @Value("${multiple.schema.test.driverClassName}")
    private String driver;

    /**
     * Test datasource
     */
    private DataSource dataSource;

    /**
     * Hibernate properties that may impact migration configuration
     */
    private Map<String, Object> hibernateProperties;

    @Before
    public void setup() throws PropertyVetoException {
        dataSource = DataSourceHelper.createPooledDataSource("testperson", url, driver, userName, password, 5, 20,
                                                             "SELECT 1");

        // Set hibernate properties
        hibernateProperties = new HashMap<>();
        //        String implicitNamingStrategyName = "org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl";
        //        String physicalNamingStrategyName = "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl";
    }

    @After
    public void clean() {
        if (dataSource != null) {
            try {
                dataSource.getConnection().close();
            } catch (SQLException e1) {
                LOGGER.error("Error during closing connection", e1);
            }
        }
    }

    /**
     * hbm2ddl sequence generation doesn't work with multiple schemas in PostgreSQL. Only a single sequence is created for all catalog.<br/>
     * hbm2ddl should not be used in production.
     * @throws JpaException if error occurs!
     */
    @Test
    public void testWithHbm2ddl() throws JpaException {

        Hbm2ddlDatasourceSchemaHelper schemaHelper = new Hbm2ddlDatasourceSchemaHelper(hibernateProperties,
                Entity.class, null);

        schemaHelper.migrate(dataSource, Person.class.getPackage().getName(), "hbm2ddl1");
        schemaHelper.migrate(dataSource, Person.class.getPackage().getName(), "hbm2ddl2");
    }

    @Test
    public void testWithFlyway() {
        FlywayDatasourceSchemaHelper migrationHelper = new FlywayDatasourceSchemaHelper(hibernateProperties);

        String moduleName = "module0";
        migrationHelper.migrate(dataSource, "flyway1", moduleName);
        migrationHelper.migrate(dataSource, "flyway2", moduleName);
        migrationHelper.migrate(dataSource, "flyway3", moduleName);

        moduleName = "module1";
        migrationHelper.migrate(dataSource, "flyway1", moduleName);
        migrationHelper.migrate(dataSource, "flyway2", moduleName);
        migrationHelper.migrate(dataSource, "flyway3", moduleName);
    }

    @Test
    public void testScanModuleScripts() {
        FlywayDatasourceSchemaHelper migrationHelper = new FlywayDatasourceSchemaHelper(hibernateProperties);
        migrationHelper.migrate(dataSource, "scan");
    }

    @Test
    public void moduleOrderingTest() {
        // plugins < models < entities < dataAccess

        DatabaseModule plugins = new DatabaseModule("plugins");
        DatabaseModule models = new DatabaseModule("models", plugins);
        DatabaseModule entities = new DatabaseModule("entities", plugins, models);
        DatabaseModule dataAccess = new DatabaseModule("dataAccess", plugins, entities);


        List<DatabaseModule> modules = new ArrayList<>();
        modules.add(models);
        modules.add(entities);
        modules.add(dataAccess);
        modules.add(plugins);

        for (DatabaseModule module : modules) {
            module.computeWeight();
        }

        Assert.assertTrue(plugins.getWeight() == 0);
        Assert.assertTrue(models.getWeight() == 1);
        Assert.assertTrue(entities.getWeight() == 2);
        Assert.assertTrue(dataAccess.getWeight() == 3);

        Collections.sort(modules, new DatabaseModuleComparator());

        Assert.assertEquals(plugins, modules.get(0));
        Assert.assertEquals(models, modules.get(1));
        Assert.assertEquals(entities, modules.get(2));
        Assert.assertEquals(dataAccess, modules.get(3));
    }
}
