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
package fr.cnes.regards.modules.acquisition.plugins.sip;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.exception.MetadataException;
import fr.cnes.regards.modules.acquisition.plugins.ISipGenerationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.MicroHelper;
import fr.cnes.regards.modules.acquisition.plugins.Microscope;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;

/**
 * SIP generation plugin adding only start and end dates from XML metadatafile
 * @author Olivier Rousselot
 */
@Plugin(id = "FromMetadataSipGenerationPlugin", version = "1.0.0-SNAPSHOT",
        description = "Generate product SIP adding only start and end dates from metadata XML file",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class FromMetadataSipGenerationPlugin implements ISipGenerationPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(FromMetadataSipGenerationPlugin.class);

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
    public SIP generate(Product product) throws ModuleException {
        // Init the builder
        SIPBuilder sipBuilder = new SIPBuilder(product.getProductName());

        // Retrieve metadata file
        AcquisitionFile metadataAcqFile = product.getActiveAcquisitionFiles().get(0);
        Path metadataFilePath = metadataAcqFile.getFilePath();

        try {
            Document doc = builder.parse(metadataFilePath.toFile());
            doc.normalize();
            // Find data file
            File dataFile = findDataFile(metadataFilePath, MicroHelper.getTagValue(doc, Microscope.FILENAME_TAG));

            // Find data file checksum
            String checksum = MicroHelper.getTagValue(doc, Microscope.CHECKSUM_TAG);

            // Add data file to SIP
            sipBuilder.getContentInformationBuilder()
                    .setDataObject(DataType.RAWDATA, dataFile.toPath(), dataFile.getName(), Microscope.CHECKSUM_ALGO,
                                   checksum, dataFile.length());
            sipBuilder.getContentInformationBuilder().setSyntax(getDataFileMimeType());
            sipBuilder.addContentInformation();
            // Add creation event
            sipBuilder.addEvent("Product SIP generation");

            // Add descriptive informations
            addDescriptiveInformations(sipBuilder, doc);

            return sipBuilder.build();
        } catch (SAXException e) {
            throw new MetadataException(
                    String.format("Metadata file '%s' is not a valid XML file", metadataFilePath.toString()), e);
        } catch (IOException e) {
            throw new MetadataException(
                    String.format("Error while attempting to read metadata file '%s'", metadataFilePath.toString()), e);
        }
    }

    protected MimeType getDataFileMimeType() {
        return MediaType.TEXT_PLAIN;
    }

    protected File findDataFile(Path metadataFilePath, String filename) throws IOException {
        return MicroHelper.findDataFileIntoSubDir(metadataFilePath, filename);
    }

    /**
     * Only add StartDate and EndDate from metadata file
     * @throws MetadataException
     */
    protected void addDescriptiveInformations(SIPBuilder sipBuilder, Document doc) throws MetadataException {
        sipBuilder.addDescriptiveInformation(Microscope.START_DATE,
                                             MicroHelper.getTagValue(doc, Microscope.START_DATE_TAG));
        sipBuilder
                .addDescriptiveInformation(Microscope.END_DATE, MicroHelper.getTagValue(doc, Microscope.END_DATE_TAG));
    }
}
