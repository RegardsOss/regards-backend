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
package fr.cnes.regards.modules.acquisition.plugins.ssalto;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ssalto.controlers.data.descriptor.DescriptorFileControler;
import ssalto.controlers.plugins.decl.ICreateProductMetadataPlugin;
import ssalto.domain.SsaltoDomainException;
import ssalto.domain.data.descriptor.DataObjectDescriptionElement;
import ssalto.domain.data.descriptor.DescriptorFile;

/**
 * @author CS
 * @since 1.2
 */

public class ProductMetadataPluginImpl implements ICreateProductMetadataPlugin {

    /**
     * Logger de la classe
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductMetadataPluginImpl.class);

    /**
     * Cree les metadata niveau produit Methode surchargee
     * 
     * @see ssalto.domain.plugins.decl.ICreateProductMetadataPlugin#createMetadataPlugin(String, Map, String, String,
     *      String)
     * @since 1.2
     * @DM SIPNG-DM-0047-CN : modification de la signature
     */
    @Override
    public String createMetadataPlugin(String pProductName, Map<File, ?> pFileMap, String pDatasetName,
            String pDicoName, String pProjectName) throws SsaltoDomainException {

        // return pProductName;
        String xmlString = null;

        // Init descriptor
        DescriptorFile descriptorFile = new DescriptorFile();
        descriptorFile.setDicoName(pDicoName);
        descriptorFile.setFileName(null);
        descriptorFile.setProjectName(pProjectName);

        // Define object element
        DataObjectDescriptionElement dataObject = new DataObjectDescriptionElement();
        dataObject.setAscendingNode(pDatasetName);
        dataObject.setDataObjectIdentifier(pProductName);
        // Add other tags
        dataObject.setCycleNumber("0");
        dataObject.setFileCreationDate("2007-10-10T00:00:00");
        dataObject.setLatitudeMax("60.0");
        dataObject.setLatitudeMin("-60.0");
        dataObject.setLongitudeMax("180.0");
        dataObject.setLongitudeMin("-180.0");
        dataObject.setObjectVersion("1");
        dataObject.setStartDate("1950-01-01T00:00:00");
        dataObject.setStopDate("2020-12-31T23:59:59");

        // Add data storage object identifier
        long fileSize = 0;
        for (File file : pFileMap.keySet()) {
            fileSize = fileSize + file.length();
            dataObject.addDataStorageObjectIdentifier(file.getName());
        }
        dataObject.setFileSize(Long.toString(fileSize));
        // Add element to descriptor
        descriptorFile.addDescElementToDocument(dataObject);

        // Write descriptor into a string
        try {
            xmlString = writeXmlToString(descriptorFile);
        } catch (IOException e) {
            LOGGER.error("Cannot create xml descriptor string for product " + pProductName);
            throw new SsaltoDomainException(e.getMessage());
        }

        return xmlString;
    }

    /**
     * Ecriture du descripteur
     * 
     * @param pTargetFile
     *            Fichier physique dans lequel ecrire
     * @param pDescFile
     *            Objet descripteur
     * @throws IOException
     * @since 1.2
     * @FA SIPNG-FA-0400-CN : ajout de code
     */
    private String writeXmlToString(DescriptorFile pDescFile) throws IOException {

        String xmlString = null;
        // Write the description document to a String
        DocumentImpl descDocumentToWrite = DescriptorFileControler.getDescDocument(pDescFile);
        if (descDocumentToWrite != null) {
            LOGGER.info("***** Computing PRODUCT xml descriptor");
            StringWriter out = new StringWriter();
            // write the update document to the disk
            OutputFormat format = new OutputFormat(descDocumentToWrite, "UTF-8", true);
            format.setLineWidth(0);
            XMLSerializer output = new XMLSerializer(out, format);
            output.serialize(descDocumentToWrite);
            out.flush();
            out.close();
            xmlString = out.getBuffer().toString();
        } else {
            LOGGER.info("***** DO NOT compute PRODUCT xml descriptor");
        }
        return xmlString;
    }

}
