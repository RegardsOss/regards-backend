/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.domain.plugin;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.storage.domain.flow.AvailabilityFlowItem;

/**
 * Plugin to handle NEARLINE storage location. <br/>
 * A nearline storage location is a lcoation where files cannot be accessed synchronously.<br/>
 * Files need to be restored in cache before they can be access for download.<br/>
 * See {@link AvailabilityFlowItem} for more information.
 *
 * @author Sébastien Binda
 */
@PluginInterface(description = "Contract to respect by any NEARLINE data storage plugin")
public interface INearlineStorageLocation extends IStorageLocation {

    /**
     * Do the retrieve action for the given working subset.
     * @param workingSubset Subset of files to restore.
     * @param progressManager {@link IRestorationProgressManager} object to inform global store process after each transfer succeed or fail.
     */
    void retrieve(FileRestorationWorkingSubset workingSubset, IRestorationProgressManager progressManager);

}
