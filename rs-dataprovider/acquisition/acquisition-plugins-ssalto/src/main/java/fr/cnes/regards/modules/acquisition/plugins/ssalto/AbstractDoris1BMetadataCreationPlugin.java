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
import java.util.HashMap;
import java.util.Map;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DataStorageObjectDescriptionElement;

public abstract class AbstractDoris1BMetadataCreationPlugin extends MetaDataCreationPlugin implements IDoris1BPlugin {

    /**
     * Liste des correspondances DatasetName => Prexix
     */
    protected Map<String, String> prefixMap = null;

    /**
     * Methode definissant un element xml de type DataStorageElement.
     * Traite les cas particuliers
     * JASON1_DORIS1B_MOE_CDDIS, JASON1_DORIS1B_MOE_CDDIS_COM, JASON1_DORIS1B_POE_CDDIS_COM data_storage_object_id doit
     * etre prefixe respectivement par MOE_CDDIS_, MOE_CDDIS_COM_ , POE_CDDIS_COM_
     */
    @Override
    protected DataStorageObjectDescriptionElement defineDataStorageElement(File acquisitionFile, String projectName,
            String dicoName, String dataSetId) throws ModuleException {

        initPrefixMap();

        // Define storage object element
        DataStorageObjectDescriptionElement dataStorageObject = new DataStorageObjectDescriptionElement();

        String dataObjectIdentifier = acquisitionFile.getName();

        // DATA_STORAGE_OBJECT_IDENTIFIER
        if ((prefixMap != null) && prefixMap.containsKey(dataSetId)) {
            String prefix = prefixMap.get(dataSetId);
            dataObjectIdentifier = prefix + dataObjectIdentifier;
        } else {
            throw new ModuleException("Prefix for " + dataSetId + "does not exist!");
        }

        dataStorageObject.setDataStorageObjectIdentifier(dataObjectIdentifier);
        // FILE_SIZE
        if (acquisitionFile.length() < 1024) {
            dataStorageObject.setFileSize(1L);
        } else {
            dataStorageObject.setFileSize(Long.valueOf(acquisitionFile.length() / 1024));
        }
        // STORAGE > STORAGE_ON_LINE > ONLINE_PATH
        // TODO CMZ à confirmer : suppression de setOnlinePath et setTransformer
        //        setOnlinePath(dataStorageObject, pSsaltoFile);
        // STORAGE > STORAGE_ON_LINE > ONLINE_OBJECT_NAME
        dataStorageObject.setOnlineFileName(acquisitionFile.getName());
        //        // TRANSFORMATION_SO_DO
        //        dataStorageObject.setTransformer((TransformerTypeEnum) null);
        return dataStorageObject;
    }

    /**
     * 
     * @param datasetName
     * @param prefix
     */
    protected void addDatasetNamePrexif(String datasetName, String prefix) {
        if (prefixMap == null) {
            prefixMap = new HashMap<>();
        }
        prefixMap.put(datasetName, prefix);
    }

}
