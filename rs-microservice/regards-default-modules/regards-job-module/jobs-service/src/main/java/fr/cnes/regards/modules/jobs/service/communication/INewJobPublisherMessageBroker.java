/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.communication;

import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;

/**
 * 
 */
public interface INewJobPublisherMessageBroker {

    void sendJob(long pJobInfoId) throws RabbitMQVhostException;

}