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
package fr.cnes.regards.modules.storage.service;

/**
 * Centralize the storage jobs priorities.
 *
 * @author Sébastien Binda
 */
public final class StorageJobsPriority {

    public static final int FILE_CACHE_JOB = 30;

    public static final int FILE_STORAGE_JOB = 40;

    public static final int FILE_DELETION_JOB = 60;

    public static final int FILE_COPY_JOB = 80;

    public static final int CACHE_VERIFICATION = 90;

    public static final int CACHE_PURGE = 100;

    public static final int STORAGE_PERIODIC_ACTION_JOB = 150;

    private StorageJobsPriority() {
    }
}
