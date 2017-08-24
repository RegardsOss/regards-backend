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
 * Plugin de verification pour les donnees LOG_VOL_POS3 de JASON2.<br>
 * l'identifiant du produit retourne correspond a l'identifiant du fichier moins l'extention _HDR ou _DBL ou _TCH.
 * 
 * @author Christophe Mertz
 *
 */
public class LogVolAltikaCheckingFilePlugin extends AbstractCheckingFilePlugin {

    /** Valeurs des extensions */
    private static String EXTENSION_HDR = "_HDR";

    private static String EXTENSION_BIN = "_BIN";

    private static String EXTENSION_TCH = "_TCH";

    protected void initExtensionList() {
        extensionList.add(EXTENSION_BIN);
        extensionList.add(EXTENSION_HDR);
        extensionList.add(EXTENSION_TCH);
    }
}
