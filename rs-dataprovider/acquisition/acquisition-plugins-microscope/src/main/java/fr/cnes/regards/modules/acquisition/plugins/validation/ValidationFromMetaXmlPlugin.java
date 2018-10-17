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
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.modules.acquisition.exception.MetadataException;
import fr.cnes.regards.modules.acquisition.plugins.IValidationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.MicroHelper;
import fr.cnes.regards.modules.acquisition.plugins.Microscope;

/**
 * Microscope product validation from XML metadata file.<br/>
 * File to validate is found under "nomFichierDonnee" tag, MD5 value is under "md5Check" tag.
 * @author Olivier Rousselot
 */
@Plugin(id = "ValidationFromMetaXmlPlugin", version = "1.0.0-SNAPSHOT",
        description = "Read given metadata XML file and validate determined file of which name is under "
                + "'nomFichierDonnee' tag with MD5 value under md5Check tag", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class ValidationFromMetaXmlPlugin implements IValidationPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationFromMetaXmlPlugin.class);

    private static final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

    @Override
    public boolean validate(Path metadataFilePath) throws ModuleException {
        try {
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document doc = builder.parse(metadataFilePath.toFile());
            doc.normalize();
            String dataFilename = MicroHelper.getTagValue(doc, Microscope.FILENAME_TAG);
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
            String providedChecksum = MicroHelper.getTagValue(doc, Microscope.CHECKSUM_TAG);
            // Compare both
            return computeChecksum.equals(providedChecksum);
        } catch (SAXException e) {
            throw new MetadataException(
                    String.format("Metadata file '%s' is not a valid XML file", metadataFilePath.toString()), e);
        } catch (IOException e) {
            throw new MetadataException(
                    String.format("Error while attempting to read metadata file '%s'", metadataFilePath.toString()), e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            LOGGER.error("Unable to create an XML document builder", e);
            throw new PluginUtilsRuntimeException(e);
        }
    }

    protected File findDataFile(Path metadataPath, String dataFilename) {
        return MicroHelper.findDataFileIntoSubDir(metadataPath, dataFilename);
    }
}
