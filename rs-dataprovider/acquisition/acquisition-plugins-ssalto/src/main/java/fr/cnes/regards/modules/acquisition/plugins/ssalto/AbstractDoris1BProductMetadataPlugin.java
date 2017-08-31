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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DataObjectDescriptionElement;

public abstract class AbstractDoris1BProductMetadataPlugin extends AbstractProductMetadataPlugin
        implements IDoris1BPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDoris1BProductMetadataPlugin.class);

    /**
     * Liste des correspondances DatasetName => Prexix
     */
    protected Map<String, String> prefixMap = null;

    /**
     * cree le squelette du fichier descripteur contenant les attributs minimums ascendingNode, fileSize, et la liste
     * des objets Methode adaptee aux plugins JASON1_DORIS1B_MOE_CDDIS, JASON1_DORIS1B_MOE_CDDIS_COM,
     * JASON1_DORIS1B_POE_CDDIS_COM Les fichiers des jeux DA_TC_JASON1_DORIS1B_MOE_CDDIS,
     * DA_TC_JASON1_DORIS1B_MOE_CDDIS_COM, DA_TC_JASON1_DORIS1B_POE_CDDIS_COM ont des nomenclatures identiques. Donc le
     * DATA_STORAGE_OBJECT_IDENTIFIER doit etre differentie et donc prefixe respectivement par : MOE_CDDIS_,
     * MOE_CDDIS_COM_, POE_CDDIS_COM_ NOTE : DATA_OBJECT_IDENTIFIER doit etre egalement differentie mais PAS DANS CETTE
     * CLASSE. ( mais plutot dans la classe ...checkingPlugin )
     * 
     * @return un DataObjectDescriptionElement minimum.
     * @param pProductName
     *            , le nom du produit dont on cree les meta donnees
     * @param pFileMap
     *            la liste des fichiers composant le produit
     * @param pDataSetName
     *            le nom du dataSet auquel rattacher l'objet de donnees.
     */
    @Override
    public DataObjectDescriptionElement createSkeleton(String pProductName, Map<File, ?> pFileMap,
            String pDataSetName) {

        initPrefixMap();

        DataObjectDescriptionElement element = new DataObjectDescriptionElement();
        element.setAscendingNode(pDataSetName);

        element.setDataObjectIdentifier(pProductName);

        long size = 0;
        for (File file : pFileMap.keySet()) {
            size = size + file.length();

            if ((prefixMap != null) && prefixMap.containsKey(pDataSetName)) {
                String prefix = prefixMap.get(pDataSetName);
                element.addDataStorageObjectIdentifier(prefix + file.getName());
            } else {
                LOGGER.error("Prefix for " + pDataSetName + "does not exist!");
            }
        }
        element.setFileSize(Long.toString(size / 1024));
        return element;
    }

    protected void addDatasetNamePrexif(String pDatasetName, String pPrefix) {
        if (prefixMap == null) {
            prefixMap = new HashMap<>();
        }
        prefixMap.put(pDatasetName, pPrefix);
    }

}
