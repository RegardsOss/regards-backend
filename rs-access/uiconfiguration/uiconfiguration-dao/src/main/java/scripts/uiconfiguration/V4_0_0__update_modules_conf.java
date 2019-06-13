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
package scripts.uiconfiguration;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;

/**
 * @author sbinda
 *
 */
public class V4_0_0__update_modules_conf extends BaseJavaMigration {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(V4_0_0__update_modules_conf.class);

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement select = context.getConnection().createStatement()) {
            try (ResultSet rows = select.executeQuery("SELECT id, type, conf FROM t_ui_module ORDER BY id")) {
                while (rows.next()) {
                    int id = rows.getInt(1);
                    String type = rows.getString(2);
                    String conf = rows.getString(3);
                    String updatedConf = conf;
                    switch (type) {
                        case "search-graph":
                        case "search-form":
                            updatedConf = updateConf(conf, id);
                            try (Statement update = context.getConnection().createStatement()) {
                                update.execute("UPDATE t_ui_module SET conf='" + updatedConf + "' WHERE id=" + id);
                            }
                            break;
                        case "search-results":
                            updatedConf = updateSearchResult(conf, id);
                            try (Statement update = context.getConnection().createStatement()) {
                                update.execute("UPDATE t_ui_module SET conf='" + updatedConf + "' WHERE id=" + id);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static String updateSearchResult(String conf, int id) {
        Gson gson = new Gson();
        Map<String, Object> rootConf = gson.fromJson(conf, Map.class);
        updateSearchResult(rootConf, id);
        return gson.toJson(rootConf);
    }

    @SuppressWarnings("unchecked")
    public static String updateConf(String conf, int id) {
        Gson gson = new Gson();
        Map<String, Object> rootConf = gson.fromJson(conf, Map.class);

        // 1. Search results conf
        updateSearchResult((Map<String, Object>) rootConf.get("searchResult"), id);

        return gson.toJson(rootConf);
    }

    public static void updateSearchResult(Map<String, Object> searchResultConfRoot, Integer id) {
        if (searchResultConfRoot.get("viewsGroups") == null) {
            LOG.info("---> UI Module Migration V4 for conf {} !", id);
            searchResultConfRoot.put("viewsGroups", createViewsGroup(searchResultConfRoot));
        } else {
            // Do not update, the configuration is already in the good version
            LOG.info("---> UI Module Migration V4 skipped for conf {} already in V4 version", id);
        }
    }

    public static Map<String, Object> createTitle(String en, String fr) {
        Map<String, Object> title = Maps.newHashMap();
        title.put("en", en);
        title.put("fr", fr);
        return title;
    }

    public static Map<String, Object> createFacets(Boolean enabled, Boolean initialViewMode, List<Object> facets) {
        Map<String, Object> facetsObject = Maps.newHashMap();
        facetsObject.put("enabled", enabled);
        facetsObject.put("initiallyEnabled", initialViewMode);
        facetsObject.put("list", facets);
        return facetsObject;
    }

    public static Map<String, Object> createViews(List<Object> tableColumns) {

        Map<String, Object> views = Maps.newHashMap();
        Map<String, Object> tableView = Maps.newHashMap();
        Map<String, Object> qlView = Maps.newHashMap();
        Map<String, Object> mapView = Maps.newHashMap();

        if (tableColumns != null) {
            tableView.put("attributes", tableColumns);
            tableView.put("enabled", !tableColumns.isEmpty());
        } else {
            tableView.put("attributes", Lists.newArrayList());
            tableView.put("enabled", false);
        }

        qlView.put("attributes", Lists.newArrayList());
        qlView.put("enabled", false);

        mapView.put("attributes", Lists.newArrayList());
        mapView.put("enabled", false);

        views.put("TABLE", tableView);
        views.put("QUICKLOOK", qlView);
        views.put("MAP", mapView);

        return views;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> createViewsGroup(Map<String, Object> rootConf) {
        Map<String, Object> viewGroups = Maps.newHashMap();
        String displayMode = (String) rootConf.get("displayMode");
        Boolean facets = (Boolean) rootConf.get("facettesInitiallySelected");
        Boolean enableDownload = (Boolean) rootConf.get("enableDownload");
        Boolean enableFacettes = (Boolean) rootConf.get("enableFacettes");

        Boolean dataViewEnabled = (displayMode.equals("data")) || (displayMode.equals("data_dataset"));
        Boolean datasetViewEnabled = (displayMode.equals("data_dataset"));
        Boolean documentViewEnabled = (displayMode.equals("document"));

        String dataSectionLabelEn = (String) rootConf.get("dataSectionLabelEn");
        String dataSectionLabelFr = (String) rootConf.get("dataSectionLabelFr");

        String datasetsSectionLabelEn = (String) rootConf.get("datasetsSectionLabelEn");
        String datasetsSectionLabelFr = (String) rootConf.get("datasetsSectionLabelFr");
        Map<String, Object> data = (Map<String, Object>) rootConf.get("data");
        List<Object> dataColumns = Lists.newArrayList();
        if ((data != null) && (data.get("columns") != null)) {
            dataColumns = (List<Object>) data.get("columns");
        }
        List<Object> dataSorting = Lists.newArrayList();
        if ((data != null) && (data.get("sorting") != null)) {
            dataSorting = (List<Object>) data.get("sorting");
        }
        List<Object> dataFacets = Lists.newArrayList();
        if ((data != null) && (data.get("facets") != null)) {
            dataFacets = (List<Object>) data.get("facets");
        }
        rootConf.remove("data");

        Map<String, Object> dataset = (Map<String, Object>) rootConf.get("dataset");
        List<Object> datasetColumns = Lists.newArrayList();
        if ((dataset != null) && (dataset.get("columns") != null)) {
            datasetColumns = (List<Object>) dataset.get("columns");
            rootConf.remove("dataset");
        }

        Map<String, Object> document = (Map<String, Object>) rootConf.get("document");
        List<Object> documentColumns = Lists.newArrayList();
        if ((document != null) && (document.get("columns") != null)) {
            documentColumns = (List<Object>) document.get("columns");
            rootConf.remove("document");
        }

        rootConf.remove("displayConf");
        rootConf.remove("facettesInitiallySelected");
        rootConf.remove("enableFacettes");
        rootConf.remove("enableQuicklooks");
        rootConf.remove("enableDownload");
        rootConf.remove("initialViewMode");
        rootConf.remove("displayMode");
        rootConf.remove("dataSectionLabelEn");
        rootConf.remove("dataSectionLabelFr");
        rootConf.remove("datasetsSectionLabelEn");
        rootConf.remove("datasetsSectionLabelFr");

        Map<String, Object> dataGroup = Maps.newHashMap();
        dataGroup.put("enabled", dataViewEnabled);
        dataGroup.put("enableDownload", enableDownload);
        dataGroup.put("facets", createFacets(enableFacettes, facets, dataFacets));
        dataGroup.put("views", createViews(dataColumns));
        dataGroup.put("initialMode", "TABLE");
        dataGroup.put("sorting", dataSorting);
        dataGroup.put("tabTitle", createTitle(dataSectionLabelEn, dataSectionLabelFr));

        Map<String, Object> datasetGroup = Maps.newHashMap();
        datasetGroup.put("enabled", datasetViewEnabled);
        datasetGroup.put("views", createViews(datasetColumns));
        datasetGroup.put("initialMode", "LIST");
        datasetGroup.put("sorting", Lists.newArrayList());
        datasetGroup.put("tabTitle", createTitle(datasetsSectionLabelEn, datasetsSectionLabelFr));

        Map<String, Object> documentGroup = Maps.newHashMap();
        documentGroup.put("enabled", documentViewEnabled);
        documentGroup.put("views", createViews(documentColumns));
        documentGroup.put("initialMode", "LIST");
        documentGroup.put("sorting", Lists.newArrayList());
        documentGroup.put("tabTitle", createTitle("", ""));

        rootConf.remove("displayMode");
        rootConf.remove("facettesInitiallySelected");
        rootConf.remove("enableDownload");
        rootConf.remove("initialViewMode");
        rootConf.remove("enableFacettes");

        viewGroups.put("DATA", dataGroup);
        viewGroups.put("DATASET", datasetGroup);
        viewGroups.put("DOCUMENT", documentGroup);

        return viewGroups;
    }

}
