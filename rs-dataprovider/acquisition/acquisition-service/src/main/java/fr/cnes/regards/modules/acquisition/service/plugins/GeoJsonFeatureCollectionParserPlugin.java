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
package fr.cnes.regards.modules.acquisition.service.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.framework.geojson.FeatureCollection;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.modules.acquisition.plugins.IScanPlugin;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;

/**
 * This plugin allows to scan a directory to find geojson files and generate a new file to acquire for each feature found in it.
 * @author Sébastien Binda
 */
@Plugin(id = "GeoJsonFeatureCollectionParserPlugin", version = "1.0.0-SNAPSHOT",
        description = "Scan directory to detect geosjson files. Generate a file to acquire for each feature found in it.",
        author = "REGARDS Team", contact = "regards@c-s.fr", license = "GPLv3", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class GeoJsonFeatureCollectionParserPlugin implements IScanPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobDiskScanning.class);

    public static final String FIELD_DIR = "directoryToScan";

    public static final String FIELD_FEATURE_ID = "featureId";

    @Autowired
    private Gson gson;

    @PluginParameter(name = FIELD_DIR,
            label = "Directories to scan to find *.json files containing geojson feature collections")
    private String directoryToScan;

    @PluginParameter(name = FIELD_FEATURE_ID,
            label = "Json path to access the identifier of each feature in the geojson file", optional = false)
    private String featureId;

    @Override
    public List<Path> scan(Optional<OffsetDateTime> lastModificationDate) throws ModuleException {
        List<Path> scannedFiles = new ArrayList<>();
        Path dirPath = Paths.get(directoryToScan);
        if (Files.isDirectory(dirPath)) {
            scannedFiles.addAll(scanDirectory(dirPath, lastModificationDate));
        } else {
            throw new PluginUtilsRuntimeException(String.format("Invalid directory path : {}", dirPath.toString()));
        }
        return scannedFiles;

    }

    private List<Path> scanDirectory(Path dirPath, Optional<OffsetDateTime> lastModificationDate) {
        List<Path> genetateFeatureFiles = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*.geojson")) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    if (lastModificationDate.isPresent()) {
                        OffsetDateTime lmd = OffsetDateTime.ofInstant(Files.getLastModifiedTime(entry).toInstant(),
                                                                      ZoneOffset.UTC);
                        if (lmd.isAfter(lastModificationDate.get()) || lmd.isEqual(lastModificationDate.get())) {
                            genetateFeatureFiles.addAll(generateFeatureFiles(entry));
                        }
                    } else {
                        genetateFeatureFiles.addAll(generateFeatureFiles(entry));
                    }
                }
            }
        } catch (IOException x) {
            throw new PluginUtilsRuntimeException("Scanning failure", x);
        }

        return genetateFeatureFiles;
    }

    private List<Path> generateFeatureFiles(Path entry) {

        List<Path> generatedFiles = Lists.newArrayList();

        try {

            // Check if file is a gson feature collection
            File gsonFile = entry.toFile();
            JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(gsonFile)));
            FeatureCollection fc = gson.fromJson(reader, FeatureCollection.class);

            for (Feature feature : fc.getFeatures()) {
                String name = (String) feature.getProperties().get(featureId);
                SIPBuilder builder = new SIPBuilder(name);
                for (String property : feature.getProperties().keySet()) {
                    builder.addDescriptiveInformation(property, feature.getProperties().get(property));
                }

                // Check for RAWDATA if any
                Path rawDataFile = Paths.get(entry.getParent().toString(), name + ".dat");
                Path thumbnailFile = Paths.get(entry.getParent().toString(), name + ".png");
                Path descFile = Paths.get(entry.getParent().toString(), name + ".pdf");

                if (Files.exists(rawDataFile)) {
                    String checksum = ChecksumUtils.computeHexChecksum(new FileInputStream(rawDataFile.toFile()),
                                                                       "MD5");
                    builder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, rawDataFile.toAbsolutePath(),
                                                                         rawDataFile.getFileName().toString(), "MD5",
                                                                         checksum, rawDataFile.toFile().length());
                    builder.getContentInformationBuilder().setSyntax(MediaType.APPLICATION_OCTET_STREAM);
                    builder.addContentInformation();
                }
                if (Files.exists(thumbnailFile)) {
                    String checksum = ChecksumUtils.computeHexChecksum(new FileInputStream(thumbnailFile.toFile()),
                                                                       "MD5");
                    builder.getContentInformationBuilder().setDataObject(DataType.THUMBNAIL,
                                                                         thumbnailFile.toAbsolutePath(),
                                                                         thumbnailFile.getFileName().toString(), "MD5",
                                                                         checksum, thumbnailFile.toFile().length());
                    builder.getContentInformationBuilder().setSyntax(MediaType.IMAGE_PNG);
                    builder.addContentInformation();

                    builder.getContentInformationBuilder().setDataObject(DataType.QUICKLOOK_SD,
                                                                         thumbnailFile.toAbsolutePath(),
                                                                         thumbnailFile.getFileName().toString(), "MD5",
                                                                         checksum, thumbnailFile.toFile().length());
                    builder.getContentInformationBuilder().setSyntax(MediaType.IMAGE_PNG);
                    builder.addContentInformation();
                }
                if (Files.exists(descFile)) {
                    String checksum = ChecksumUtils.computeHexChecksum(new FileInputStream(descFile.toFile()), "MD5");
                    builder.getContentInformationBuilder().setDataObject(DataType.DESCRIPTION,
                                                                         descFile.toAbsolutePath(),
                                                                         descFile.getFileName().toString(), "MD5",
                                                                         checksum, descFile.toFile().length());
                    builder.getContentInformationBuilder().setSyntax(MediaType.APPLICATION_PDF);
                    builder.addContentInformation();
                }

                SIP sip = builder.build();
                sip.setGeometry(feature.getGeometry());

                if (!sip.getProperties().getContentInformations().isEmpty()) {
                    Path file = Paths.get(entry.getParent().toString(), name + ".json");
                    generatedFiles.add(Files.write(file, Arrays.asList(gson.toJson(sip)), Charset.forName("UTF-8")));
                }
            }

        } catch (IOException | NoSuchAlgorithmException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return generatedFiles;
    }

    public void setDirectoryToScan(String dir) {
        this.directoryToScan = dir;
    }

    public void setGson(Gson gson) {
        this.gson = gson;
    }

    public void setFeatureId(String featureId) {
        this.featureId = featureId;
    }

}
