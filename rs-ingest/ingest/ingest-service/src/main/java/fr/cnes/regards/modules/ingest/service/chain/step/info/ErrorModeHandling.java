/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.chain.step.info;

/**
 * Enum to handle {@link fr.cnes.regards.modules.ingest.service.chain.step.AbstractIngestStep} in case of error.
 *
 * @author Iliana Ghazali
 **/
public enum ErrorModeHandling {

    /**
     * The state of the request will be updated with errors.
     */
    HANDLE_ONLY_REQUEST_ERROR,

    /**
     * The state of the request will be updated with errors along with the job which is handling it.
     * The job will be considered as failed because an unexpected error occurred during its execution.
     */
    HANDLE_REQUEST_WITH_JOB_CRASH,

    /**
     * No action will be performed. The request state remains unchanged to allow the admin to handle this specific case.
     */
    NOTHING_TO_DO;

}
