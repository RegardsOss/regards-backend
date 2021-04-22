/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package scripts.uiconfiguration;

import com.google.gson.Gson;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Migrates modules configuration from previous REGARDS version into 1.3.0
 *
 * @author Raphaël Mechali
 */
public class V1_3_0__update extends BaseJavaMigration {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(V1_3_0__update.class);

    /**
     * Updates UI settings
     *
     * @param configuration initial settings configuration
     * @return updated UI settings configuration
     */
    public static Map<String, Object> updateUISettings(Map<String, Object> configuration) {
        configuration.put("showVersion", true);
        return configuration;
    }

    /**
     * Parses a JSON map from string and produce new configuration (a map too) as string
     *
     * @param configuration current configuration
     * @param updater       configuration updater
     * @return new configuration as string
     */
    public static String withParsedMap(String configuration,
                                       Function<Map<String, Object>, Map<String, Object>> updater) {
        Gson gson = new Gson();
        Map<String, Object> configurationAsMap = gson.fromJson(configuration, Map.class);
        // Update configuration
        Map<String, Object> updated = updater.apply(configurationAsMap);
        // serialize map after update
        String newConfiguration = gson.toJson(updated);
        // replace curiously handled numbers in GSon... (otherwise integers become float)
        return newConfiguration.replaceAll("\\.0", "");
    }

    /**
     * Updates description module: add showOtherVersion field by type
     *
     * @param initialConfiguration initial configuration
     * @return updated configuration as map
     */
    public static Map<String, Object> updateDescriptionModule(Map<String, Object> initialConfiguration) {
        // perform inner update, by reference, on each description type
        String[] pseudoEntityTypes = {"DATA", "DATASET", "COLLECTION", "DOCUMENT"};
        for (String type : pseudoEntityTypes) {
            Map<String, Object> typeConfiguration = (Map<String, Object>) initialConfiguration.get(type);
            typeConfiguration.put("showOtherVersions", false); // behaves as v1.2
            typeConfiguration.put("showQuicklooks", true); // behaves as v1.2
        }
        return initialConfiguration;
    }

    /**
     * Updates search results configuration: add restriction onData
     *
     * @param initialConfiguration initial configuration
     * @return updated configuration as map
     */
    public static Map<String, Object> updateSearchResultsConfiguration(Map<String, Object> initialConfiguration) {
        Map<String, Object> restrictions = (Map<String, Object>) initialConfiguration.get("restrictions");
        Map<String, Object> onData = new HashMap<>();
        onData.put("lastVersionOnly", false);
        restrictions.put("onData", onData);
        return initialConfiguration; // return initial configuration (updated by reference)
    }

    @Override
    public void migrate(Context context) throws Exception {
        this.migrateUISettings(context);
        this.migrateModules(context);
    }

    /**
     * Migrates modules
     * @param context flyway context
     */
    public void migrateModules(Context context) throws Exception {
        try (Statement select = context.getConnection().createStatement()) {
            try (ResultSet rows = select.executeQuery("SELECT id, type, conf FROM t_ui_module ORDER BY id")) {
                while (rows.next()) {
                    int id = rows.getInt(1);
                    String type = rows.getString(2);
                    String conf = rows.getString(3);
                    String updatedConf = null;
                    switch (type) {
                        case "search-results":
                            updatedConf = withParsedMap(conf,
                                    V1_3_0__update::updateSearchResultsConfiguration);
                            break;
                        case "description":
                            updatedConf = withParsedMap(conf,
                                    V1_3_0__update::updateDescriptionModule);
                            break;
                        default:
                            // No update for other module types
                            break;
                    }
                    if (updatedConf != null) {
                        LOG.info(String.format("Updating module %s (type %s)", id, type));
                        String sqlRequest = "UPDATE t_ui_module SET conf=? WHERE id=?";
                        try (PreparedStatement preparedStatement = context.getConnection()
                                .prepareStatement(sqlRequest)) {
                            preparedStatement.setString(1, updatedConf);
                            preparedStatement.setInt(2, id);
                            preparedStatement.executeUpdate();
                        }
                    }

                }
            }
        }
    }

    /**
     * Migrates UI settings
     * @param context flyway context
     */
    public void migrateUISettings(Context context) throws Exception {
        try (Statement select = context.getConnection().createStatement()) {
            try (ResultSet rows = select.executeQuery("SELECT id, application_id, configuration FROM t_ui_configuration ORDER BY id")) {
                while (rows.next()) {
                    int id = rows.getInt(1);
                    String applicationId = rows.getString(2);
                    String conf = rows.getString(3);

                    if (applicationId.equals("user")) {
                        String updatedConf = withParsedMap(conf, V1_3_0__update::updateUISettings);
                        LOG.info("Updating user application configuration");

                        String sqlRequest = "UPDATE t_ui_configuration SET configuration=? WHERE id=?";
                        try (PreparedStatement preparedStatement = context.getConnection()
                                .prepareStatement(sqlRequest)) {
                            preparedStatement.setString(1, updatedConf);
                            preparedStatement.setInt(2, id);
                            preparedStatement.executeUpdate();
                        }
                    }
                }
            }
        }
    }

}
