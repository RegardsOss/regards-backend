/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.configuration;

import java.util.List;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;

/**
 * @author svissier
 *
 */
public interface IRabbitVirtualHostAdmin {

    /**
     * GET Request to host/api/vhosts to know which Vhosts are already defined
     *
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
     *
     * PUT Request to /api/vhost/{vhostName} to add this Vhost only if it is not already defined
     *
     * @param pTenant
     *            name of the tenant related to the Vhost you want to add
     */
    void addVhost(String pTenant);

    /**
     * DELETE Request to /api/vhost/{vhostName}
     *
     * @param pTenant
     *            name of the tenant related to the Vhost you want to remove
     */
    void removeVhost(String pTenant);

    /**
     * Retrieve {@link ConnectionFactory} for tenant
     *
     * @param pTenant
     *            tenant
     * @return vhost {@link ConnectionFactory}
     */
    ConnectionFactory getVhostConnectionFactory(String pTenant);

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
     * @param pTenant
     *            name of the tenant related to the Vhost you want to check
     * @return true if the vhost is already known
     */
    boolean existVhost(String pTenant);

    /**
     * @return either the message broker is running or not
     */
    boolean brokerRunning();

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

    /**
     * Bind {@link ConnectionFactory} to tenant (and vhost) before declaring an AMQP element
     *
     * @param pTenant
     *            tenant to bind
     */
    public void bind(String pTenant);

    /**
     * Unbind {@link ConnectionFactory} from tenant (and vhost)
     *
     */
    public void unbind();

    /**
     *
     * @return true if a {@link ConnectionFactory} is bound independently of the tenant
     */
    public boolean isBound();

    /**
     *
     * @param pTenant
     *            tenant
     * @return true if the tenant {@link ConnectionFactory} is already bound
     */
    public boolean isBound(String pTenant);
}