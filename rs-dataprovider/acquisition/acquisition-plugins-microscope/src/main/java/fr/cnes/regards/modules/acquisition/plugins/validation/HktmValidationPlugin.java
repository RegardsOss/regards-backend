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
import fr.cnes.regards.modules.acquisition.plugins.Microscope;

/**
 * Microscope product validation from XML metadata file.<br/>
 * File to validate is found under "nomFichierDonnee" tag, MD5 value is under "md5Check" tag.
 * @author oroussel
 */
@Plugin(id = "HktmValidationPlugin", version = "1.0.0-SNAPSHOT", description =
        "Read given metadata XML file and validate determined file of which name is deducted from metadata"
                + " path ('metadonnees/year/month' vs 'year/month') with MD5 value under md5Check tag",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class HktmValidationPlugin extends ValidationFromMetaXmlPlugin {
    private static final String METADATA_DIR = "metadonnees";

    @Override
    protected File findDataFile(Path metadataPath, String dataFilename) {
        String metadataFilename = metadataPath.getFileName().toString();
        int rootPathIdx = -1;
        for (int i = 0; i < metadataPath.getNameCount(); i++) {
            if (metadataPath.getName(i).toString().equals(METADATA_DIR)) {
                rootPathIdx = i;
                break;
            }
        }
        // Metadata file path does not contain METADATA_DIR (should not occur)
        if (rootPathIdx == -1) {
            return null;
        }
        return metadataPath.subpath(0, rootPathIdx)
                .resolve(metadataPath.subpath(rootPathIdx + 1, metadataPath.getNameCount() - 1))
                .resolve(metadataFilename.substring(0, metadataFilename.indexOf(Microscope.METADATA_SUFFIX)))
                .resolve(dataFilename)
                .toFile();
    }
}
