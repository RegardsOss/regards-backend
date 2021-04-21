/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * @author Marc Sordi
 * @author Sébastien Binda
 *
 */
public enum IngestJobPriority {

    INGEST_PROCESSING_JOB_PRIORITY {

        @Override
        public int getPriority() {
            return 0;
        }
    },

    CHOOSE_VERSIONING_JOB_PRIORITY {

        @Override
        public int getPriority() {
            return 0;
        }
    },

    SESSION_DELETION_JOB_PRIORITY {

        @Override
        public int getPriority() {
            return 0;
        }
    },

    UPDATE_AIP_SCAN_JOB_PRIORITY {

        @Override
        public int getPriority() {
            return 0;
        }
    },

    UPDATE_AIP_RUNNER_PRIORITY {

        @Override
        public int getPriority() {
            return 0;
        }
    },

    AIP_SAVE_METADATA_RUNNER_PRIORITY {

        @Override
        public int getPriority() {
            return 0;
        }
    },
    OAIS_DELETION_JOB_PRIORITY {

        @Override
        public int getPriority() {
            return 0;
        }
    },

    REQUEST_DELETION_JOB_PRIORITY {

        @Override
        public int getPriority() {
            return 0;
        }
    },

    REQUEST_RETRY_JOB_PRIORITY {

        @Override
        public int getPriority() {
            return 0;
        }
    },
    POST_PROCESSING_JOB {

        @Override
        public int getPriority() {
            return 0;
        }
    };

    public abstract int getPriority();
}
