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
package fr.cnes.regards.modules.ingest.domain.request.update;

import com.google.common.collect.Lists;

/**
 * Types of AIP task update
 *
 * @author Léo Mieulet
 */
public enum AIPUpdateTaskType {

    /**
     * Task to add a tag to an AIP
     */
    ADD_TAG,
    /**
     * Task to remove a tag from an AIP
     */
    REMOVE_TAG,
    /**
     * Task to add a category
     */
    ADD_CATEGORY,
    /**
     * Task to remove a category
     */
    REMOVE_CATEGORY,
    /**
     * Task to remove a storage location
     */
    REMOVE_STORAGE,
    /**
     * Task to add a file location
     */
    ADD_FILE_LOCATION,
    /**
     * Task to remove a file location
     */
    REMOVE_FILE_LOCATION;

    /**
     * Get the comparator between two tasks, as some task must be executed before others
     *
     * @param otherTaskType
     */
    public int getOrder(AIPUpdateTaskType otherTaskType) {
        // Test if the current type should be executed at the end
        if (Lists.newArrayList(ADD_FILE_LOCATION, REMOVE_FILE_LOCATION).contains(this)) {
            // Test for same type of task
            if (Lists.newArrayList(ADD_FILE_LOCATION, REMOVE_FILE_LOCATION).contains(otherTaskType)) {
                return 0;
            }
            // The other task should be executed before the current one
            return -1;
        }
        // Test if the other task should be executed after the current one
        if (Lists.newArrayList(ADD_FILE_LOCATION, REMOVE_FILE_LOCATION).contains(otherTaskType)) {
            return 1;
        }
        return 0;
    }
}
