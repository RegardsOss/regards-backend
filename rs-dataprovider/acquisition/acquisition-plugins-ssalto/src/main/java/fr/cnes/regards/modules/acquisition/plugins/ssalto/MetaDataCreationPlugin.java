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
import java.util.List;
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.finder.MultipleFileNameFinder;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DataStorageObjectDescriptionElement;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;

public class MetaDataCreationPlugin implements IGenerateSIPPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleFileNameFinder.class);

    // TODO CMZ à confirmer
    //    /**
    //     * Instancie le filePattern et permet de determiner le repertoire de depot dans l'archive.
    //     * 
    //     * @see ssalto.domain.plugins.decl.ICreateFileMetadataPlugin#getArchiveDirectory(java.lang.String, java.lang.String)
    //     * @since 1.2
    //     */
    //    @Override
    //    public String getArchiveDirectory(String filePath, String pattern) {
    //
    //        Date day = Calendar.getInstance().getTime();
    //        SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
    //        String year = sdf.format(day);
    //        sdf = new SimpleDateFormat("MM");
    //        String month = sdf.format(day);
    //        sdf = new SimpleDateFormat("dd");
    //        String cycle = sdf.format(day);
    //
    //        String result = pattern;
    //        result = replacePattern("\\[YYYY\\]", year, result);
    //        result = replacePattern("\\[MM\\]", month, result);
    //        result = replacePattern("\\[CCC\\]", cycle, result);
    //        return result;
    //    }
    //
    //    /**
    //     * Permet de traiter les patterns
    //     * 
    //     * @param pPattern
    //     * @param pReplacement
    //     * @param pResult
    //     * @return
    //     * @since 1.2
    //     */
    //    private String replacePattern(String pPattern, String pReplacement, String pResult) {
    //
    //        Pattern pattern = Pattern.compile(pPattern);
    //        Matcher matcher = pattern.matcher(pResult);
    //        return matcher.replaceAll(pReplacement);
    //    }

    /**
     * Methode definissant un element xml de type DataStorageElement
     */
    protected DataStorageObjectDescriptionElement defineDataStorageElement(File acquisitionFile, String projectName,
            String dicoName, String dataSetId) throws ModuleException {

        // Define storage object element
        DataStorageObjectDescriptionElement dataStorageObject = new DataStorageObjectDescriptionElement();
        // DATA_STORAGE_OBJECT_IDENTIFIER
        dataStorageObject.setDataStorageObjectIdentifier(acquisitionFile.getName());
        // FILE_SIZE
        if (acquisitionFile.length() < 1024) {
            dataStorageObject.setFileSize(new Long(1));
        } else {
            dataStorageObject.setFileSize(new Long(acquisitionFile.length() / 1024));
        }

        // STORAGE > STORAGE_ON_LINE > ONLINE_PATH
        // TODO CMZ à confirmer
        //        setOnlinePath(dataStorageObject, acquisitionFile);
        // STORAGE > STORAGE_ON_LINE > ONLINE_OBJECT_NAME
        dataStorageObject.setOnlineFileName(acquisitionFile.getName());

        // TRANSFORMATION_SO_DO
        // TODO CMZ à confirmer
        //        dataStorageObject.setTransformer((TransformerTypeEnum) null);

        return dataStorageObject;
    }

    // TODO CMZ à confirmer
    //    /**
    //     * Calcul le chemin relatif a partir du chemin absolu et de l'archive locale.
    //     * 
    //     * @param pDataStorageObject
    //     * @param pSsaltoFile
    //     * @since 1.2
    //     * @DM : SIPNG-DM-0060-CN : 2009/06/27 : Pb sur le replace avec les \\
    //     */
    //    protected void setOnlinePath(DataStorageObjectDescriptionElement pDataStorageObject, File pSsaltoFile) {
    //        // path absolu de la locale archive
    //        String localArchivePath = LocalArchive.getInstance().getDataFolder();
    //        if (!localArchivePath.endsWith(File.separator)) {
    //            localArchivePath = localArchivePath.concat(File.separator);
    //        }
    //        // regExp pour traiter le cas d'un rep contenant \local_archive par ex ( le \l est un caractere reserve )
    //        //
    //        String regExp = localArchivePath.replaceAll("\\\\", "\\\\\\\\");
    //        String relativeOnlinePath = pSsaltoFile.getParent().replaceAll(regExp, "");
    //        pDataStorageObject.setOnlinePath(relativeOnlinePath);
    //    }

    @Override
    public SortedMap<Integer, Attribute> createMetadataPlugin(List<AcquisitionFile> acqFiles, String datasetName)
            throws ModuleException {
        // TODO CMZ createMetadataPlugin à compléter        
        return null;
    }

    @Override
    public SortedMap<Integer, Attribute> createMetaDataPlugin(List<AcquisitionFile> acqFiles) throws ModuleException {
        // TODO CMZ createMetaDataPlugin à compléter
        return null;
    }

    @Override
    public SIPCollection runPlugin(String sessionId, List<AcquisitionFile> acqFiles, String datasetName)
            throws ModuleException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SIPCollection runPlugin(String sessionId, List<AcquisitionFile> acqFiles) throws ModuleException {
        // TODO Auto-generated method stub
        return null;
    }

}
