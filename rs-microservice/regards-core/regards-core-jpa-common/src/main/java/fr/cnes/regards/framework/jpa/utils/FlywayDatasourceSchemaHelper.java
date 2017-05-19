/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.utils;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.internal.util.Location;
import org.flywaydb.core.internal.util.scanner.Resource;
import org.flywaydb.core.internal.util.scanner.Scanner;
import org.hibernate.cfg.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * @author Marc Sordi
 *
 */
public class FlywayDatasourceSchemaHelper implements IDatasourceSchemaHelper {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FlywayDatasourceSchemaHelper.class);

    /**
     * Default class loader
     */
    private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    /**
     * Default script suffix (default value)
     */
    private static final String SQL_MIGRATION_SUFFIX = ".sql";

    /**
     * Base module script directory.<br/>
     * Example :<br/>
     * If path is <code>scripts</code>, module scripts will be scanned in <code>scripts/{moduleName}/*.sql</code>
     *
     */
    private String scriptLocationPath = "scripts";

    /**
     * Hibernate properties that may impact migration configuration
     */
    private final Map<String, Object> hibernateProperties;

    public FlywayDatasourceSchemaHelper(Map<String, Object> hibernateProperties) {
        this.hibernateProperties = hibernateProperties;
    }

    /**
     * Migrate datasource schema to new version
     * @param dataSource the datasource to migrate
     * @param schema the target schema
     * @param moduleName the module
     */
    public void migrate(DataSource dataSource, String schema, String moduleName) {

        Assert.notNull(dataSource);
        Assert.notNull(schema);
        Assert.notNull(moduleName);

        LOGGER.debug("Migrating datasource with schema {} for module {}", schema, moduleName);

        // Init Flywaydb tool
        Flyway flyway = new Flyway();
        // Associate datasource
        flyway.setDataSource(dataSource);
        // Set module location
        flyway.setLocations(scriptLocationPath + File.separator + moduleName);
        // Specify working schema
        flyway.setSchemas(schema);
        // Create one migration table by module
        flyway.setTable(moduleName + "_schema_version");
        flyway.setBaselineOnMigrate(true);
        // When creating module metadata table, set beginning version to 0 in order to properly apply all init scripts
        flyway.setBaselineVersion(MigrationVersion.fromVersion("0"));
        // Do migrate
        flyway.migrate();
    }

    /**
     * Migrate datasource schema to new version looping on each detected module
     * @param dataSource the datasource to migrate
     * @param schema the target schema
     */
    public void migrate(DataSource dataSource, String schema) {

        Assert.notNull(dataSource);
        Assert.notNull(schema);

        // Use flyway scanner to be consistent
        Scanner scanner = new Scanner(classLoader);
        // Scan all resources without considering modules
        Resource[] resources = scanner.scanForResources(new Location(scriptLocationPath), "", SQL_MIGRATION_SUFFIX);
        // Manage resource pattern
        Pattern resourcePattern = Pattern.compile("^" + scriptLocationPath + File.separator + "(.*)" + File.separator
                + ".*\\" + SQL_MIGRATION_SUFFIX + "$");
        // Retrieve all modules
        Set<String> modules = new HashSet<>();
        for (Resource resource : resources) {
            Matcher matcher = resourcePattern.matcher(resource.getLocation());
            if (matcher.matches()) {
                modules.add(matcher.group(1));
            } else {
                LOGGER.warn("Cannot retrieve module name in resource {}. Format must conform to {}",
                            resource.getLocation(), resourcePattern.toString());
            }
        }
        // Apply module migration
        modules.forEach(module -> migrate(dataSource, schema, module));
    }

    /**
     * Use JPA configuration to retrieve schema and launch migration
     * @param dataSource the datasource to migrate
     */
    @Override
    public void migrate(DataSource dataSource) {
        migrate(dataSource, (String) hibernateProperties.get(Environment.DEFAULT_SCHEMA));
    }

    public String getScriptLocationPath() {
        return scriptLocationPath;
    }

    /**
     * Change base module script location. Default : <code>scripts</code>.<br/>
     * Script will be detected in this location only.
     * @param pScriptLocationPath new location path
     */
    public void setScriptLocationPath(String pScriptLocationPath) {
        scriptLocationPath = pScriptLocationPath;
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.framework.jpa.utils.IDatasourceSchemaHelper#getHibernateProperties()
     */
    @Override
    public Map<String, Object> getHibernateProperties() {
        return hibernateProperties;
    }
}
