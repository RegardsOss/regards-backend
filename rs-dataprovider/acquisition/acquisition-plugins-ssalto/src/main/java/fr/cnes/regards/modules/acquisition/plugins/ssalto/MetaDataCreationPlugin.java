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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.domain.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.finder.MultipleFileNameFinder;
import sipad.domain.transformer.TransformerTypeEnum;
import ssalto.controlers.data.descriptor.DescriptorFileControler;
import ssalto.controlers.plugins.decl.ICreateFileMetadataPlugin;
import ssalto.domain.SsaltoDomainException;
import ssalto.domain.data.descriptor.DataStorageObjectDescriptionElement;
import ssalto.domain.data.descriptor.DescriptorFile;
import ssalto.domain.data.storage.LocalArchive;

public class MetaDataCreationPlugin implements IGenerateSIPPlugin {

    /**
     * Logger de la classe
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleFileNameFinder.class);

    /**
     * Instancie le filePattern et permet de determiner le repertoire de depot dans l'archive.
     * 
     * @see ssalto.domain.plugins.decl.ICreateFileMetadataPlugin#getArchiveDirectory(java.lang.String, java.lang.String)
     * @since 1.2
     */
    @Override
    public String getArchiveDirectory(String filePath, String pattern) {

        Date day = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
        String year = sdf.format(day);
        sdf = new SimpleDateFormat("MM");
        String month = sdf.format(day);
        sdf = new SimpleDateFormat("dd");
        String cycle = sdf.format(day);

        String result = pattern;
        result = replacePattern("\\[YYYY\\]", year, result);
        result = replacePattern("\\[MM\\]", month, result);
        result = replacePattern("\\[CCC\\]", cycle, result);
        return result;
    }

    /**
     * Permet de traiter les patterns
     * 
     * @param pPattern
     * @param pReplacement
     * @param pResult
     * @return
     * @since 1.2
     */
    private String replacePattern(String pPattern, String pReplacement, String pResult) {

        Pattern pattern = Pattern.compile(pPattern);
        Matcher matcher = pattern.matcher(pResult);
        return matcher.replaceAll(pReplacement);
    }

    /**
     * Methode definissant un element xml de type DataStorageElement
     * 
     * @since 1.3
     * @DM SIPNG-DM-0047-CN : Creation : Ajout de pPRojectName et pDicoName
     */
    protected DataStorageObjectDescriptionElement defineDataStorageElement(File pSsaltoFile, String pProjectName,
            String pDicoName, String pDataSetId) throws SsaltoDomainException {

        // Define storage object element
        DataStorageObjectDescriptionElement dataStorageObject = new DataStorageObjectDescriptionElement();
        // DATA_STORAGE_OBJECT_IDENTIFIER
        dataStorageObject.setDataStorageObjectIdentifier(pSsaltoFile.getName());
        // FILE_SIZE
        if (pSsaltoFile.length() < 1024) {
            dataStorageObject.setFileSize(new Long(1));
        }
        else {
            dataStorageObject.setFileSize(new Long(pSsaltoFile.length() / 1024));
        }
        // STORAGE > STORAGE_ON_LINE > ONLINE_PATH
        setOnlinePath(dataStorageObject, pSsaltoFile);
        // STORAGE > STORAGE_ON_LINE > ONLINE_OBJECT_NAME
        dataStorageObject.setOnlineFileName(pSsaltoFile.getName());
        // TRANSFORMATION_SO_DO
        dataStorageObject.setTransformer((TransformerTypeEnum) null);
        return dataStorageObject;
    }

    /**
     * Generation du descripteur de fichier sous forme de chaine de caractere. Methode surchargee
     * 
     * @see ssalto.domain.plugins.decl.ICreateFileMetadataPlugin#generateXml(File, String, String)
     * @since 1.2
     * @DM SIPNG-DM-0047-CN : Modification : Ajout de pPRojectName et pDicoName
     * @DM SIPNG-DM-0060-CN : Modification : Sort la portion de code defineDataStorageElement et Rajout du parametre
     *     pDataSetId
     */
    @Override
    public String generateXml(File pSsaltoFile, String pProjectName, String pDicoName, String pDataSetId)
            throws SsaltoDomainException {
        String xmlString = null;

        // Init descriptor
        DescriptorFile descriptorFile = new DescriptorFile();
        descriptorFile.setDicoName(pDicoName);
        descriptorFile.setFileName(pSsaltoFile.getName());
        descriptorFile.setProjectName(pProjectName);

        DataStorageObjectDescriptionElement dataStorageObject = defineDataStorageElement(pSsaltoFile, pProjectName,
                                                                                         pDicoName, pDataSetId);

        // Add element to descriptor
        descriptorFile.addDescElementToDocument(dataStorageObject);

        // Write descriptor into a string
        try {
            xmlString = writeXmlToString(descriptorFile);
        }
        catch (IOException e) {
            LOGGER.error("Cannot create xml descriptor string for file " + pSsaltoFile.getAbsolutePath());
            throw new SsaltoDomainException(e.getMessage());
        }

        return xmlString;
    }

    /**
     * Calcul le chemin relatif a partir du chemin absolu et de l'archive locale.
     * 
     * @param pDataStorageObject
     * @param pSsaltoFile
     * @since 1.2
     * @DM : SIPNG-DM-0060-CN : 2009/06/27 : Pb sur le replace avec les \\
     */
    protected void setOnlinePath(DataStorageObjectDescriptionElement pDataStorageObject, File pSsaltoFile) {
        // path absolu de la locale archive
        String localArchivePath = LocalArchive.getInstance().getDataFolder();
        if (!localArchivePath.endsWith(File.separator)) {
            localArchivePath = localArchivePath.concat(File.separator);
        }
        // regExp pour traiter le cas d'un rep contenant \local_archive par ex ( le \l est un caractere reserve )
        //
        String regExp = localArchivePath.replaceAll("\\\\", "\\\\\\\\");
        String relativeOnlinePath = pSsaltoFile.getParent().replaceAll(regExp, "");
        pDataStorageObject.setOnlinePath(relativeOnlinePath);
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
            LOGGER.info("***** Computing FILE xml descriptor");
            StringWriter out = new StringWriter();
            // write the update document to the disk
            OutputFormat format = new OutputFormat(descDocumentToWrite, "UTF-8", true);
            format.setLineWidth(0);
            XMLSerializer output = new XMLSerializer(out, format);
            output.serialize(descDocumentToWrite);
            out.flush();
            out.close();
            xmlString = out.getBuffer().toString();
        }
        else {
            LOGGER.info("***** DO NOT compute FILE xml descriptor");
        }
        return xmlString;
    }

}
