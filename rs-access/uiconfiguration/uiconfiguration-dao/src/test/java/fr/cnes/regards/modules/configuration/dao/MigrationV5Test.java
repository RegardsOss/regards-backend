package fr.cnes.regards.modules.configuration.dao;

import com.google.gson.Gson;
import org.junit.Ignore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This test holds migrating script from v4+ to v1.0.x. It is by no way meant to be a test...
 *
 * @author Raphaël Mechali
 */
public class MigrationV5Test {

    /**
     * Builds updated configuration
     *
     * @param configuration configuration to update as string (from database)
     * @param updater       function to update initial configuration map
     * @return updated configuration as string (to DB)
     */
    @SuppressWarnings("unchecked")
    public static String buildUpdatedConfiguration(String configuration, Consumer<Map<String, Object>> updater) {
        Gson gson = new Gson();
        // 1 - Produce a mutable reference on JSON content
        Map<String, Object> confAsMap = gson.fromJson(configuration, Map.class);
        // 2 - Update map content
        updater.accept(confAsMap);
        // 3 - Serialize
        return gson.toJson(confAsMap);
    }

    @SuppressWarnings("unchecked")
    public static void updateSearchGraphConf(Map<String, Object> configuration) {
        updateSearchResultsConf((Map<String, Object>) configuration.get("searchResult"));
    }

    @SuppressWarnings("unchecked")
    private static void updateSearchFormConf(Map<String, Object> configuration) {
        updateSearchResultsConf((Map<String, Object>) configuration.get("searchResult"));
        // migrate restriction (for datesets only, dataset model ID is no longer used)
        Map<String, Object> oldRestrictions = (Map<String, Object>) configuration.remove("datasets");
        String oldRestrictionsType = oldRestrictions != null ? (String) oldRestrictions.get("type") : "all";
        Map<String, Object> newRestrictions = null;
        Map<String, Object> byDataset = new HashMap<>();
        switch (oldRestrictionsType) {
            case "selectedDatasets":
                newRestrictions = new HashMap<>();
                byDataset.put("type", "SELECTED_DATASETS");
                byDataset.put("selection", oldRestrictions.get("selectedDatasets"));
                newRestrictions.put("byDataset", byDataset);
                break;
            case "seletedDatasetModels":
                newRestrictions = new HashMap<>();
                byDataset.put("type", "SELECTED_MODELS");
                byDataset.put("selection", new String[0]);
                newRestrictions.put("byDataset", byDataset);
                break;
            case "all":
            default: // nothing to do
        }
        // commit converted restrictions, if there are any
        if (newRestrictions != null) {
            Map<String, Object> searchResultsConf = (Map<String, Object>) configuration.get("searchResult");
            searchResultsConf.put("restrictions", newRestrictions);
        }
    }

    @SuppressWarnings("unchecked")
    public static void updateSearchResultsConf(Map<String, Object> configuration) {
        if (configuration.get("facets") == null) {
            // A - Make sure only data and dataset are present in configuration (documents removal)
            Map<String, Object> viewsGroups = (Map<String, Object>) configuration.get("viewsGroups");
            Map<String, Object> documentView = (Map<String, Object>) viewsGroups.remove("DOCUMENT");
            viewsGroups.remove("COLLECTION");
            // B - When documents view is enabled, replace data view by document view (it was previously a document catalog)
            if (documentView != null && (Boolean) documentView.get("enabled")) {
                viewsGroups.put("DATA", documentView);
            }
            // C - Report data facets configuration in new root field "facets"
            Map<String, Object> dataView = (Map<String, Object>) viewsGroups.get("DATA");
            Map<String, Object> facets = (Map<String, Object>) dataView.remove("facets");
            if (facets == null) {
                // C.1 - default value: disabled. Initialize whole facets field
                facets = new HashMap<>();
                Map<String, Object> enabledFor = new HashMap<>();
                enabledFor.put("DATA", false);
                enabledFor.put("DATASET", false);
                facets.put("enabledFor", enabledFor);
                facets.put("initiallyEnabled", false);
                facets.put("list", new Object[0]);
            } else {
                // C.2 - report enabled into enabledFor.DATA field (initialize enabledFor field)
                Boolean dataEnabled = (Boolean) facets.remove("enabled");
                Map<String, Object> enabledFor = new HashMap<>();
                enabledFor.put("DATA", dataEnabled == null ? false : dataEnabled);
                enabledFor.put("DATASET", false);
                facets.put("enabledFor", enabledFor);
            }
            configuration.put("facets", facets);
            // D  - add the default restrictions value
            Map<String, Object> restrictions = new HashMap<>();
            Map<String, Object> byDataset = new HashMap<>();
            byDataset.put("type", "NONE");
            byDataset.put("selection", new String[0]);
            restrictions.put("byDataset", byDataset);
            configuration.put("restrictions", restrictions);
        }
    }

