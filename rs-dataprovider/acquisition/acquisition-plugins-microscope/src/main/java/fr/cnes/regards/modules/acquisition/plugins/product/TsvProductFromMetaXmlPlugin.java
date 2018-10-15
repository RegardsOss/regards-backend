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
package fr.cnes.regards.modules.acquisition.plugins.product;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;

/**
 * Microscope Metadata XML product name reader plugin.<br/>
 * This plugin retrieves product name from metadata "_metadata.xml" files, it is the value under nomFichierDonnee tag
 * without ".tsv" extension..
 * @author Olivier Rousselot
 */
@Plugin(id = "TsvProductFromMetaXmlPlugin", version = "1.0.0-SNAPSHOT",
        description = "Reade metadata XML files to retrieve product name", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class TsvProductFromMetaXmlPlugin extends ProductFromMetaXmlPlugin {

    @Override
    protected String findProductNameFromTagContent(String text) {
        return text.substring(0, text.indexOf(".tsv"));
    }
}
