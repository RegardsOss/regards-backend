/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.jobs.domain;

/**
 * JobInfo status
 * 
 * @author Léo Mieulet
 */
public enum JobStatus {
    /**
     * Job created but not yet to be taken into account
     */
    PENDING {
        @Override
        public boolean isCompatibleWithPause() {
            return false;
        }
    },
    /**
     * Job taken into account by job service pool
     */
    QUEUED {
        @Override
        public boolean isCompatibleWithPause() {
            return false;
        }
    },
    /**
     * Job taken into account by a microservice to be run
     */
    TO_BE_RUN {
        @Override
        public boolean isCompatibleWithPause() {
            return false;
        }
    },
    /**
     * Job running
     */
    RUNNING {
        @Override
        public boolean isCompatibleWithPause() {
            return false;
        }
    },
    /**
     * Job finished without error
     */
    SUCCEEDED {
        @Override
        public boolean isCompatibleWithPause() {
            return true;
        }
    },
    /**
     * Job finished with error(s)
     */
    FAILED {
        @Override
        public boolean isCompatibleWithPause() {
            return true;
        }
    },
    /**
     * Job cancelled
     */
    ABORTED {
        @Override
        public boolean isCompatibleWithPause() {
            return true;
        }
    };

    @Override
    public String toString() {
        return this.name();
    }

    /**
     * Tell wether or not specified status is compatible with a paused Job (ie job is ended or aborted)
     */
    public abstract boolean isCompatibleWithPause();
}
