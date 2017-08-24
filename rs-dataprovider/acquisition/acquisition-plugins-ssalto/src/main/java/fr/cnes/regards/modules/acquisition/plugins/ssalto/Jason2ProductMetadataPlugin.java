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
 * plugin de creation des meta donnees d'un produit jason2. Ce plugin est generique, et utilise un fichier de
 * configuration pour pouvoir initialiser un finder par attribut a trouver. Il y a differents types de finder, les
 * fileNameFinder, et les autres. Chaque finder renvoie une class Attribute qui est ajoutee ensuite dans une class
 * DataObjectDescriptionElement. Cet Element sert a creer le fichier de configuration grace à la classe descriptorFile
 * 
 * @author CS
 * @version 1.2
 * @since 1.2
 */
public class Jason2ProductMetadataPlugin extends GenericProductMetadataPlugin {

    private static final String PROJECT_NAME = "JASON2";

    protected String getProjectName() {
        return PROJECT_NAME;
    }
}
