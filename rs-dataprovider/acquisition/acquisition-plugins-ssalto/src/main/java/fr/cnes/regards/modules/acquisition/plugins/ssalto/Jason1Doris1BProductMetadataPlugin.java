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
import java.util.Map;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DataObjectDescriptionElement;

public class Jason1Doris1BProductMetadataPlugin extends Jason1ProductMetadataPlugin {

    private static final String PROJECT_NAME = "JASON";

    private static final String DATASETNAME_JASON1_DORIS1B_MOE_CDDIS = "DA_TC_JASON1_DORIS1B_MOE_CDDIS";

    private static final String DATASETNAME_JASON1_DORIS1B_MOE_CDDIS_COM = "DA_TC_JASON1_DORIS1B_MOE_CDDIS_COM";

    private static final String DATASETNAME_JASON1_DORIS1B_POE_CDDIS_COM = "DA_TC_JASON1_DORIS1B_POE_CDDIS_COM";

    private static final String PREFIX_MOE_CDDIS = "MOE_CDDIS_";

    private static final String PREFIX_MOE_CDDIS_COM = "MOE_CDDIS_COM_";

    private static final String PREFIX_POE_CDDIS_COM = "POE_CDDIS_COM_";

    @Override
    protected String getProjectName() {
        return PROJECT_NAME;
    }

    /**
     * cree le squelette du fichier descripteur contenant les attributs minimums ascendingNode, fileSize, et la liste
     * des objets Methode adaptee aux plugins JASON1_DORIS1B_MOE_CDDIS, JASON1_DORIS1B_MOE_CDDIS_COM,
     * JASON1_DORIS1B_POE_CDDIS_COM Les fichiers des jeux DA_TC_JASON1_DORIS1B_MOE_CDDIS,
     * DA_TC_JASON1_DORIS1B_MOE_CDDIS_COM, DA_TC_JASON1_DORIS1B_POE_CDDIS_COM ont des nomenclatures identiques. Donc le
     * DATA_OBJECT_IDENTIFIER et le DATA_STORAGE_OBJECT_IDENTIFIER doivent etre differenties et donc prefixes
     * respectivement par : MOE_CDDIS_, MOE_CDDIS_COM_, POE_CDDIS_COM_
     * 
     * @return un DataObjectDescriptionElement minimum.
     * @param pProductName
     *            , le nom du produit dont on cree les meta donnees
     * @param pFileMap
     *            la liste des fichiers composant le produit
     * @param pDataSetName
     *            le nom du dataSet auquel rattacher l'objet de donnees.
     * @since 1.2
     */
    @Override
    public DataObjectDescriptionElement createSkeleton(String pProductName, Map<File, ?> pFileMap,
            String pDataSetName) {

        DataObjectDescriptionElement element = new DataObjectDescriptionElement();
        element.setAscendingNode(pDataSetName);

        element.setDataObjectIdentifier(pProductName);

        long size = 0;
        for (File file : pFileMap.keySet()) {
            size = size + file.length();

            if (pDataSetName.equals(DATASETNAME_JASON1_DORIS1B_MOE_CDDIS)) {
                element.addDataStorageObjectIdentifier(PREFIX_MOE_CDDIS + file.getName());
            } else if (pDataSetName.equals(DATASETNAME_JASON1_DORIS1B_MOE_CDDIS_COM)) {
                element.addDataStorageObjectIdentifier(PREFIX_MOE_CDDIS_COM + file.getName());
            } else if (pDataSetName.equals(DATASETNAME_JASON1_DORIS1B_POE_CDDIS_COM)) {
                element.addDataStorageObjectIdentifier(PREFIX_POE_CDDIS_COM + file.getName());
            }

        }
        element.setFileSize(Long.toString(size / 1024));
        return element;
    }
}
