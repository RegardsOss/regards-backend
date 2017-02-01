/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.service.communication;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;

/**
 * @author Léo Mieulet
 */
@FunctionalInterface
public interface INewJobPublisher {

    /**
     *
     * @param pJobInfoId
     *            the {@link JobInfo} id
     */
    void sendJob(long pJobInfoId);

}