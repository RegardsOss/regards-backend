/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.jpa.utils;

import com.google.common.base.Preconditions;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.api.resource.LoadableResource;
import org.flywaydb.core.api.resource.Resource;
import org.flywaydb.core.internal.scanner.LocationScannerCache;
import org.flywaydb.core.internal.scanner.ResourceNameCache;
import org.flywaydb.core.internal.scanner.Scanner;
import org.hibernate.cfg.AvailableSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Marc Sordi
 */
public class FlywayDatasourceSchemaHelper extends AbstractDataSourceSchemaHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlywayDatasourceSchemaHelper.class);

    private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    /**
     * Default script suffix (default value)
     */
    private static final String SQL_MIGRATION_SUFFIX = ".sql";

    /**
     * Base module script directory.<br/>
     * Example :<br/>
     * If path is <code>scripts</code>, module scripts will be scanned in <code>scripts/{moduleName}/*.sql</code>
     */
    private static final String SCRIPT_LOCATION_PATH = "scripts";

    private static final String TABLE_SUFFIX = "_schema_version";

    private static final String MIGRATION_TABLE_NAME = "migration";

    private final ApplicationContext applicationContext;

    public FlywayDatasourceSchemaHelper(Map<String, Object> hibernateProperties,
                                        ApplicationContext applicationContext) {
        super(hibernateProperties);
        this.applicationContext = applicationContext;
    }

    /**
     * Use JPA configuration to retrieve schema and launch migration
     *
     * @param dataSource the datasource to migrate
     * @param tenant     associated tenant
     */
    @Override
    public void migrate(DataSource dataSource, String tenant) {
        IRuntimeTenantResolver runtimeTenantResolver = applicationContext.getBean(IRuntimeTenantResolver.class);
        try {
            runtimeTenantResolver.forceTenant(tenant);
            migrateSchema(dataSource, (String) hibernateProperties.get(AvailableSettings.DEFAULT_SCHEMA));
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    /**
     * Migrate datasource schema to new version looping on each detected module
     *
     * @param dataSource the datasource to migrate
     * @param schema     the target schema
     */
    public void migrateSchema(DataSource dataSource, String schema) {

        Preconditions.checkNotNull(dataSource);
        Preconditions.checkNotNull(schema, "Flyway migration tool requires a database schema");

        LOGGER.info("Migrating datasource {} with schema {}", dataSource, schema);

        // Use flyway scanner initialized with script dir (ie resources/scripts)
        Scanner<JavaMigration> scanner = new Scanner<>(JavaMigration.class,
                                                       Collections.singleton(new Location(SCRIPT_LOCATION_PATH)),
                                                       classLoader,
                                                       Charset.defaultCharset(),
                                                       false,
                                                       false,
                                                       new ResourceNameCache(),
                                                       new LocationScannerCache(),
                                                       false);

        // Scan all sql scripts without considering modules (into resources/scripts, there are one dir per module)
        Collection<LoadableResource> sqlScripts = scanner.getResources("", SQL_MIGRATION_SUFFIX);
        // Manage resource (ie SQL scripts) pattern (^scripts/(.*)/.*\\.sql)
        Pattern scriptPattern = Pattern.compile("^"
                                                + SCRIPT_LOCATION_PATH
                                                + File.separator
                                                + "(.*)"
                                                + File.separator
                                                + ".*\\"
                                                + SQL_MIGRATION_SUFFIX
                                                + "$");
        // Retrieve all modules (scripts are into <module> dir)
        Set<String> modules = new HashSet<>();
        for (Resource script : sqlScripts) {
            // Match script from relative path
            Matcher matcher = scriptPattern.matcher(script.getAbsolutePath());
            if (matcher.matches()) {
                modules.add(matcher.group(1));
            } else {
                LOGGER.warn("Cannot retrieve module name in resource {}. Format must conform to {}",
                            script.getAbsolutePath(),
                            scriptPattern);
            }
        }
        // Apply dependency check
        List<DatabaseModule> depModules = buildDatabaseModules(modules);

        // Apply module migration on sorted modules
        depModules.forEach(module -> migrateModule(dataSource, schema, module.getName()));

        // Run Spring aware JavaMigrations
        performMigrations(dataSource, schema);
    }

    /**
     * Migrate a specific module in datasource and schema to new version
     *
     * @param dataSource the datasource to migrate
     * @param schema     the target schema
     * @param moduleName the module
     */
    private void migrateModule(DataSource dataSource, String schema, String moduleName) {

        Preconditions.checkNotNull(dataSource);
        Preconditions.checkNotNull(schema);
        Preconditions.checkNotNull(moduleName);

        LOGGER.info("Migrating datasource with schema {} for module {}", schema, moduleName);

        Flyway flyway = Flyway.configure()
                              // Associate datasource
                              .dataSource(dataSource)
                              // Set module location
                              .locations(SCRIPT_LOCATION_PATH + File.separator + moduleName)
                              // Specify working schema
                              .schemas(schema).defaultSchema(schema)
                              // Create one migration table by module
                              .table(moduleName + TABLE_SUFFIX).baselineOnMigrate(true)
                              // When creating module metadata table, set beginning version to 0 in order to properly apply all init scripts
                              .baselineVersion(MigrationVersion.fromVersion("0")).load();
        flyway.migrate();
    }

    /**
     * Build database module tree and sort all modules by priority
     *
     * @param modules list of modules to consider
     * @return a list of modules ordered according to its dependencies
     */
    private List<DatabaseModule> buildDatabaseModules(Set<String> modules) {

        Map<String, DatabaseModule> moduleMap = new HashMap<>();

        // Init each database module
        modules.forEach(module -> moduleMap.put(module, new DatabaseModule(module)));

        // Init dependencies
        initModuleDependencies(moduleMap);

        // Compute weight
        moduleMap.values().forEach(DatabaseModule::computeWeight);

        // Compute sorted result list
        List<DatabaseModule> dbModules = new ArrayList<>(moduleMap.values());
        dbModules.sort(new DatabaseModuleComparator());

        return dbModules;
    }

    private void initModuleDependencies(Map<String, DatabaseModule> moduleMap) {

        for (DatabaseModule dbModule : moduleMap.values()) {

            Properties moduleProperties = getModuleProperties(dbModule.getName());
            String dependencyProperty = moduleProperties.getProperty("module.dependencies");

            if (!StringUtils.isEmpty(dependencyProperty)) {

                for (String depModule : dependencyProperty.split(",")) {

                    DatabaseModule depDbModule = moduleMap.get(depModule);
                    if (depDbModule == null) {
                        LOGGER.warn("Dependent module \"{}\" of module \"{}\" not found in classpath",
                                    depModule,
                                    dbModule.getName());
                    } else {
                        LOGGER.debug("Dependency found for module \"{}\": \"{}\"",
                                     dbModule.getName(),
                                     depDbModule.getName());
                        moduleMap.get(dbModule.getName()).addDependency(depDbModule);
                    }

                }

            } else {
                LOGGER.debug("No dependency found for module \"{}\"", dbModule.getName());
            }
        }
    }

    /**
     * Load module properties if any
     *
     * @param module name of the module
     * @return {@link Properties}
     */
    private Properties getModuleProperties(String module) {

        Properties properties = new Properties();

        try (InputStream input = classLoader.getResourceAsStream(SCRIPT_LOCATION_PATH
                                                                 + File.separator
                                                                 + module
                                                                 + File.separator
                                                                 + "dbmodule.properties")) {
            if (input == null) {
                LOGGER.info("No module property found for module \"{}\"", module);
            } else {
                properties.load(input);
            }
        } catch (IOException e) {
            LOGGER.error("Error reading or closing database module properties", e);
        }

        return properties;
    }

    /**
     * Run all Spring managed JavaMigration beans.
     *
     * @param dataSource dataSource to migrate
     * @param schema     target schema
     */
    private void performMigrations(DataSource dataSource, String schema) {

        Preconditions.checkNotNull(dataSource);
        Preconditions.checkNotNull(schema);

        LOGGER.info("Running Java migrations for datasource {} with schema {}", dataSource, schema);

        Collection<JavaMigration> migrations = applicationContext.getBeansOfType(JavaMigration.class).values();
        Flyway flyway = Flyway.configure()
                              // Associate datasource
                              .dataSource(dataSource)
                              // Specify working schema
                              .schemas(schema).defaultSchema(schema)
                              // Create one migration table by module
                              .table(MIGRATION_TABLE_NAME + TABLE_SUFFIX).baselineOnMigrate(true)
                              // When creating module metadata table, set beginning version to 0 in order to properly apply all init scripts
                              .baselineVersion(MigrationVersion.fromVersion("0"))
                              // Include all Spring managed Java migrations
                              .javaMigrations(migrations.toArray(new JavaMigration[0])).load();

        LOGGER.info("Migration beans : {}",
                    migrations.stream()
                              .map(javaMigration -> javaMigration.getClass().getSimpleName())
                              .collect(Collectors.toList()));

        flyway.migrate();
    }

}