    @SuppressWarnings("unchecked")
    public static void updateDescriptionConf(Map<String, Object> configuration) {
        if (configuration.get("allowSearching") == null) {
            // A - relocate allow searching field
            configuration.put("allowSearching", configuration.remove("allowTagSearch"));
            // Add missing fields in each type configuration
            String[] allTypes = { "DATA", "DATASET", "COLLECTION", "DOCUMENT" };
            for (String t : allTypes) {
                Map<String, Object> typeConfiguration = (Map<String, Object>) configuration.get(t);
                typeConfiguration.put("hideEmptyAttributes", false);
                typeConfiguration.put("showCoupling", true);
                typeConfiguration.put("showLinkedEntities", true);
                typeConfiguration.put("attributeToDescriptionFiles", new Object[0]);
            }
        }
    }

    public static void noUpdate(Map<String, Object> configuration) {
        // nothing to do
    }

    public static String retreatConfiguration(String built) {
        // remove doubles conversion from gson...
        return built.replaceAll("\\.0", "");
    }

    public static void writeConfFile(String outputFolder, String conf, int fileIndex, boolean backup)
        throws IOException {
        // A - Build file name: .../module_{index}{_backup?}.json
        String outputFile = outputFolder + (outputFolder.endsWith("/") ? "" : "/") + "module_" + fileIndex + (backup ?
            "_backup" :
            "") + ".json";
        Files.write(Paths.get(outputFile), conf.getBytes());
    }

    public static Object extractValue(Map<String, Object> source, List<String> path) {
        String nextField = path.remove(0);
        Object nextSource = source.get(nextField);
        if (nextSource == null || path.isEmpty()) {
            return nextSource;
        }
        return extractValue((Map<String, Object>) nextSource, path);
    }

    public static String toSQLStringValue(Map<String, Object> source, String field) {
        String s = (String) extractValue(source, new ArrayList<>(Arrays.asList(field.split("\\."))));
        return s == null ? "NULL" : "'" + s + "'";
    }

    public static String toSQLBoolValue(Map<String, Object> source, String field) {
        Boolean b = (Boolean) extractValue(source, new ArrayList<>(Arrays.asList(field.split("\\."))));
        return b == null || !b ? "FALSE" : "TRUE";
    }

    public static void writeToSQL(String outputFolder, List<Map<String, Object>> modules) throws IOException {
        String sqlInsertionScript = "DELETE FROM accessproject.t_ui_module;\n"
                                    + "INSERT INTO accessproject.t_ui_module(id,type,description,container,active,applicationid,home,customiconurl,icontype,title,conf)\n"
                                    + "VALUES\n";
        List<String> insertedModules = new ArrayList<>(modules.size());
        for (Map<String, Object> module : modules) {
            Map<String, Object> moduleContent = (Map<String, Object>) module.get("content");
            List<String> values = new ArrayList<>(11);
            values.add("nextval('accessproject.seq_ui_module')"); // ID
            values.add(toSQLStringValue(moduleContent, "type")); // type
            values.add(toSQLStringValue(moduleContent, "description")); // description
            values.add(toSQLStringValue(moduleContent, "container")); // container
            values.add(toSQLBoolValue(moduleContent, "active")); // active
            values.add(toSQLStringValue(moduleContent, "applicationId")); // application ID
            values.add(toSQLBoolValue(moduleContent, "page.home")); // home
            values.add(toSQLStringValue(moduleContent, "page.customIconURL")); // custom icon URL
            values.add(toSQLStringValue(moduleContent, "page.iconType"));
            values.add(toSQLStringValue(moduleContent, "page.title")); // title
            values.add(toSQLStringValue(moduleContent, "conf")); // conf

            insertedModules.add("\t(" + String.join(", ", values) + ")");
        }
        sqlInsertionScript += String.join(",\n", insertedModules) + ";";
        String outputSQLFile = outputFolder + (outputFolder.endsWith("/") ? "" : "/") + "insert_updated_modules.sql";
        Files.write(Paths.get(outputSQLFile), sqlInsertionScript.getBytes());
    }

