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
package fr.cnes.regards.modules.ingest.domain.request.deletion;

import javax.validation.constraints.NotNull;

import fr.cnes.regards.modules.ingest.domain.IngestValidationMessages;
import fr.cnes.regards.modules.ingest.dto.aip.AbstractSearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;

/**
 * Payload for {@link OAISDeletionRequest}
 *
 * @author Sébastien Binda
 */
public class OAISDeletionPayload extends AbstractSearchAIPsParameters<OAISDeletionPayload> {

    @NotNull(message = IngestValidationMessages.MISSING_SESSION_DELETION_MODE)
    private SessionDeletionMode deletionMode;

    private boolean deleteFiles;

    /**
     * All internal request steps including local and remote ones
     */
    @NotNull(message = "Deletion request step is required")
    private DeletionRequestStep step;

    public SessionDeletionMode getDeletionMode() {
        return deletionMode;
    }

    public void setDeletionMode(SessionDeletionMode deletionMode) {
        this.deletionMode = deletionMode;
    }

    public static OAISDeletionPayload build(SessionDeletionMode deletionMode, boolean deleteFiles) {
        OAISDeletionPayload odp = new OAISDeletionPayload();
        odp.setDeletionMode(deletionMode);
        odp.step = DeletionRequestStep.INITIAL;
        odp.deleteFiles = deleteFiles;
        return odp;
    }

    public DeletionRequestStep getStep() {
        return step;
    }

    public void setStep(DeletionRequestStep step) {
        this.step = step;
    }

    public boolean isDeleteFiles() {
        return deleteFiles;
    }

}
