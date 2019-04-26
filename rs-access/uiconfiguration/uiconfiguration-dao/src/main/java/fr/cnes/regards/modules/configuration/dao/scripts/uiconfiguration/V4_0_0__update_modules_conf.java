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
package fr.cnes.regards.modules.configuration.dao.scripts.uiconfiguration;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;

/**
 * @author sbinda
 *
 */
public class V4_0_0__update_modules_conf extends BaseJavaMigration {

    /* (non-Javadoc)
     * @see org.flywaydb.core.api.migration.JavaMigration#migrate(org.flywaydb.core.api.migration.Context)
     */
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement select = context.getConnection().createStatement()) {
            try (ResultSet rows = select.executeQuery("SELECT id, conf FROM t_ui_module ORDER BY id")) {
                while (rows.next()) {
                    int id = rows.getInt(1);
                    String conf = rows.getString(2);
                    String updatedConf = updateConf(conf);
                    try (Statement update = context.getConnection().createStatement()) {
                        update.execute("UPDATE t_ui_module SET conf='" + updatedConf + "' WHERE id=" + id);
                    }
                }
            }
        }
    }

    public static String updateConf(String conf) {
        Gson gson = new Gson();
        Map<String, Object> rootConf = gson.fromJson(conf, Map.class);

        // 1. Search results conf
        updateSearchResult((Map<String, Object>) rootConf.get("searchResult"));

        return gson.toJson(rootConf);
    }

    public static void updateSearchResult(Map<String, Object> searchResultConfRoot) {
        searchResultConfRoot.put("viewsGroup", createViewsGroup(searchResultConfRoot));
    }

    public static Map<String, Object> createTitle(String en, String fr) {
        Map<String, Object> title = Maps.newHashMap();
        title.put("en", en);
        title.put("fr", fr);
        return title;
    }

    public static Map<String, Object> createFacets(Boolean enabled, Boolean initialViewMode) {
        Map<String, Object> facets = Maps.newHashMap();
        facets.put("enabled", enabled);
        facets.put("initialViewMode", initialViewMode);
        return facets;
    }

    public static Map<String, Object> createViews(List<Object> tableColumns) {

        Map<String, Object> views = Maps.newHashMap();
        Map<String, Object> view = Maps.newHashMap();

        view.put("enabled", true);
        if (tableColumns != null) {
            view.put("attributes", tableColumns);
        } else {
            view.put("attributes", Lists.newArrayList());
        }

        views.put("TABLE", view);
        views.put("LIST", view);
        return views;
    }

    public static Map<String, Object> createViewsGroup(Map<String, Object> rootConf) {
        Map<String, Object> viewGroups = Maps.newHashMap();
        String displayMode = (String) rootConf.get("displayMode");
        Boolean facets = (Boolean) rootConf.get("facettesInitiallySelected");
        Boolean enableDownload = (Boolean) rootConf.get("enableDownload");
        Boolean enableFacettes = (Boolean) rootConf.get("enableFacettes");
        String initialViewMode = (String) rootConf.get("initialViewMode");

        Boolean dataViewEnabled = (displayMode == "data") || (displayMode == "data_dataset");
        Boolean datasetViewEnabled = (displayMode == "data_dataset");
        Boolean documentViewEnabled = (displayMode == "document");

        String dataSectionLabelEn = (String) rootConf.get("dataSectionLabelEn");
        String dataSectionLabelFr = (String) rootConf.get("dataSectionLabelFr");

        String datasetsSectionLabelEn = (String) rootConf.get("datasetsSectionLabelEn");
        String datasetsSectionLabelFr = (String) rootConf.get("datasetsSectionLabelFr");
        Map<String, Object> data = (Map<String, Object>) rootConf.get("data");
        List<Object> dataColumns = Lists.newArrayList();
        if ((data != null) && (data.get("columns") != null)) {
            dataColumns = (List<Object>) data.get("columns");
            List<Object> dataFacets = (List<Object>) data.get("facets");
            rootConf.remove("data");
        }
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
        dataGroup.put("facets", createFacets(enableFacettes, facets));
        dataGroup.put("title", createTitle(dataSectionLabelEn, dataSectionLabelFr));
        dataGroup.put("views", createViews(dataColumns));
        dataGroup.put("initialMode", "TABLE");
        dataGroup.put("sorting", Lists.newArrayList());
        dataGroup.put("tabTitle", createTitle("", ""));

        Map<String, Object> datasetGroup = Maps.newHashMap();
        datasetGroup.put("enabled", datasetViewEnabled);
        datasetGroup.put("initialViewMode", initialViewMode);
        datasetGroup.put("title", createTitle(datasetsSectionLabelEn, datasetsSectionLabelFr));
        dataGroup.put("views", createViews(datasetColumns));
        datasetGroup.put("initialMode", "TABLE");
        datasetGroup.put("sorting", Lists.newArrayList());
        datasetGroup.put("tabTitle", createTitle("", ""));

        Map<String, Object> documentGroup = Maps.newHashMap();
        documentGroup.put("enabled", documentViewEnabled);
        documentGroup.put("initialViewMode", initialViewMode);
        documentGroup.put("views", createViews(documentColumns));
        documentGroup.put("initialMode", "TABLE");
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
