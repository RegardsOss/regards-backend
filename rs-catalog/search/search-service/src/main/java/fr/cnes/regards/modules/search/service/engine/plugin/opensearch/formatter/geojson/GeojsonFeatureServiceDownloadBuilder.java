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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.geojson;

import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.DataFileHrefBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Léo Mieulet
 */
public final class GeojsonFeatureServiceDownloadBuilder {

    private GeojsonFeatureServiceDownloadBuilder() {
    }

    public static Map<String, Map<String, Object>> buildGeojsonServices(DataFile firstRawData, String scope) {
        Map<String, Map<String, Object>> services = new HashMap();
        services.put("download", getDownloadService(firstRawData, scope));
        return services;
    }

    private static Map<String, Object> getDownloadService(DataFile firstRawData, String scope) {
        Map<String, Object> downloadService = new HashMap();
        downloadService.put("url", DataFileHrefBuilder.getDataFileHref(firstRawData, scope));
        downloadService.put("mimeType", firstRawData.getMimeType());
        downloadService.put("size", firstRawData.getFilesize());
        downloadService.put("checksum", firstRawData.getChecksum());
        downloadService.put("fileName", firstRawData.getFilename());
        return downloadService;
    }
}
