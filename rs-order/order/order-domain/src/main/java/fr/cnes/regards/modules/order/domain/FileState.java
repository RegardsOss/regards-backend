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
package fr.cnes.regards.modules.order.domain;

/**
 * In case a file is nearline, it is at one of these states :
 * - PENDING : asked to storage to make it available,
 * - AVAILABLE : available to be downloaded,
 * - DOWNLOADED : already downloaded (maybe no more available),
 * - DOWNLOAD_ERROR : AVAILABLE but failed when attempting to be downloaded,
 * - PROCESSING_ERROR : failed while applying treatment (NOT available),
 * - ERROR : in error while asked to be made available.
 * BEWARE !!!
 * - ONLINE files are not stored into rs-storage BUT are managed by rs-storage. Hence, it is mandatory to
 * ask storage for their availability and storage respond they are immediately available.
 * So, from order point of view, an online data file is the same as a NEARLINE data file.
 *
 * @author oroussel
 */
public enum FileState {
    AVAILABLE(false),
    DOWNLOADED(true),
    DOWNLOAD_ERROR(true),
    PROCESSING_ERROR(true),
    ERROR(true),
    PENDING(false);

    private final boolean finalState;

    FileState(boolean finalState) {
        this.finalState = finalState;
    }

    public boolean isFinalState() {
        return finalState;
    }
}
