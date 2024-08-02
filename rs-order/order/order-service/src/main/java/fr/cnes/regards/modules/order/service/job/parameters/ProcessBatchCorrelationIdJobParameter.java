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
package fr.cnes.regards.modules.order.service.job.parameters;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;

/**
 * This class is a Job Parameter used by {@link fr.cnes.regards.modules.order.service.job.ProcessExecutionJob}.
 *
 * @author Guillaume Andrieu
 */
public class ProcessBatchCorrelationIdJobParameter extends JobParameter {

    public static final String NAME = "batchCorrelationId";

    public ProcessBatchCorrelationIdJobParameter(String value) {
        super(NAME, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getValue() {
        return super.getValue();
    }

    public void setValue(String value) {
        super.setValue(value);
    }

    public static boolean isCompatible(JobParameter param) {
        return param.getName().equals(NAME);
    }

}
