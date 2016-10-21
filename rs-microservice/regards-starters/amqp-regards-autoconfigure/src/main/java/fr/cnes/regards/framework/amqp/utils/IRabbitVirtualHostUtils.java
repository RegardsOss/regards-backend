/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.utils;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;

import fr.cnes.regards.framework.amqp.exception.AddingRabbitMQVhostPermissionException;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;

/**
 * @author svissier
 *
 */
public interface IRabbitVirtualHostUtils {

    /**
     * GET Request to host/api/vhosts to know which Vhosts are already defined
     */
    void retrieveVhostList();

    /**
     * @return basic authentication to the broker
     */
    String setBasic();

    /**
     * @return complete url string representing rabbitMQ api endpoint for vhost
     */
    String getRabbitApiVhostEndpoint();

    /**
     *
     * PUT Request to /api/vhost/{name} to add this Vhost only if it is not already defined
     *
     * @param pName
     *            name of the Vhost you want to add
     * @param pConnectionFactory
     *            connection factory to which the virtual host should be bound to for further use
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */
    void addVhost(String pName, CachingConnectionFactory pConnectionFactory) throws RabbitMQVhostException;

    /**
     *
     * PUT Request to /api/vhost/{name} to add this Vhost only if it is not already defined
     *
     * @param pName
     *            name of the Vhost you want to add
     * @throws RabbitMQVhostException
     *             represent any error that could occur while handling RabbitMQ Vhosts
     */
    void addVhost(String pName) throws RabbitMQVhostException;

    /**
     *
     * Determine if the request done is to be considered successful
     *
     * @param pStatusValue
     *            status to examine
     * @return true if the request was successfull, false otherwise
     */
    boolean isSuccess(int pStatusValue);

    /**
     * @param pName
     *            name of the Vhost you want to check
     * @return true if the vhost is already known
     */
    boolean existVhost(String pName);

    /**
     * @return either the message broker is running or not
     */
    boolean brokerRunning();

    /**
     * @param pVhost
     *            vhost on which we want to add permission to our user
     * @return parameterized url to add permission on specified vhost to the user used to interact with the broker
     */
    String getRabbitApiPermissionVhostEndpoint(String pVhost);

    /**
     * @param pVhost
     *            vhost to add to our user permission
     * @throws AddingRabbitMQVhostPermissionException
     *             represent any error that could occur while adding the permission to the specified vhost
     *
     */
    void addPermissionToAccessVhost(String pVhost) throws AddingRabbitMQVhostPermissionException;

    /**
     * @return parameterized url to /api of the broker
     */
    String getRabbitApiEndpoint();

    /**
     * @param pRabbitmqUserName
     *            username
     * @param pRabbitmqPassword
     *            password
     * @return the encoded credential to give to the broker
     */
    String encode(String pRabbitmqUserName, String pRabbitmqPassword);

}