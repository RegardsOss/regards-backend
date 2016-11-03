/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.communication;

import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;

/**
 *
 */
public interface IStoppingJobPublisher {

    /**
     * @param pJobInfoId
     *            the jobInfo id
     * @throws RabbitMQVhostException
     *             rabbit host not found
     */
    void send(Long pJobInfoId) throws RabbitMQVhostException;

}