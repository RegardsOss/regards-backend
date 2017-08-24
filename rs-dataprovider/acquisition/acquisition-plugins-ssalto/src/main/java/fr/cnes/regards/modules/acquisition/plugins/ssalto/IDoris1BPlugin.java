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
 * 
 * @author Christophe Mertz
 *
 */
public interface IDoris1BPlugin {

    /*
     * Liste des prefixes pour donnees DORIS1B
     */
    public static final String PREFIX_MOE_CDDIS = "MOE_CDDIS_";

    public static final String PREFIX_MOE_CDDIS_COM = "MOE_CDDIS_COM_";

    public static final String PREFIX_POE_CDDIS_COM = "POE_CDDIS_COM_";
    
    /**
     * Initialise la table de correspondance datasetName => prefix pour les
     * donnees Doris1B<br>
     * La methode utilise la methode <code>addDatasetNamePrexif</code> pour ce faire.
     * 
     * @since 1.4
     */
    public void initPrefixMap();
    
}
