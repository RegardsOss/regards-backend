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
 * plugin specifiques au donnees jason2 GPSP10Flot Les attributs traites specifiquement sont les TIME_PERIOD ( résolu
 * comme pour les donnees Doris, et FILE_CREATION_DATE, qui ne se recupere pas de la meme maniere en fonction du nom des
 * fichiers.
 *
 * @author CS
 * @version 1.2
 * @since 1.2
 */

public class Jason2Ptlm1ProductMetadataPlugin extends JasonPltm1ProductMetadataPlugin {

    private static final String PROJECT_NAME = "JASON2";

    @Override
    protected String getProjectName() {
        return PROJECT_NAME;
    }

    @Override
    protected String getProjectPrefix() {
        return "JA2";
    }

}
