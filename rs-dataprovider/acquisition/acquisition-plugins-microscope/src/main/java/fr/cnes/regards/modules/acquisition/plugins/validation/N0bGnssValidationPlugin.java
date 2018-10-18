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
package fr.cnes.regards.modules.acquisition.plugins.validation;

import java.io.File;
import java.nio.file.Path;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;

/**
 * Microscope N0b_GNSS products validation.<br/>
 * File to validate is found under same directory as sag_descripteur.xml metadata file and has tgz extension. A file
 * with same name with _MD5.txt at the end (in place of .tgz) contains MD5 informations.
 * @author Olivier Rousselot
 */
@Plugin(id = "N0bGnssValidationPlugin", version = "1.0.0-SNAPSHOT", description =
        "Read given 'sag_descripteur.xml' metadata XML file path and validate tgz file under same directory with "
                + "MD5 value contained into associated '_MD5.txt' file",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class N0bGnssValidationPlugin extends N0xValidationPlugin {

    /**
     * Same directory as sa_descripteur.xml file
     */
    @Override
    protected File findProductDirectory(Path sagDescriptorFilePath) {
        return sagDescriptorFilePath.toFile().getParentFile();
    }
}
