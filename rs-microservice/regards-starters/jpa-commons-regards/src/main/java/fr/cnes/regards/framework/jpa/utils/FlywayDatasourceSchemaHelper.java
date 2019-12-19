/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.internal.resource.LoadableResource;
import org.flywaydb.core.internal.resource.Resource;
import org.flywaydb.core.internal.scanner.ResourceNameCache;
import org.flywaydb.core.internal.scanner.Scanner;
import org.hibernate.cfg.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * @author Marc Sordi
 */
public class FlywayDatasourceSchemaHelper extends AbstractDataSourceSchemaHelper {

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
     */
    private String scriptLocationPath = "scripts";

    public FlywayDatasourceSchemaHelper(Map<String, Object> hibernateProperties) {
        super(hibernateProperties);
    }

    /**
     * Migrate datasource schema to new version
     * @param dataSource the datasource to migrate
     * @param schema the target schema
     * @param moduleName the module
     */
    public void migrate(DataSource dataSource, String schema, String moduleName) {

        Preconditions.checkNotNull(dataSource);
        Preconditions.checkNotNull(schema);
        Preconditions.checkNotNull(moduleName);

        LOGGER.debug("Migrating datasource with schema {} for module {}", schema, moduleName);

        Flyway flyway = Flyway.configure() // get configurfation
                .dataSource(dataSource) // Associate datasource
                .locations(scriptLocationPath + File.separator + moduleName) // Set module location
                .schemas(schema) // Specify working schema
                .table(moduleName + "_schema_version") // Create one migration table by module
                .baselineOnMigrate(true).baselineVersion(MigrationVersion.fromVersion("0"))// When creating module metadata table,
                // set beginning version to 0 in order to properly apply all init scripts
                .load();
        // Do migrate
        flyway.migrate();
    }

    /**
     * Migrate datasource schema to new version looping on each detected module
     * @param dataSource the datasource to migrate
     * @param schema the target schema
     */
    public void migrate(DataSource dataSource, String schema) {

        Preconditions.checkNotNull(dataSource);
        Preconditions.checkNotNull(schema, "Flyway migration tool requires a database schema");

        // Use flyway scanner initialized with script dir (ie resources/scripts)
        Scanner<JavaMigration> scanner = new Scanner<>(JavaMigration.class,
                Collections.singleton(new Location(scriptLocationPath)), classLoader, Charset.defaultCharset(),
                new ResourceNameCache());

        // Scan all sql scripts without considering modules (into resources/scripts, there are one dir per module)
        Collection<LoadableResource> sqlScripts = scanner.getResources("", SQL_MIGRATION_SUFFIX);
        // Manage resource (ie SQL scripts) pattern (^scripts/(.*)/.*\\.sql)
        Pattern scriptPattern = Pattern.compile("^" + scriptLocationPath + File.separator + "(.*)" + File.separator
                + ".*\\" + SQL_MIGRATION_SUFFIX + "$");
        // Retrieve all modules (scripts are into <module> dir)
        Set<String> modules = new HashSet<>();
        for (Resource script : sqlScripts) {
            // Match script from relative path
            Matcher matcher = scriptPattern.matcher(script.getAbsolutePath());
            if (matcher.matches()) {
                modules.add(matcher.group(1));
            } else {
                LOGGER.warn("Cannot retrieve module name in resource {}. Format must conform to {}",
                            script.getAbsolutePath(), scriptPattern.toString());
            }
        }
        // Apply dependency check
        List<DatabaseModule> depModules = buildDatabaseModules(modules);

        // Apply module migration on sorted modules
        depModules.forEach(module -> migrate(dataSource, schema, module.getName()));
    }

    /**
     * Build database module tree and sort all modules by priority
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

            // Retrieve module properties
            Properties ppties = getModuleProperties(dbModule.getName());
            String depPpty = ppties.getProperty("module.dependencies");
            if (depPpty != null && !depPpty.isEmpty()) {
                for (String depModule : depPpty.split(",")) {
                    // Retrieve database module
                    DatabaseModule depDbModule = moduleMap.get(depModule);
                    if (depDbModule == null) {
                        LOGGER.warn("Dependent module \"{}\" of module \"{}\" not found in classpath", depModule,
                                    dbModule.getName());
                    } else {
                        LOGGER.debug("Dependency found for module \"{}\": \"{}\"", dbModule.getName(),
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
     * @param module name of the module
     * @return {@link Properties}
     */
    private Properties getModuleProperties(String module) {
        Properties ppties = new Properties();

        try (InputStream input = classLoader.getResourceAsStream(scriptLocationPath + File.separator + module
                + File.separator + "dbmodule.properties")) {
            if (input == null) {
                LOGGER.info("No module property found for module \"{}\"", module);
            } else {
                ppties.load(input);
            }
        } catch (IOException e) {
            LOGGER.error("Error reading or closing database module properties", e);
        }
        return ppties;
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
}
