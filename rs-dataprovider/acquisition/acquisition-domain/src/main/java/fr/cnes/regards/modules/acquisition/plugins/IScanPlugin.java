/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;

/**
 * First <b>required</b> step of acquisition processing chain. This step is used to make disk scanning for file
 * detection.
 *
 * @author Marc Sordi
 *
 */
@PluginInterface(description = "File scanning plugin contract")
public interface IScanPlugin {

    /**
     * Scan disk to detect and retrieve files.<br/>
     * Warning : if last modification date is not used, file might be acquired several times!<br/>
     * <b>When using last modification date, we assume scan plugin is working with a precision to the second at least.
     * Plugin has to return files with last modification date equals or after the given last modification date.
     *  The system will filter duplicates if any!</b>
     *
     * @param lastModificationDate The last most recent last modification date of all the last scanned files. May be
     *            null for first scan!
     * @return list of detected files
     * @throws ModuleException if error occurs!
     */
    List<Path> scan(Optional<OffsetDateTime> lastModificationDate) throws ModuleException;
}
