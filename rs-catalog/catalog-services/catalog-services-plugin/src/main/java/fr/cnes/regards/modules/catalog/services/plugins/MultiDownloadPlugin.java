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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.utils.file.DownloadUtils;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.annotations.CatalogServicePlugin;
import fr.cnes.regards.modules.catalog.services.domain.plugins.IEntitiesServicePlugin;
import fr.cnes.regards.modules.catalog.services.helper.CatalogPluginResponseFactory;
import fr.cnes.regards.modules.catalog.services.helper.CatalogPluginResponseFactory.CatalogPluginResponseType;
import fr.cnes.regards.modules.catalog.services.helper.IServiceHelper;
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

    @PluginParameter(label = "Maximum number of files", name = "maxFilesToDownload", defaultValue = "1000",
            description = "Maximum number of files that this plugin allow to download.")
    private int maxFilesToDownload;

    private ResponseEntity<StreamingResponseBody> apply(List<DataObject> dataObjects, HttpServletResponse response) {
        Set<DataFile> toDownloadFiles = Sets.newHashSet();
        if ((dataObjects != null) && !dataObjects.isEmpty()) {
            dataObjects.forEach(dataObject -> addOnlineFiles(dataObject, toDownloadFiles));
        }

        if (toDownloadFiles.isEmpty()) {
            return CatalogPluginResponseFactory
                    .createSuccessResponse(response, CatalogPluginResponseType.JSON,
                                           "None of the selected files are available for download");
        } else {
            return CatalogPluginResponseFactory.createStreamSuccessResponse(response, getFilesAsZip(toDownloadFiles),
                                                                            "download.zip",
                                                                            MediaType.APPLICATION_OCTET_STREAM);
        }
    }

    @Override
    public ResponseEntity<StreamingResponseBody> applyOnEntities(List<String> pEntitiesId,
            HttpServletResponse response) {
        Page<DataObject> results = serviceHelper.getDataObjects(pEntitiesId, 0, maxFilesToDownload);
        long nbResults = results.getTotalElements();
        if (nbResults > maxFilesToDownload) {
            return CatalogPluginResponseFactory.createSuccessResponse(response, CatalogPluginResponseType.JSON,
                                                                      String.format("Number of files to download %d exceed maximum allowed of %d",
                                                                                    nbResults, maxFilesToDownload));
        } else {
            return apply(results.getContent(), response);
        }
    }

    @Override
    public ResponseEntity<StreamingResponseBody> applyOnQuery(String pOpenSearchQuery, EntityType pEntityType,
            HttpServletResponse response) {
        Page<DataObject> results;
        try {
            results = serviceHelper.getDataObjects(pOpenSearchQuery, 0, maxFilesToDownload);
            long nbResults = results.getTotalElements();
            if (nbResults > maxFilesToDownload) {
                return CatalogPluginResponseFactory.createSuccessResponse(response, CatalogPluginResponseType.JSON,
                                                                          String.format("Number of files to download %d exceed maximum allowed of %d",
                                                                                        nbResults, maxFilesToDownload));
            } else {
                return apply(results.getContent(), response);
            }
        } catch (OpenSearchParseException e) {
            String message = String.format("Error applying service. OpenSearchQuery is not a valid query : %s",
                                           pOpenSearchQuery);
            LOGGER.error(message, e);
            return CatalogPluginResponseFactory.createSuccessResponse(response, CatalogPluginResponseType.JSON,
                                                                      message);
        }

    }

    /**
     * Add into the files parameter all the online {@link DataFile} of the given {@link DataObject}
     * @param dataObject
     * @param files
     */
    private void addOnlineFiles(DataObject dataObject, Set<DataFile> files) {
        if ((dataObject != null) && (dataObject.getFiles() != null)) {
            dataObject.getFiles().forEach((type, file) -> {
                if (DataType.RAWDATA.equals(type) && Boolean.TRUE.equals(file.getOnline()) && (file.getUri() != null)) {
                    files.add(file);
                }
            });
        }
    }

    /**
     * Create a StreamingResponseBody by writting a ZIP containing all given {@link DataFile}
     * @param files {@link DataFile}s to write into the ZIP.
     * @return {@link StreamingResponseBody}
     */
    private StreamingResponseBody getFilesAsZip(Set<DataFile> files) {
        return (StreamingResponseBody) outputStream -> {
            try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
                for (DataFile file : files) {
                    String fileName = file.getName();
                    if (fileName == null) {
                        fileName = file.getUri().getPath();
                    }
                    zos.putNextEntry(new ZipEntry(fileName));
                    try {
                        ByteStreams.copy(DownloadUtils.getInputStream(new URL(file.getUri().toString())), zos);
                    } catch (IOException e) {
                        LOGGER.error(String.format("Error downloading file %s", file.getUri().toString()), e);
                    } finally {
                        zos.closeEntry();
                    }
                }
            }
        };
    }

}
