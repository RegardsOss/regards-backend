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
package fr.cnes.regards.framework.integration.test.job;

import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Override {@link AbstractRegardsServiceIT} to provide Job Utils
 *
 * @author Léo Mieulet
 */
@ContextConfiguration(classes = { JobTestUtils.class })
@ActiveProfiles({ "nojobs" })
public abstract class AbstractRegardsServiceWithJobIT extends AbstractRegardsServiceIT {

    @Autowired
    private JobTestUtils jobTestUtils;

    public JobTestUtils getJobTestUtils() {
        return jobTestUtils;
    }
}
