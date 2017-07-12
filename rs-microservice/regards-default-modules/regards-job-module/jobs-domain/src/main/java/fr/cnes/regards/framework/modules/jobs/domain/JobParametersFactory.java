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
 *
 * Utility class to manage a {@link JobParameters}.
 *
 * @author Christophe Mertz
 */
public class JobParametersFactory {

    /**
     * 
     */
    private JobParameters parameters;

    /**
     * Constructor
     *
     */
    public JobParametersFactory() {
        parameters = new JobParameters();
    }

    /**
     *
     * Build a new class
     *
     * @return a factory
     */
    public static JobParametersFactory build() {
        return new JobParametersFactory();
    }

    /**
     *
     * Chained set method
     *
     * @param pParameterName
     *            the name parameter
     * 
     * @param pParameterValue
     *            the value parameter
     * 
     * @return the factory
     */
    public JobParametersFactory addParameter(String pParameterName, Object pParameterValue) {
        parameters.add(pParameterName, pParameterValue);
        return this;
    }

    public JobParameters getParameters() {
        return parameters;
    }
}
