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
package fr.cnes.regards.framework.swagger.autoconfigure;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Check that spring boot server is already configured
 *
 * @author msordi
 *
 */
public class ServerProperties extends AllNestedConditions {

	/**
     * Class constructor
     */
    public ServerProperties() {
        super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnProperty("server.address")
    static class OnServerAdress {

    }

    @ConditionalOnProperty("server.port")
    static class OnServerPort {

    }

}
