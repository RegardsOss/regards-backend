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
package fr.cnes.regards.modules.order.service.job.parameters;

import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;

/**
 * Expiration date specific job parameter, it contains the pair "expirationDate" : date
 * @author oroussel
 */
public class SubOrderAvailabilityPeriodJobParameter extends JobParameter {

    public static final String NAME = "subOrderAvailabilityPeriod";

    public SubOrderAvailabilityPeriodJobParameter(Integer value) {
        super(NAME, value);
    }

    /**
     * Check if given JobParameter is compatible with ExpirationDateJobParameter ie same name
     * @param param
     * @return {@link Boolean}
     */
    public static boolean isCompatible(JobParameter param) {
        return param.getName().equals(NAME);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Integer getValue() {
        return super.getValue();
    }

    public void setValue(Integer value) {
        super.setValue(value);
    }
}
