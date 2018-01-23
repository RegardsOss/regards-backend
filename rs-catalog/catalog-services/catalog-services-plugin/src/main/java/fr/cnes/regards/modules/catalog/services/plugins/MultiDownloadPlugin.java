/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.catalog.services.plugins;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.utils.file.DownloadUtils;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.annotations.CatalogServicePlugin;
import fr.cnes.regards.modules.catalog.services.domain.plugins.IEntitiesServicePlugin;
import fr.cnes.regards.modules.catalog.services.plugins.helper.CatalogPluginResponseFactory;
import fr.cnes.regards.modules.catalog.services.plugins.helper.IServiceHelper;
import fr.cnes.regards.modules.catalog.services.plugins.helper.CatalogPluginResponseFactory.CatalogPluginResponseType;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchParseException;

@Plugin(description = "Plugin to allow download on multiple data selection by creating an archive.",
        id = "MultiDownloadPlugin", version = "1.0.0", author = "REGARDS Team", contact = "regards@c-s.fr",
        licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
@CatalogServicePlugin(applicationModes = { ServiceScope.MANY }, entityTypes = { EntityType.DATA })
public class MultiDownloadPlugin implements IEntitiesServicePlugin {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogPluginResponseFactory.class);

    @Autowired
    private IServiceHelper serviceHelper;

    public static final int MAX_NUMBER_OF_RESULTS = 1000;

    private ResponseEntity<StreamingResponseBody> apply(List<DataObject> dataObjects, HttpServletResponse response) {
        Set<DataFile> toDownloadFiles = Sets.newHashSet();
        List<String> uris = new ArrayList<>();
        List<String> other = new ArrayList<>();
        if ((dataObjects != null) && !dataObjects.isEmpty()) {
            dataObjects.forEach(dataObject -> {
                dataObject.getFiles().forEach((type, file) -> {
                    if (DataType.RAWDATA.equals(type) && file.getOnline() && (file.getUri() != null)) {
                        // Add file to zipInputStream
                        toDownloadFiles.add(file);
                        uris.add(file.getUri().toString());
                    } else {
                        other.add(file.getName());
                    }
                });
            });
        }

        //        return CatalogPluginResponseFactory.createStreamSuccessResponse(response, getFilesAsZip(toDownloadFiles),
        //                                                                        "download.zip");
        return CatalogPluginResponseFactory.createSuccessResponse(response, CatalogPluginResponseType.JSON,
                                                                  new DownloadResponse(uris, other));
    }

    @Override
    public ResponseEntity<StreamingResponseBody> applyOnEntities(List<String> pEntitiesId,
            HttpServletResponse response) {
        Page<DataObject> results = serviceHelper.getDataObjects(pEntitiesId, 0, MAX_NUMBER_OF_RESULTS);
        if (results.getTotalElements() > MAX_NUMBER_OF_RESULTS) {
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        } else {
            return apply(results.getContent(), response);
        }
    }

    @Override
    public ResponseEntity<StreamingResponseBody> applyOnQuery(String pOpenSearchQuery, EntityType pEntityType,
            HttpServletResponse response) {
        Page<DataObject> results;
        try {
            results = serviceHelper.getDataObjects(pOpenSearchQuery, 0, MAX_NUMBER_OF_RESULTS);
            if (results.getTotalElements() > MAX_NUMBER_OF_RESULTS) {
                return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
            } else {
                return apply(results.getContent(), response);
            }
        } catch (OpenSearchParseException e) {
            LOGGER.error(String.format("Error applying service. OpenSearchQuery is not a valid query : %s",
                                       pOpenSearchQuery),
                         e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

    }

    private StreamingResponseBody getFilesAsZip(Set<DataFile> files) {
        return (StreamingResponseBody) outputStream -> {
            try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
                for (DataFile file : files) {
                    zos.putNextEntry(new ZipEntry(file.getName()));
                    ByteStreams.copy(DownloadUtils.getInputStream(new URL(file.getUri().toString())), zos);
                    zos.closeEntry();
                }
            }
        };
    }

}
