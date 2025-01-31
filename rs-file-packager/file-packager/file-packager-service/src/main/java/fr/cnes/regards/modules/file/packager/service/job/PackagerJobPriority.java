/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.file.packager.service.job;

/**
 * Centralize file-packager job priorities.
 * Higher priority will be processed first.
 *
 * @author Thibaud Michaudel
 **/
public class PackagerJobPriority {

    public static final int DELETE_LOCAL_FILES_JOB = 150;

    public static final int STORE_COMPLETE_PACKAGE_JOB = 100;

    private PackagerJobPriority() {
    }
}
