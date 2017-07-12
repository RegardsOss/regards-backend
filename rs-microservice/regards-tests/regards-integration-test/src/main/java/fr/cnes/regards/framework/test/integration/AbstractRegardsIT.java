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
package fr.cnes.regards.framework.test.integration;

import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.framework.jpa.multitenant.test.DefaultTestConfiguration;

/**
 * Base class to realize integration tests using JWT and MockMvc and mocked Cots. Should hold all the configurations to
 * be considred by any of its children.
 *
 * @author svissier
 * @author Sébastien Binda
 */
@ContextConfiguration(classes = { DefaultTestConfiguration.class, MockAmqpConfiguration.class })
public abstract class AbstractRegardsIT extends AbstractRegardsITWithoutMockedCots {

}
