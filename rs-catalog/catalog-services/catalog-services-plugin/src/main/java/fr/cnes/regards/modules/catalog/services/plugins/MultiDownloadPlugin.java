/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
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
public class MultiDownloadPlugin extends AbstractCatalogServicePlugin implements IEntitiesServicePlugin {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogPluginResponseFactory.class);

    @Autowired
    private IServiceHelper serviceHelper;

    @Autowired
    private JWTService jwtService;

    @PluginParameter(label = "Maximum number of files", name = "maxFilesToDownload", defaultValue = "1000",
            description = "Maximum number of files that this plugin allow to download.")
    private int maxFilesToDownload;

    @PluginParameter(label = "Maximum total size for download (Mo)", name = "maxFilesSizeToDownload",
            defaultValue = "100", description = "Maximum total size of selected files for one download.")
    private int maxFilesSizeToDownload;

    @PluginParameter(label = "Archive file name", name = "archiveFileName", defaultValue = "download.zip",
            description = "Name of the archive containing all selected files for download.")
    private String archiveFileName;

    @Override
    public ResponseEntity<StreamingResponseBody> applyOnEntities(List<String> pEntitiesId,
            HttpServletResponse response) {
        Page<DataObject> results = serviceHelper.getDataObjects(pEntitiesId, 0, 10000);
        return apply(results.getContent(), response);
    }

    @Override
    public ResponseEntity<StreamingResponseBody> applyOnQuery(String pOpenSearchQuery, EntityType pEntityType,
            HttpServletResponse response) {
        Page<DataObject> results;
        try {
            results = serviceHelper.getDataObjects(pOpenSearchQuery, 0, 10000);
            return apply(results.getContent(), response);
        } catch (OpenSearchParseException e) {
            String message = String.format("Error applying service. OpenSearchQuery is not a valid query : %s",
                                           pOpenSearchQuery);
            LOGGER.error(message, e);
            return CatalogPluginResponseFactory.createSuccessResponse(response, CatalogPluginResponseType.JSON,
                                                                      message);
        }

    }

    /**
     * Global application for DataObjects datafiles download. A ZIPStream is created containing all onlines {@link DataFile}
     * for each fiven {@link DataObject}
     * @param dataObjects
     * @param response
     * @return
     */
    private ResponseEntity<StreamingResponseBody> apply(List<DataObject> dataObjects, HttpServletResponse response) {
        // Retrieve all onlines files from each DataObject
        Map<DataObject, Set<DataFile>> toDownloadFilesMap = Maps.newHashMap();
        if ((dataObjects != null) && !dataObjects.isEmpty()) {
            dataObjects.forEach(dataObject -> toDownloadFilesMap.put(dataObject, getOnlineFiles(dataObject)));
        }

        // Check for maximum number of files limit
        int nbFiles = toDownloadFilesMap.values().stream().mapToInt(list -> list.size()).sum();
        // If files number exceed maximum configured, return a JSON message with the error.
        LOGGER.debug(String.format("Number of files to download : %d", nbFiles));
        if (nbFiles > maxFilesToDownload) {
            return CatalogPluginResponseFactory.createSuccessResponse(response, CatalogPluginResponseType.JSON,
                                                                      String.format("Number of files to download %d exceed maximum allowed of %d",
                                                                                    nbFiles, maxFilesToDownload));
        }

        // Check for maximum file size limit
        long filesSizeInBytes = toDownloadFilesMap.values().stream()
                .mapToLong(list -> list.stream().mapToLong(d -> d.getSize() != null ? d.getSize() : 0).sum()).sum();
        LOGGER.debug(String.format("Total size of files to download : %d", filesSizeInBytes));
        // If size exceed maximum configured, return a JSON message with the error.
        if (filesSizeInBytes > (maxFilesSizeToDownload * 1024 * 1024)) {
            return CatalogPluginResponseFactory.createSuccessResponse(response, CatalogPluginResponseType.JSON, String
                    .format("Total size of selected files exceeded maximum allowed of %d (Mo)", maxFilesToDownload));
        }

        // If tere is no file downloadable, return a JSON message.
        if (nbFiles == 0) {
            return CatalogPluginResponseFactory
                    .createSuccessResponse(response, CatalogPluginResponseType.JSON,
                                           "None of the selected files are available for download");
        }

        // Create and stream the ZIP archive containg all downloadable files
        return CatalogPluginResponseFactory.createStreamSuccessResponse(response, getFilesAsZip(toDownloadFilesMap),
                                                                        getArchiveName(),
                                                                        MediaType.APPLICATION_OCTET_STREAM);
    }

    /**
     * Get the archive name by reading plugin parameters configuration.
     * @return String archive name
     */
    private String getArchiveName() {
        String fileName = archiveFileName;
        if (!fileName.endsWith(".zip")) {
            fileName = String.format("%s.zip", fileName);
        }
        return fileName;
    }

    /**
     * Get all the online {@link DataFile} of the given {@link DataObject}
     * @param dataObject {@link DataObject}
     * @return online {@link DataFile}s
     */
    private Set<DataFile> getOnlineFiles(DataObject dataObject) {
        Set<DataFile> files = Sets.newHashSet();
        if ((dataObject != null) && (dataObject.getFiles() != null)) {
            dataObject.getFiles().forEach((type, file) -> {
                if (DataType.RAWDATA.equals(type) && Boolean.TRUE.equals(file.getOnline()) && (file.getUri() != null)) {
                    files.add(file);
                }
            });
        }
        return files;
    }

    /**
     * Create a StreamingResponseBody by writting a ZIP containing all given {@link DataFile}
     * @param files {@link DataFile}s to write into the ZIP.
     * @return {@link StreamingResponseBody}
     */
    private StreamingResponseBody getFilesAsZip(Map<DataObject, Set<DataFile>> files) {
        return (StreamingResponseBody) outputStream -> createZipArchive(outputStream, files);
    }

    /**
     * Write a ZIP Archive with the given {@link DataFile}s into the given {@link OutputStream}
     * @param outputStream {@link OutputStream}
     * @param dataFiles {@link DataFile}s
     */
    private void createZipArchive(OutputStream outputStream, Map<DataObject, Set<DataFile>> dataFiles) {
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            for (Entry<DataObject, Set<DataFile>> entry : dataFiles.entrySet()) {
                DataObject dataobject = entry.getKey();
                Set<DataFile> dataobjectFiles = entry.getValue();
                for (DataFile file : dataobjectFiles) {
                    writeDataFileIntoZip(file, dataobject, zos);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error creating zip archive file for download", e);
        }
    }

    /**
     * Add a new file in the streaming ZIP Archive by Writing a {@link DataFile} into the given {@link ZipOutputStream}
     * @param file {@link DataFile}
     * @param dataobject {@link DataObject} of the given {@link DataFile}
     * @param zos {@link ZipOutputStream} to write into
     */
    private void writeDataFileIntoZip(DataFile file, DataObject dataobject, ZipOutputStream zos) {
        String fileName = getDataObjectFileNameForDownload(dataobject, file);
        try {
            LOGGER.debug(String.format("Adding file %s into ZIP archive", fileName));
            zos.putNextEntry(new ZipEntry(fileName));
            ByteStreams.copy(DownloadUtils.getInputStream(getDataFileURL(file)), zos);
        } catch (IOException e) {
            LOGGER.error(String.format("Error downloading file %s", file.getUri().toString()), e);
        } finally {
            try {
                zos.closeEntry();
            } catch (IOException e) {
                LOGGER.error(String.format("Error closing new entry %s into ZipOutputStream", fileName), e);
            }
        }
    }

    /**
     * File name for download is : <dataobjectName/dataFileName>. The dataFile name is the name of {@link DataFile} or name of URI if name is null.
     * @param dataobject {@link DataObject}
     * @param datafile {@link DataFile}
     * @return String fileName
     */
    private String getDataObjectFileNameForDownload(DataObject dataobject, DataFile datafile) {
        String fileName = datafile.getName() != null ? datafile.getName()
                : FilenameUtils.getName(datafile.getUri().getPath());
        String dataObjectName = dataobject.getLabel() != null ? dataobject.getLabel().replaceAll(" ", "") : "files";
        return String.format("%s/%s", dataObjectName, fileName);
    }

    /**
     * Generate URL to download the given {@link DataFile}
     * @param file {@link DataFile} to download
     * @return {@link URL}
     * @throws MalformedURLException
     */
    private URL getDataFileURL(DataFile file) throws MalformedURLException {
        URI fileUri = file.getUri();
        try {
            fileUri = new URIBuilder(fileUri).addParameter("token", jwtService.getCurrentToken().getJwt()).build();
            LOGGER.debug(String.format("File url is : %s", fileUri.toString()));
        } catch (JwtException | URISyntaxException e) {
            LOGGER.error("Error generating URI with current security token", e);
        }
        return fileUri.toURL();
    }

}
