/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.dto.request;

import fr.cnes.regards.modules.ingest.domain.IngestValidationMessages;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import jakarta.validation.constraints.NotNull;
import org.springframework.util.Assert;

/**
 * Session deletion request
 *
 * @author Marc SORDI
 * @author Léo Mieulet
 */
public class OAISDeletionPayloadDto extends SearchAIPsParameters {

    @NotNull(message = IngestValidationMessages.MISSING_SESSION_DELETION_MODE)
    private SessionDeletionMode deletionMode;

    /**
     * Build a new session deletion request
     */
    public static OAISDeletionPayloadDto build(SessionDeletionMode deletionMode) {
        Assert.notNull(deletionMode, IngestValidationMessages.MISSING_SESSION_DELETION_MODE);

        OAISDeletionPayloadDto item = new OAISDeletionPayloadDto();
        item.setDeletionMode(deletionMode);
        return item;
    }

    public SessionDeletionMode getDeletionMode() {
        return deletionMode;
    }

    public void setDeletionMode(SessionDeletionMode deletionMode) {
        this.deletionMode = deletionMode;
    }

}
