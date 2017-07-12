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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Convert;

import fr.cnes.regards.framework.modules.jobs.domain.converters.JobParameterConverter;

/**
 * @author Léo Mieulet
 *
 */
@Convert(converter = JobParameterConverter.class)
public class JobParameters {

    /**
     * store jobInfo parameters
     */
    private final Map<String, Object> parameters;

    /**
     * Default constructor
     */
    public JobParameters() {
        super();
        parameters = new HashMap<>();
    }

    /**
     * Define a job parameter
     *
     * @param pParamName
     *            The attribute name
     * @param pValue
     *            The attribute value
     */
    public void add(final String pParamName, final Object pValue) {
        parameters.put(pParamName, pValue);
    }

    /**
     * @return job parameters
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

}
