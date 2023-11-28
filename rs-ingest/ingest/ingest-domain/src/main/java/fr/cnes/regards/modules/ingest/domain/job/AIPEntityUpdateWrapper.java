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
package fr.cnes.regards.modules.ingest.domain.job;

import fr.cnes.regards.modules.filecatalog.dto.request.FileDeletionRequestDto;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;

import java.util.ArrayList;
import java.util.Collection;

/**
 * During a AIPJob update, we detect if the task have done some update on the entity to avoid
 * unecessary metadata save
 *
 * @author Léo Mieulet
 */
public class AIPEntityUpdateWrapper {

    /**
     * The AIP to
     */
    private AIPEntity aip;

    /**
     * True when the entity have not been updated
     */
    private boolean pristine = true;

    /**
     * True when the aip inside the AIPEntity have not been updated
     */
    private boolean aipPristine = false;

    private Collection<FileDeletionRequestDto> deletionRequests;

    public AIPEntity getAip() {
        return aip;
    }

    public void setAip(AIPEntity aip) {
        this.aip = aip;
    }

    public boolean isPristine() {
        return pristine;
    }

    public Collection<FileDeletionRequestDto> getDeletionRequests() {
        return deletionRequests;
    }

    public boolean hasDeletionRequests() {
        return !deletionRequests.isEmpty();
    }

    public void setDeletionRequests(Collection<FileDeletionRequestDto> deletionRequests) {
        this.deletionRequests = deletionRequests;
    }

    public void addDeletionRequests(Collection<FileDeletionRequestDto> requests) {
        this.deletionRequests.addAll(requests);
    }

    public void markAsUpdated(boolean aipUpdated) {
        this.pristine = false;
        this.aipPristine = this.aipPristine || !aipUpdated;
    }

    public static AIPEntityUpdateWrapper build(AIPEntity aip) {
        AIPEntityUpdateWrapper wrapper = new AIPEntityUpdateWrapper();
        wrapper.aip = aip;
        wrapper.pristine = true;
        wrapper.aipPristine = true;
        wrapper.deletionRequests = new ArrayList<>();
        return wrapper;
    }

    public boolean isAipPristine() {
        return aipPristine;
    }
}