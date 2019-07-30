/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;

/**
 * @author svissier
 */
public interface IRabbitVirtualHostAdmin {

    /**
     * GET Request to host/api/vhosts to know which Vhosts are already defined
     * @return list of all virtual hosts
     */
    List<String> retrieveVhostList();

    /**
     * @return basic authentication to the broker
     */
    String setBasic();

    /**
     * @return complete url string representing rabbitMQ api endpoint for vhost /api/vhosts
     */
    String getRabbitApiVhostEndpoint();

    /**
     * PUT Request to /api/vhost/{vhostName} to add this Vhost only if it is not already defined
     * @param virtualHost name virtual host you want to add
     */
    void addVhost(String virtualHost);

    /**
     * DELETE Request to /api/vhost/{vhostName}
     * @param virtualHost name of virtual host you want to remove
     */
    void removeVhost(String virtualHost);

    /**
     * Retrieve {@link ConnectionFactory} for virtual host
     * @param virtualHost virtual host
     * @return vhost {@link ConnectionFactory}
     */
    ConnectionFactory getVhostConnectionFactory(String virtualHost);

    /**
     * Determine if the request done is to be considered successful
     * @param pStatusValue status to examine
     * @return true if the request was successfull, false otherwise
     */
    boolean isSuccess(int pStatusValue);

    /**
     * @param virtualHost name of the virtual host you want to check
     * @return true if the vhost is already known
     */
    boolean existVhost(String virtualHost);

    /**
     * @return either the message broker is running or not
     */
    boolean brokerRunning();

    /**
     * @return parameterized url to /api of the broker
     */
    String getRabbitApiEndpoint();

    /**
     * @param pRabbitmqUserName username
     * @param pRabbitmqPassword password
     * @return the encoded credential to give to the broker
     */
    String encode(String pRabbitmqUserName, String pRabbitmqPassword);

    /**
     * Bind {@link ConnectionFactory} to virtual host before declaring an AMQP element
     * @param virtualHost virtual host to bind
     */
    void bind(String virtualHost);

    /**
     * Unbind {@link ConnectionFactory} from virtual host
     */
    void unbind();

    /**
     * @return true if a {@link ConnectionFactory} is bound
     */
    boolean isBound();

    /**
     * @param virtualHost virtual host to bind
     * @return true if the virtual host {@link ConnectionFactory} is already bound
     */
    boolean isBound(String virtualHost);

    /**
     * Retrieve Virtual host mode
     */
    public VirtualHostMode getMode();
}