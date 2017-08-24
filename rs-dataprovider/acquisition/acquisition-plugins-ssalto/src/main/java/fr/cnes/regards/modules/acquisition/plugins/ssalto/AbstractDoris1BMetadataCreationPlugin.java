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
import sipad.domain.transformer.TransformerTypeEnum;
import ssalto.domain.SsaltoDomainException;
import ssalto.domain.data.descriptor.DataStorageObjectDescriptionElement;

public abstract class AbstractDoris1BMetadataCreationPlugin extends MetaDataCreationPlugin implements IDoris1BPlugin {

    /**
     * Liste des correspondances DatasetName => Prexix
     */
    protected Map<String, String> prefixMap_ = null;

    /**
     * Methode definissant un element xml de type DataStorageElement Methode surchargee Traite les cas particuliers
     * JASON1_DORIS1B_MOE_CDDIS, JASON1_DORIS1B_MOE_CDDIS_COM, JASON1_DORIS1B_POE_CDDIS_COM data_storage_object_id doit
     * etre prefixe respectivement par MOE_CDDIS_, MOE_CDDIS_COM_ , POE_CDDIS_COM_
     * 
     * @since 1.3
     * @DM SIPNG-DM-0047-CN : Creation : Ajout de pPRojectName et pDicoName
     */
    @Override
    protected DataStorageObjectDescriptionElement defineDataStorageElement(File pSsaltoFile, String pProjectName,
            String pDicoName, String pDataSetId) throws ModuleException {

        initPrefixMap();

        // Define storage object element
        DataStorageObjectDescriptionElement dataStorageObject = new DataStorageObjectDescriptionElement();

        String dataObjectIdentifier = pSsaltoFile.getName();

        // DATA_STORAGE_OBJECT_IDENTIFIER
        if ((prefixMap_ != null) && prefixMap_.containsKey(pDataSetId)) {
            String prefix = prefixMap_.get(pDataSetId);
            dataObjectIdentifier = prefix + dataObjectIdentifier;
        }
        else {
            throw new ModuleException("Prefix for " + pDataSetId + "does not exist!");
        }

        dataStorageObject.setDataStorageObjectIdentifier(dataObjectIdentifier);
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
//        // TRANSFORMATION_SO_DO
//        dataStorageObject.setTransformer((TransformerTypeEnum) null);
        return dataStorageObject;
    }

    /**
     * 
     * @param pDatasetName
     * @param pPrefix
     * @since 1.3
     */
    protected void addDatasetNamePrexif(String pDatasetName, String pPrefix) {
        if (prefixMap_ == null) {
            prefixMap_ = new HashMap<>();
        }
        prefixMap_.put(pDatasetName, pPrefix);
    }

}
