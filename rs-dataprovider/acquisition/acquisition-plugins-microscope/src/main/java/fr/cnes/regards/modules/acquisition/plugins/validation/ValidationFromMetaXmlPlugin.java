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
package fr.cnes.regards.modules.acquisition.plugins.validation;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.modules.acquisition.exception.MetadataException;
import fr.cnes.regards.modules.acquisition.plugins.IValidationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.Microscope;

/**
 * Microscope product validation from XML metadata file.<br/>
 * File to validate is found under "nomFichierDonnee" tag, MD5 value is under "md5Check" tag.
 * @author oroussel
 */
@Plugin(id = "ValidationFromMetaXmlPlugin", version = "1.0.0-SNAPSHOT",
        description = "Read given metadata XML file and validate determined file of which name is under "
                + "'nomFichierDonnee' tag with MD5 value under md5Check tag", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class ValidationFromMetaXmlPlugin implements IValidationPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationFromMetaXmlPlugin.class);

    private static final String FILENAME_TAG = "nomFichierDonnee";

    private static final String CHECKSUM_TAG = "md5Check";

    private static final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

    private static DocumentBuilder builder;

    // Initialize Xml builder
    static {
        try {
            builder = builderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            LOGGER.error("Unable to create an XML document builder", e);
            throw new PluginUtilsRuntimeException(e);
        }
    }

    @Override
    public boolean validate(Path metadataFilePath) throws ModuleException {
        try {
            Document doc = builder.parse(metadataFilePath.toFile());
            doc.normalize();
            // Find data filename
            NodeList filenameElements = doc.getElementsByTagName(FILENAME_TAG);
            // it should exist only one tag
            if (filenameElements.getLength() == 1) {
                String dataFilename = filenameElements.item(0).getTextContent();
                // Determine data file from tag content, metada file path and/or validation plugin immplementation
                File dataFile = this.findDataFile(metadataFilePath, dataFilename);
                if (dataFile == null) {
                    throw new IOException("Bad product directory tree");
                }
                if (!dataFile.exists()) {
                    throw new FileNotFoundException(
                            String.format("Data file '%s' does not exist", dataFile.getAbsolutePath()));
                } else if (!dataFile.canRead()) {
                    throw new IOException(
                            String.format("Missing read access to '%s' data file", dataFile.getAbsolutePath()));
                }
                // Compute checksum
                String computeChecksum = ChecksumUtils
                        .computeHexChecksum(new FileInputStream(dataFile), Microscope.CHECKSUM_ALGO);
                // Find checksum from tag content, metada file path and/or validation plugin immplementation
                String givenChecksum = this.findChecksumValue(metadataFilePath, doc);
                // Compare both
                return computeChecksum.equals(givenChecksum);
            }
            // More than one tag => validation NOK
            return false;
        } catch (SAXException e) {
            throw new MetadataException(
                    String.format("Metadata file '%s' is not a valid XML file", metadataFilePath.toString()), e);
        } catch (IOException e) {
            throw new MetadataException(
                    String.format("Error while attempting to read metadata file '%s'", metadataFilePath.toString()), e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Data file is under sub-directory with same name as metadata xml file (after removing end "_metadata.xml").
     */
    protected File findDataFile(Path metadataPath, String dataFilename) {
        String metadataFilename = metadataPath.getFileName().toString();
        Path dirPath = Paths.get(metadataPath.getParent().toString(),
                                 metadataFilename.substring(0, metadataFilename.indexOf(Microscope.METADATA_SUFFIX)));
        return Paths.get(dirPath.toString(), dataFilename).toFile();
    }

    /**
     * MD5 value is under XML metadata file md5Check tag
     */
    protected String findChecksumValue(Path metadataPath, Document doc) throws MetadataException {
        NodeList md5CheckElements = doc.getElementsByTagName(CHECKSUM_TAG);
        if (md5CheckElements.getLength() == 1) {
            return md5CheckElements.item(0).getTextContent();
        }
        throw new MetadataException(
                String.format("XML Metadata file '%s' does not contain a unique %s tag", metadataPath.toString(),
                              CHECKSUM_TAG));
    }
}
