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
package fr.cnes.regards.modules.file.packager.service.scheduler;

/**
 * Centralize file-packager schedulers lock
 *
 * @author Thibaud Michaudel
 **/
public class FilePackagerSchedulersLock {

    /**
     * Common lock for {@link FilePackagingScheduler} and {@link CompletePackageScheduler} as they shouldn't be
     * running at the same time to prevent package update conflicts.
     */
    public static final String LOCK_ID = "file-packager-file-packaging";

    /**
     * Common lock title for {@link FilePackagingScheduler} and {@link CompletePackageScheduler} as they shouldn't be
     * running at the same time to prevent package update conflicts.
     */
    public static final String LOCK_TITLE = "File Packager file packaging scheduling";
}
