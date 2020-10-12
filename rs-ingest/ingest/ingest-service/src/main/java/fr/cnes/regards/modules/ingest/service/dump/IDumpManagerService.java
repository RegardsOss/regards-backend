/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

package fr.cnes.regards.modules.ingest.service.dump;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.domain.dump.DumpSettings;
import fr.cnes.regards.modules.ingest.service.conf.IngestConfigurationManager;
import fr.cnes.regards.modules.ingest.service.schedule.AIPSaveMetadataScheduler;
/**
 * Dump Configuration Service Interface for {@link IngestConfigurationManager}
 * @author Iliana Ghazali
 */

public interface IDumpManagerService {

    /**
     * Update the {@link DumpSettings} and the {@link AIPSaveMetadataScheduler} with the new configuration
     * @param newDumpSettings the new dump configuration
     */
    void updateDumpAndScheduler(DumpSettings newDumpSettings) throws ModuleException;

    /**
     * Get the current {@link DumpSettings} for the tenant
     * @return current dumpSettings
     */
    DumpSettings getCurrentDumpSettings();
}
