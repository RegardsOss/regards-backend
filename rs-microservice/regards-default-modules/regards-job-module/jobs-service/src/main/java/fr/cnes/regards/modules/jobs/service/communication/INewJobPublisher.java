/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.communication;

import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;

/**
 * @author lmieulet
 */
@FunctionalInterface
public interface INewJobPublisher {

    void sendJob(long pJobInfoId) throws RabbitMQVhostException;

}