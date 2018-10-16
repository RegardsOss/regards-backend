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
package fr.cnes.regards.modules.acquisition.plugins.product;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.modules.acquisition.exception.MetadataException;
import fr.cnes.regards.modules.acquisition.plugins.IProductPlugin;
import fr.cnes.regards.modules.acquisition.plugins.MicroHelper;
import fr.cnes.regards.modules.acquisition.plugins.Microscope;
import fr.cnes.regards.modules.acquisition.plugins.validation.ValidationFromMetaXmlPlugin;

/**
 * Microscope Metadata XML product name reader plugin.<br/>
 * This plugin retrieves product name from metadata "_metadata.xml" files, it is the value under nomFichierDonnee tag.
 * @author Olivier Rousselot
 */
@Plugin(id = "ProductFromMetaXmlPlugin", version = "1.0.0-SNAPSHOT",
        description = "Read metadata XML files to retrieve product name", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class ProductFromMetaXmlPlugin implements IProductPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationFromMetaXmlPlugin.class);

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
    public String getProductName(Path metadataFilePath) throws ModuleException {
        try {
            Document doc = builder.parse(metadataFilePath.toFile());
            doc.normalize();
            // Find data filename
            return this.findProductNameFromTagContent(MicroHelper.getTagValue(doc, Microscope.FILENAME_TAG));
        } catch (SAXException e) {
            throw new MetadataException(
                    String.format("Metadata file '%s' is not a valid XML file", metadataFilePath.toString()), e);
        } catch (IOException e) {
            throw new MetadataException(
                    String.format("Error while attempting to read metadata file '%s'", metadataFilePath.toString()), e);
        }
    }

    protected String findProductNameFromTagContent(String text) {
        return text;
    }
}
