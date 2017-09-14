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

package fr.cnes.regards.modules.acquisition.plugins;

import java.util.HashSet;
import java.util.Set;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;

/**
 * Class ScanDirectoryPlugin A default {@link Plugin} of type {@link IConnectionPlugin}. Allows to
 *
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
@Plugin(id = "ScanDirectoryPlugin", version = "1.0.0-SNAPSHOT",
        description = "Scan directories to detect incoming data files", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class ScanDirectoryPlugin implements IAcquisitionScanPlugin {

    private static final String META_FILES_PARAM = "metaFileLists";

    @PluginParameter(name = META_FILES_PARAM)
    Set<MetaFile> metaFileList;

    @Override
    public Set<AcquisitionFile> getAcquisitionFiles() {
        Set<AcquisitionFile> metaFileList = new HashSet<>();

        AcquisitionFile a = new AcquisitionFile();
        a.setFileName("Hello");
        a.setSize(33L);
        metaFileList.add(a);

        AcquisitionFile b = new AcquisitionFile();
        b.setFileName("Coucou");
        b.setSize(156L);
        metaFileList.add(b);

        return metaFileList;
    }

}
