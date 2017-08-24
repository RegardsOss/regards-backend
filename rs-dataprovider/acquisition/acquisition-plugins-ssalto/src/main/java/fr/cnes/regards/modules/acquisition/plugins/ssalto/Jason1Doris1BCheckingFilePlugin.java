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


/**
 * plugin de verification des fichiers. Ce plugin permet aussi d'indiquer le node_identifier qui doit etre utiliser pour
 * enregistrer le fichier dans le catalogue de diffusion. Ce plugin est utilise pour Jason1-Doris1B.
 * 
 * @author Christophe Mertz
 *
 */
public class Jason1Doris1BCheckingFilePlugin extends AbstractDoris1BCheckingPlugin {

    private static final String DATASETNAME_JASON1_DORIS1B_MOE_CDDIS = "DA_TC_JASON1_DORIS1B_MOE_CDDIS";

    private static final String DATASETNAME_JASON1_DORIS1B_MOE_CDDIS_COM = "DA_TC_JASON1_DORIS1B_MOE_CDDIS_COM";

    private static final String DATASETNAME_JASON1_DORIS1B_POE_CDDIS_COM = "DA_TC_JASON1_DORIS1B_POE_CDDIS_COM";
    
    public void initPrefixMap() {
        addDatasetNamePrexif(DATASETNAME_JASON1_DORIS1B_MOE_CDDIS, PREFIX_MOE_CDDIS);
        addDatasetNamePrexif(DATASETNAME_JASON1_DORIS1B_MOE_CDDIS_COM, PREFIX_MOE_CDDIS_COM);
        addDatasetNamePrexif(DATASETNAME_JASON1_DORIS1B_POE_CDDIS_COM, PREFIX_POE_CDDIS_COM);        
    }
}