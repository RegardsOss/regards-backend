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
package fr.cnes.regards.modules.fileaccess.dto;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Status for a File Storage Request
 *
 * @author Thibaud Michaudel
 **/
public enum StorageRequestStatus {

    // Note for developers :
    // This enum is stored using ordinals. Please consider that altering the orders of the values might have
    // consequences on the microservice behaviour.
    // See IFileStorageRequestAggregationRepository#findRequestChecksumToHandle()

    /**
     * Request is valid. Ordinal 0
     */
    GRANTED,

    /**
     * Request can be handled. Ordinal 1
     */
    TO_HANDLE,

    /**
     * Request is being handled. Ordinal 2
     */
    HANDLED,

    /**
     * Request is delayed, waiting to be reactivated. Ordinal 3
     */
    DELAYED,

    /**
     * Request is completed and can be deleted. Ordinal 4
     */
    TO_DELETE,

    /**
     * Request is finished in error. Ordinal 5
     */
    ERROR;

    public final static Set<StorageRequestStatus> RUNNING_STATUS = Stream.of(StorageRequestStatus.TO_HANDLE,
                                                                             StorageRequestStatus.HANDLED)
                                                                         .collect(Collectors.toCollection(HashSet::new));

    public final static Set<StorageRequestStatus> RUNNING_AND_DELAYED_STATUS = Stream.of(StorageRequestStatus.TO_HANDLE,
                                                                                         StorageRequestStatus.HANDLED,
                                                                                         StorageRequestStatus.DELAYED)
                                                                                     .collect(Collectors.toCollection(
                                                                                         HashSet::new));
}
