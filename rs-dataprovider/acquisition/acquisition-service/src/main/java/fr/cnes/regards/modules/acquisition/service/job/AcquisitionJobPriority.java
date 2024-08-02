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
package fr.cnes.regards.modules.acquisition.service.job;

/**
 * Acquisition jobs priority management
 *
 * @author Marc Sordi
 */
public final class AcquisitionJobPriority {

    /**
     * Only one job per acquisition chain can be available at a time
     */
    public static final int PRODUCT_ACQUISITION_JOB_PRIORITY = 10;

    /**
     * One product, one job!
     */
    public static final int SIP_GENERATION_JOB_PRIORITY = 0;

    /**
     * Only one job per acquisition chain can be available at a time
     */
    public static final int SIP_SUBMISSION_JOB_PRIORITY = 10;

    /**
     * One product, one job!
     */
    public static final int POST_ACQUISITION_JOB_PRIORITY = 0;

    public static final int DELETION_JOB = 100;

    private AcquisitionJobPriority() {
    }
}
