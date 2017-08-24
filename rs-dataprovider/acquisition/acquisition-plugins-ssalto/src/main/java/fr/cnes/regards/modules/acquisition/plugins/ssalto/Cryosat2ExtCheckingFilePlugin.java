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
 * Plugin de validation des fichiers CRYOSAT2.<br>
 * L'identifiant du produit retourne correspond à l'identifiant du fichier sans l'extension.
 * 
 * @author Christophe Mertz
 *
 */
public class Cryosat2ExtCheckingFilePlugin extends AbstractCheckingFilePlugin {

    private static String EXTENSION_HDR = ".HDR";

    private static String EXTENSION_DBL = ".DBL";

    protected void initExtensionList() {
        extensionList.add(EXTENSION_HDR);
        extensionList.add(EXTENSION_DBL);
    }

}
