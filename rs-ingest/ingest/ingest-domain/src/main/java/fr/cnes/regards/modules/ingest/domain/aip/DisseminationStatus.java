/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.aip;

/**
 * The status of the dissemination of an AIP.
 * This status concerns the dissemination to all Regards destinations.
 *
 * @author Thomas GUILLOU
 **/
public enum DisseminationStatus {
    /**
     * No dissemination started
     */
    NONE,
    /**
     * The dissemination of the AIP is not finished, or is in error. That means at least one DisseminationInfo of
     * the AIP still has no ack date.
     */
    PENDING,
    /**
     * The dissemination of the AIP is finished. All DisseminationInfo of the AIP have an ack date.
     */
    DONE
}
