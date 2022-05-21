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


package fr.cnes.regards.modules.ingest.service.schedule;

/**
 * @author Iliana Ghazali
 */

public class SchedulerConstant {

    private SchedulerConstant() {
    }

    /**
     * Common
     */

    public static final String LOG_FORMAT = "[{}] {} {} scheduled in {} ms";

    public static final Long MAX_TASK_DELAY = 60L; // In second

    public static final String DEFAULT_INITIAL_DELAY = "10000";

    public static final String DEFAULT_SCHEDULING_DELAY = "1000";

    /**
     * For postprocess
     */
    public static final String POST_PROCESS_REQUESTS = "AIP POST PROCESS REQUESTS";

    public static final String POST_PROCESS_REQUEST_LOCK = "scheduledAIPPostProcess";

    public static final String POST_PROCESS_TITLE = "Post process scheduling";

    /**
     * For aip update
     */

    public static final String AIP_UPDATE_REQUESTS = "AIP UPDATE REQUESTS";

    public static final String AIP_UPDATE_REQUEST_LOCK = "scheduledAIPUpdate";

    public static final String AIP_UPDATE_TITLE = "AIP update scheduling";

    /**
     * For aip deletion
     */
    public static final String AIP_DELETION_REQUESTS = "AIP DELETION REQUESTS";

    public static final String AIP_DELETION_REQUEST_LOCK = "scheduledAIPDeletion";

    public static final String AIP_DELETION_TITLE = "AIP deletion scheduling";

    /**
     * For request pending
     */
    public static final String UNLOCK_REQ_SCHEDULER_LOCK = "request-pending-scheduler-lock";

    public static final String UNLOCK_ACTIONS = "UNLOCK REQUESTS ACTIONS";

    public static final String UNLOCK_TITLE = "Unlock requests scheduling";

    /**
     * For aip metadata
     */
    public static final String AIP_SAVE_METADATA_REQUESTS = "AIP SAVE METADATA REQUESTS";

    public static final String AIP_SAVE_METADATA_REQUEST_LOCK = "scheduledAIPSaveMetadata";

    public static final String AIP_SAVE_METADATA_TITLE = "AIP save metadata scheduling";
}