    public static void v04toV1Configuration(String modulesDumpPath,
                                            String outputFolder,
                                            boolean backup,
                                            boolean newConf) throws IOException {
        // 1 - Extract modules list from dump
        Gson gson = new Gson();
        String fileAsString = new String(Files.readAllBytes(Paths.get(modulesDumpPath)));
        Map<String, Object> confAsMap = gson.fromJson(fileAsString, Map.class);
        List<Map<String, Object>> modules = (List<Map<String, Object>>) confAsMap.get("content");

        // 2 - Fo' each module entry, generate file after update
        int fileIndex = 0;
        for (Map<String, Object> module : modules) {
            Map<String, Object> content = (Map<String, Object>) module.get("content");
            int id = (int) Math.round((Double) content.get("id"));
            String type = (String) content.get("type");
            String conf = (String) content.get("conf");
            Consumer<Map<String, Object>> updater = null;
            // 1 - Compute configuration to update and corresponding updated configuration
            switch (type) {
                case "search-graph":
                    updater = MigrationV5Test::updateSearchGraphConf;
                    break;
                case "search-form":
                    updater = MigrationV5Test::updateSearchFormConf;
                    break;
                case "search-results":
                    updater = MigrationV5Test::updateSearchResultsConf;
                    break;
                case "description":
                    updater = MigrationV5Test::updateDescriptionConf;
                    break;
                default:
                    updater = MigrationV5Test::noUpdate;
            }
            // 2 - Serialize file configuration
            if (backup) {
                writeConfFile(outputFolder, conf, fileIndex, true);
            }
            String updatedConf = retreatConfiguration(buildUpdatedConfiguration(conf, updater));
            if (newConf) {
                writeConfFile(outputFolder, updatedConf, fileIndex, false);
            }
            fileIndex++;
            // 3 - Update module as well for later SQL insert statement
            content.put("conf", updatedConf);
        }
        // All modules where updated: serialize into an sql script
        writeToSQL(outputFolder, modules);
    }

    @Ignore
    public void generateConfigurationsFiles() throws Exception {
        // Note: fake test, just example for migrator (here from folder with all migrated elements)
        List<Path> inputDumpFiles = Files.list(Paths.get("/home/rmechali/Bureau/tempo/input"))
                                         .collect(Collectors.toList());
        for (Path p : inputDumpFiles) {
            // A - Create folder for that project
            String fileName = p.getFileName().toString();
            String[] parts = fileName.split("\\.");
            String outputFolderName = parts[0];
            File outputFolder = new File("/home/rmechali/Bureau/tempo/output/" + outputFolderName);
            if (!outputFolder.exists()) {
                if (!outputFolder.mkdir()) {
                    throw new RuntimeException("Failed creating folder at " + outputFolder.getAbsolutePath());
                }
            }
            // Parameter #1: Modules list dump, from rs-access/modules (root, with both content and metadata fields)
            // Parameter #2: Path to the folder that will hold backup, updated files and insertion script
            // Parameter #3: Should write on HDD backup configuration files?
            // Parameter #4: Should write on HDD new configuration files?
            v04toV1Configuration("/home/rmechali/Bureau/tempo/input/" + p.getFileName(),
                                 outputFolder.getAbsolutePath(),
                                 true,
                                 true);

        }
    }

}
