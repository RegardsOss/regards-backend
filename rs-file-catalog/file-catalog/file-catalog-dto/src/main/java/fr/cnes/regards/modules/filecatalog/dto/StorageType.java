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
package fr.cnes.regards.modules.filecatalog.dto;

/**
 * Type of storage, nearline or online.
 *
 * @author Sylvain Vissiere-Guerinet
 */
public enum StorageType {
    /**
     * Storage where data is always accessible
     */
    ONLINE(3),
    /**
     * Storage where a restore is needed before download
     */
    NEARLINE(2),
    /**
     * Storage where data are not accessible
     */
    OFFLINE(1),
    /**
     * Must not be used except for internal or external storage cache for Regards.
     */
    CACHE(0);

    /**
     * Priority of storage.
     * Priority is used to get data from the highest priority storage if it is present
     * on multiple storage.
     */
    private final int priority;

    StorageType(int priority) {
        this.priority = priority;
    }

    public int comparePriorityWith(StorageType storageType) {
        return this.priority - storageType.priority;
    }
}