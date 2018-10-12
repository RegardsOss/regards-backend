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
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.exception.MetadataException;
import fr.cnes.regards.modules.acquisition.plugins.ISipGenerationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.Microscope;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;

/**
 * RINEX product SIP generation
 * @author Olivier Rousselot
 */
@Plugin(id = "RinexSipGenerationPlugin", version = "1.0.0-SNAPSHOT",
        description = "RINEX product SIP generation plugin", author = "REGARDS Team", contact = "regards@c-s.fr",
        licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class RinexSipGenerationPlugin implements ISipGenerationPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(RinexSipGenerationPlugin.class);

    private static final String SCENARIO_FILENAME = "mic_cmsm_scenarioTravail.xml";

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

        // Acquisition file is RINEX_<MISSION>.tar.gz, there should be only one file (in fact, there MUST be if we are
        // here so no need to test it)
        AcquisitionFile af = product.getActiveAcquisitionFiles().get(0);
        // Content dir
        File dir = af.getFilePath().toFile().getParentFile();
        // Deduce session from data file name
        String session = af.getFilePath().getFileName().toString().replaceFirst("RINEX_(\\d+)\\.tar\\.gz", "$1");

        sipBuilder.getContentInformationBuilder()
                .setDataObject(DataType.RAWDATA, af.getFilePath().toAbsolutePath(), af.getChecksumAlgorithm(),
                               af.getChecksum());
        sipBuilder.getContentInformationBuilder().setSyntax(Microscope.GZIP_MIME_TYPE);
        sipBuilder.addContentInformation();

        // Add creation event
        sipBuilder.addEvent("Product SIP generation");

        // Add Session descriptive info
        sipBuilder.addDescriptiveInformation(Microscope.SESSION, session);

        // Read scenario XML file for descriptive informations
        File scenarioFile = new File(dir, SCENARIO_FILENAME);
        try {
            if (!Files.isRegularFile(scenarioFile.toPath())) {
                throw new IOException(String.format("'%s' is not a regular file", scenarioFile.getAbsolutePath()));
            }
            Document doc = builder.parse(scenarioFile);
            doc.normalize();
            // For all "Sequence" tags
            String sequenceNumber = Integer.valueOf(session).toString();
            NodeList sequences = doc.getElementsByTagName("Sequence");
            boolean found = false;
            for (int i = 0; i < sequences.getLength(); i++) {
                Node sequence = sequences.item(i);
                // Search for sequence with asked sequenceNumber
                String numeroSequence = getChildValue(sequence, "numeroSequence");
                if ((!found) && (numeroSequence.equals(sequenceNumber))) {
                    found = true;
                    sipBuilder.addDescriptiveInformation(Microscope.START_DATE, getChildValue(sequence, "dateDebut"));
                } else if (found) { // Element after searched sequence
                    sipBuilder.addDescriptiveInformation(Microscope.END_DATE, getChildValue(sequence, "dateDebut"));
                    break;
                }
            }
        } catch (SAXException e) {
            throw new MetadataException(
                    String.format("Scenario file '%s' is not a valid XML file", scenarioFile.toString()), e);
        } catch (IOException e) {
            throw new MetadataException(
                    String.format("Error while attempting to read scenario file '%s'", scenarioFile.toString()), e);
        }
        return sipBuilder.build();
    }

    private static String getChildValue(Node parentNode, String tagName) {
        NodeList children = parentNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ((child instanceof Element) && ((Element) child).getTagName().equals(tagName)) {
                return child.getTextContent();
            }
        }
        return null;
    }
}
