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
package fr.cnes.regards.modules.ingest.service.job;

/**
 * Ingest jobs priority management
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 */
public final class IngestJobPriority {

    public static final int INGEST_PROCESSING_JOB_PRIORITY = 0;

    public static final int CHOOSE_VERSIONING_JOB_PRIORITY = 0;

    public static final int SESSION_DELETION_JOB_PRIORITY = 0;

    public static final int UPDATE_AIP_SCAN_JOB_PRIORITY = 0;

    public static final int UPDATE_AIP_RUNNER_PRIORITY = 0;

    public static final int AIP_SAVE_METADATA_RUNNER_PRIORITY = 0;

    public static final int OAIS_DELETION_JOB_PRIORITY = 0;

    public static final int REQUEST_DELETION_JOB_PRIORITY = 0;

    public static final int REQUEST_RETRY_JOB_PRIORITY = 0;

    public static final int POST_PROCESSING_JOB = 0;

    public static final int DELETE_SIP_BODY_JOB_PRIORITY = 0;

    public static final int AIP_DISSEMINATION_JOB_PRIORITY = 0;

    private IngestJobPriority() {
    }
}
