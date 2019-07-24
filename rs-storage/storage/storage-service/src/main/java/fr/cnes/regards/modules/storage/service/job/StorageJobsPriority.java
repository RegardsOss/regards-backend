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
package fr.cnes.regards.modules.storage.service.job;

/**
 * Stsorage Jobs prioitiy
 * STORE_METADATA_JOB > WRITING_METADATA_JOB > STORE_DATA_JOB > METADATA_DELETION_JOB > UPDATE_TAGS_JOB > DELETION_JOB
 * @author Sébastien Binda
 */
public class StorageJobsPriority {

    public static final Integer DELETION_JOB = 0;

    public static final Integer UPDATE_TAGS_JOB = 10;

    public static final Integer METADATA_DELETION_JOB = 20;

    public static final Integer STORE_DATA_JOB = 30;

    public static final Integer WRITING_METADATA_JOB = 40;

    public static final Integer STORE_METADATA_JOB = 50;

}
