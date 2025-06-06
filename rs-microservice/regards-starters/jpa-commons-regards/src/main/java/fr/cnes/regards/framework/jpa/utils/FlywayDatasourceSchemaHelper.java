/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import jakarta.annotation.Nullable;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.exception.FlywayValidateException;
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
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Runs the flyway migration scripts (.sql and java) per tenant.
 *
 * @author Marc Sordi
 */
public class FlywayDatasourceSchemaHelper extends AbstractDataSourceSchemaHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlywayDatasourceSchemaHelper.class);

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

    private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    private final ApplicationContext applicationContext;

    /**
     * Only for testing. Should remain null in production. Enabled using {@link #enableMigrationLogging()}.
     */
    private @Nullable List<MigrationInfo> migrationInfos;

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
     * Enable logging of applied migrations. Should be only used for testing.
     */
    @VisibleForTesting
    /*package*/ void enableMigrationLogging() {
        if (migrationInfos == null) {
            migrationInfos = new ArrayList<>();
        }
    }

    /**
     * Returns the migrations that were performed so far.
     */
    @VisibleForTesting
    /*package*/ List<MigrationInfo> getMigrationInfos() {
        return migrationInfos;
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

        Map<String, DatabaseModule> moduleMap = new HashMap<>();
        List<JavaMigration> legacyMigrations = new ArrayList<>();
        scanSqlModules(moduleMap);
        scanJavaModules(moduleMap, legacyMigrations);

        // Apply dependency check
        List<DatabaseModule> depModules = buildDatabaseModules(moduleMap);

        // Apply module migration on sorted modules
        depModules.forEach(module -> migrateModule(dataSource, schema, module));

        // Run legacy JavaMigrations. These ones are run last, and that's why they are legacy. The
        // new ones are run altogether with .sql scripts.
        performLegacyMigrations(dataSource, schema, legacyMigrations);
    }

    /**
     * Use flyway to scan all SQL migration scripts in the classpath. Scripts are expected to be
     * arranged in scripts.{moduleName}/*.sql. scripts.{moduleName} is a folder with one or more .sql
     * files that should be processed by flyway altogether. This folder may also contain a file named
     * dbModules.properties that contain additional metadata for this module (currently, it can contain
     * only a property named module.dependencies that describes the list of modules that should be processed
     * before this module).
     *
     * @param moduleMap The map of modules to be filled by this method.
     */
    private void scanSqlModules(Map<String, DatabaseModule> moduleMap) {
        // Use flyway scanner initialized with script dir (ie resources/scripts)
        Configuration config = new FluentConfiguration().locations(new Location(SCRIPT_LOCATION_PATH))
                                                        .encoding(Charset.defaultCharset())
                                                        .detectEncoding(false)
                                                        .failOnMissingLocations(false);
        Scanner<JavaMigration> scanner = new Scanner<>(JavaMigration.class,
                                                       false,
                                                       new ResourceNameCache(),
                                                       new LocationScannerCache(),
                                                       config);

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
        // Retrieve all modules (scripts are into <module> dir).
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
        for (String module : modules) {
            moduleMap.computeIfAbsent(module, DatabaseModule::new).setHasSqlScripts(true);
        }
    }

    /**
     * Collects all flyway java migration beans. There are two kind of java migrations beans: the module-aware ones,
     * implementing RegardsJavaMigration, and the legacy ones, implementing JavaMigration but not RegardsJavaMigration.
     *
     * @param moduleMap        The map to be filled with beans implementing RegardsJavaMigration.
     * @param legacyMigrations The list to be filled with beans implementing JavaMigration but not RegardsJavaMigration.
     */
    private void scanJavaModules(Map<String, DatabaseModule> moduleMap, List<JavaMigration> legacyMigrations) {
        Collection<JavaMigration> migrations = applicationContext.getBeansOfType(JavaMigration.class).values();
        for (JavaMigration m : migrations) {
            if (m instanceof RegardsJavaMigration rjm) {
                moduleMap.computeIfAbsent(rjm.getModuleName(), DatabaseModule::new).addJavaMigration(rjm);
            } else {
                legacyMigrations.add(m);
            }
        }
    }

    /**
     * Migrate a specific module in datasource and schema to new version
     *
     * @param dataSource the datasource to migrate
     * @param schema     the target schema
     * @param module     the module
     */
    @VisibleForTesting
    /*package*/ void migrateModule(DataSource dataSource, String schema, DatabaseModule module) {
        Objects.requireNonNull(dataSource);
        Objects.requireNonNull(schema);
        String moduleName = Objects.requireNonNull(module).getName();

        LOGGER.info("Migrating datasource with schema {} for module {}", schema, moduleName);
        Flyway flywayAutoConf = applicationContext.getBean(Flyway.class);

        FluentConfiguration conf = getFlywayConfiguration(flywayAutoConf, dataSource, schema)
            // Create one migration table by module
            .table(moduleName + TABLE_SUFFIX);
        if (module.hasSqlScripts()) {
            // Set module location
            conf.locations(SCRIPT_LOCATION_PATH + File.separator + moduleName);
        }
        if (!module.getJavaMigrations().isEmpty()) {
            conf.javaMigrations(module.getJavaMigrations().toArray(new JavaMigration[0]));
        }
        Flyway flyway = conf.load();
        try {
            flyway.migrate();
            if (migrationInfos != null) {
                migrationInfos.addAll(List.of(flyway.info().applied()));
            }
        } catch (FlywayValidateException e) {
            try {
                LOGGER.error("Error while migrating table {} of schema {} with script location '{}' in database {}",
                             moduleName + TABLE_SUFFIX,
                             schema,
                             SCRIPT_LOCATION_PATH + File.separator + moduleName,
                             dataSource.getConnection().getCatalog());
            } catch (SQLException sqlException) {
                LOGGER.error(sqlException.getMessage(), sqlException);
            }
            throw e;
        }
    }

    /**
     * Build database module tree and sort all modules by priority
     *
     * @param moduleMap A map of modules to consider
     * @return a list of modules ordered according to its dependencies
     */
    private List<DatabaseModule> buildDatabaseModules(Map<String, DatabaseModule> moduleMap) {
        // Init dependencies
        initModuleDependencies(moduleMap);

        // Compute weight
        moduleMap.values().forEach(DatabaseModule::computeWeight);

        // Compute sorted result list
        List<DatabaseModule> dbModules = new ArrayList<>(moduleMap.values());
        // The greater weight, the greater dependency depth, so sort modules by ascending weight:
        dbModules.sort(Comparator.comparingInt(DatabaseModule::getWeight));

        return dbModules;
    }

    private void initModuleDependencies(Map<String, DatabaseModule> moduleMap) {
        for (DatabaseModule dbModule : moduleMap.values()) {
            Set<String> dependencies = getModuleDependencies(dbModule);
            if (dependencies.isEmpty()) {
                LOGGER.debug("No dependency found for module \"{}\"", dbModule.getName());
            } else {
                for (String depModule : dependencies) {
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
            }
        }
    }

    /**
     * Returns all dependencies of a given module, considering both the module.dependencies file
     * that may be present in the module folder, and the list of dependencies declared by java migrations.
     */
    private Set<String> getModuleDependencies(DatabaseModule module) {
        Set<String> ret = new HashSet<>();
        Properties moduleProperties = getModuleProperties(module.getName());
        String dependencyProperty = moduleProperties.getProperty("module.dependencies");

        if (StringUtils.hasText(dependencyProperty)) {
            ret.addAll(List.of(dependencyProperty.split(",")));
        }
        ret.addAll(module.getJavaMigrations().stream().flatMap(jm -> jm.getDependencies().stream()).toList());
        return ret;
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
     * Run all the specified JavaMigration objects.
     *
     * @param dataSource dataSource to migrate
     * @param schema     target schema
     * @param migrations list of Java migration beans. Beans instance of RegardsJavaMigrations should
     *                   not be included in this list.
     */
    private void performLegacyMigrations(DataSource dataSource, String schema, List<JavaMigration> migrations) {

        Preconditions.checkNotNull(dataSource);
        Preconditions.checkNotNull(schema);

        LOGGER.info("Running legacy Java migrations for datasource {} with schema {}", dataSource, schema);

        Flyway flywayAutoConf = applicationContext.getBean(Flyway.class);
        Flyway flyway = getFlywayConfiguration(flywayAutoConf, dataSource, schema)
            // Create one migration table by module
            .table(MIGRATION_TABLE_NAME + TABLE_SUFFIX)
            // Include all Spring managed Java migrations
            .javaMigrations(migrations.toArray(new JavaMigration[0])).load();

        LOGGER.info("Migration beans : {}",
                    migrations.stream()
                              .map(javaMigration -> javaMigration.getClass().getSimpleName())
                              .collect(Collectors.toList()));

        flyway.migrate();
        if (migrationInfos != null) {
            migrationInfos.addAll(List.of(flyway.info().applied()));
        }
    }

    private static FluentConfiguration getFlywayConfiguration(Flyway flywayAutoConf,
                                                              DataSource dataSource,
                                                              String schema) {
        return Flyway.configure()
                     // Import configuration from our auto configure bean
                     .configuration(flywayAutoConf.getConfiguration())
                     // Associate datasource
                     .dataSource(dataSource)
                     // Specify working schema
                     .schemas(schema).defaultSchema(schema).baselineOnMigrate(true)
                     // When creating module metadata table, set beginning version to 0 in order to properly apply all init scripts
                     .baselineVersion(MigrationVersion.fromVersion("0"));
    }

}
