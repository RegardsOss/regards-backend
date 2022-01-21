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
package scripts.uiconfiguration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

/**
 * Migrates modules configuration from previous REGARDS version into 1.2.0 (for search related changes)
 *
 * @author Raphaël Mechali
 */
public class V1_2_0__update_modules_conf extends BaseJavaMigration {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(V1_2_0__update_modules_conf.class);

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
     * Updates search graph configuration
     *
     * @param initialConfiguration initial configuration
     * @return updated configuration as map
     */
    public static Map<String, Object> updateSearchGraphConfiguration(Map<String, Object> initialConfiguration) {
        // perform inner update, by reference, on search result part
        updateSearchResultsConfiguration((Map<String, Object>) initialConfiguration.get("searchResult"));
        return initialConfiguration;
    }

    /**
     * Updates search graph configuration
     *
     * @param initialConfiguration initial configuration
     * @return updated configuration as map
     */
    public static Map<String, Object> updateSearchResultsConfiguration(Map<String, Object> initialConfiguration) {
        // Append new key for criteria groups in search results
        initialConfiguration.put("criteriaGroups", new String[0]);
        return initialConfiguration; // return initial configuration (updated by reference)
    }

    /**
     * Updates search graph configuration
     *
     * @param initialConfiguration initial configuration
     * @return updated configuration as map
     */
    public static Map<String, Object> updateSearchFormConfiguration(Map<String, Object> initialConfiguration) {
        // transform search form into new search results: remove upper levels and convert old search-form
        // criteria into new search-results criteria (in an untitled root criterion group)
        Map<String, Object> searchResultsConfiguration = (Map<String, Object>) initialConfiguration.get("searchResult");
        List<Map<String, Object>> criteria = (List<Map<String, Object>>) initialConfiguration.get("criterion");
        Map<String, Object> rootGroup = new HashMap<>();
        Map<String, Object> groupTitle = new HashMap<>();
        groupTitle.put("en", "");
        groupTitle.put("fr", "");
        rootGroup.put("showTitle", false);
        rootGroup.put("title", groupTitle);
        if (criteria != null) {
            rootGroup.put("criteria", criteria.stream().map(V1_2_0__update_modules_conf::updateCriterionConfiguration)
                    .collect(Collectors.toList()));
        } else {
            rootGroup.put("criteria", Lists.newArrayList());
        }
        searchResultsConfiguration.put("criteriaGroups", Collections.singletonList(rootGroup));
        return searchResultsConfiguration;
    }

    /**
     * Updates a criterion configuration from previous search-form module criterion into new search-results module criterion
     *
     * @param initialConfiguration initial configuration
     * @return updated configuration
     */
    private static Map<String, Object> updateCriterionConfiguration(Map<String, Object> initialConfiguration) {
        // pluginId and conf are reported, container, active and pluginInstanceId are removed
        Map<String, Object> newConfiguration = new HashMap<>();
        newConfiguration.put("pluginId", initialConfiguration.get("pluginId"));
        newConfiguration.put("conf", initialConfiguration.get("conf"));
        // Add a label indicating to configure criteria
        Map<String, Object> criterionLabel = new HashMap<>();
        criterionLabel.put("en", "");
        criterionLabel.put("fr", "");
        newConfiguration.put("label", criterionLabel);
        return newConfiguration;
    }

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement select = context.getConnection().createStatement()) {
            try (ResultSet rows = select.executeQuery("SELECT id, type, conf FROM t_ui_module ORDER BY id")) {
                while (rows.next()) {
                    int id = rows.getInt(1);
                    String type = rows.getString(2);
                    String conf = rows.getString(3);
                    String updatedConf = null;
                    String newType = type;

                    switch (type) {
                        case "search-results":
                            updatedConf = withParsedMap(conf,
                                                        V1_2_0__update_modules_conf::updateSearchResultsConfiguration);
                            break;
                        case "search-graph":
                            updatedConf = withParsedMap(conf,
                                                        V1_2_0__update_modules_conf::updateSearchGraphConfiguration);
                            break;
                        case "search-form":
                            updatedConf = withParsedMap(conf,
                                                        V1_2_0__update_modules_conf::updateSearchFormConfiguration);
                            newType = "search-results";// also migrating to search-results type
                            break;
                        default:
                            // No update for other module types
                            break;
                    }
                    if (updatedConf != null) {
                        LOG.info(String.format("Updating module %s from type %s to type %s", id, type, newType));
                        String sqlRequest = "UPDATE t_ui_module SET conf=? , type=? WHERE id=?";
                        try (PreparedStatement preparedStatement = context.getConnection()
                                .prepareStatement(sqlRequest)) {
                            preparedStatement.setString(1, updatedConf);
                            preparedStatement.setString(2, newType);
                            preparedStatement.setInt(3, id);
                            preparedStatement.executeUpdate();
                        }
                    }

                }
            }
        }
    }

}
