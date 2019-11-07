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

import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionSelectionMode;
import java.util.Set;

/**
 * @author Léo Mieulet
 */
public class OAISDeletionPayload {

    private SessionDeletionMode deletionMode;

    private SessionDeletionSelectionMode selectionMode;

    private Boolean deletePhysicalFiles = true;

    /**
     * URN of the SIP(s) to preserve or remove in the specified session (according to {@link #selectionMode})
     */
    private Set<String> sipIds;

    /**
     * Provider id(s) of the SIP to preserve or remove in the specified session (according to {@link #selectionMode})
     */
    private Set<String> providerIds;

    public SessionDeletionMode getDeletionMode() {
        return deletionMode;
    }

    public void setDeletionMode(SessionDeletionMode deletionMode) {
        this.deletionMode = deletionMode;
    }

    public SessionDeletionSelectionMode getSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(SessionDeletionSelectionMode selectionMode) {
        this.selectionMode = selectionMode;
    }

    public Boolean getDeletePhysicalFiles() {
        return deletePhysicalFiles;
    }

    public void setDeletePhysicalFiles(Boolean deletePhysicalFiles) {
        this.deletePhysicalFiles = deletePhysicalFiles;
    }

    public Set<String> getSipIds() {
        return sipIds;
    }

    public void setSipIds(Set<String> sipIds) {
        this.sipIds = sipIds;
    }

    public Set<String> getProviderIds() {
        return providerIds;
    }

    public void setProviderIds(Set<String> providerIds) {
        this.providerIds = providerIds;
    }
}
