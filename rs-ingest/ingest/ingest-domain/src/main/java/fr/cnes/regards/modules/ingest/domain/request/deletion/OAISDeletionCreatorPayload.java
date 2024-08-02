/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.request.deletion;

import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;

/**
 * Payload for {@link OAISDeletionCreatorRequest}
 *
 * @author Léo Mieulet
 * @author Sébastien Binda
 */
public class OAISDeletionCreatorPayload extends SearchAIPsParameters {

    /**
     * This boolean is sent to storage
     */
    private Boolean deletePhysicalFiles = true;

    private SessionDeletionMode deletionMode;

    public Boolean getDeletePhysicalFiles() {
        return deletePhysicalFiles;
    }

    public void setDeletePhysicalFiles(Boolean deletePhysicalFiles) {
        this.deletePhysicalFiles = deletePhysicalFiles;
    }

    public SessionDeletionMode getDeletionMode() {
        return deletionMode;
    }

    public void setDeletionMode(SessionDeletionMode deletionMode) {
        this.deletionMode = deletionMode;
    }
}
