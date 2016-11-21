/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.communication;

import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.modules.jobs.domain.JobInfo;

/**
 * @author Léo Mieulet
 */
@FunctionalInterface
public interface INewJobPublisher {

    /**
     * 
     * @param pJobInfoId
     *            the {@link JobInfo} id
     * @throws RabbitMQVhostException
     *             rabbit host not found
     */
    void sendJob(long pJobInfoId) throws RabbitMQVhostException;

}