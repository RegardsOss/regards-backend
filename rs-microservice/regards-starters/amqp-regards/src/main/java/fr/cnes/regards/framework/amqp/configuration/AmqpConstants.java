/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp.configuration;

/**
 * AMQP utility constants
 * @author Marc Sordi
 */
public final class AmqpConstants {

    /**
     * Base name for the AMQP manager virtual host.
     */
    public static final String AMQP_INSTANCE_MANAGER = "regards.instance.manager";

    public static final String AMQP_MULTITENANT_MANAGER = "regards.multitenant.manager";

    public static final String INSTANCE_TENANT = "instance";

    /**
     * Headers
     */
    public static final String REGARDS_HEADER_NS = "regards.";

    public static final String REGARDS_TENANT_HEADER = REGARDS_HEADER_NS + "tenant";

    private AmqpConstants() {
    }
}
