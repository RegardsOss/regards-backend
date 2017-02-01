/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.service.communication;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;

/**
 * @author Léo Mieulet
 */
@FunctionalInterface
public interface IStoppingJobPublisher {

    /**
     * @param pJobInfoId
     *            the {@link JobInfo} id
     */
    void send(Long pJobInfoId);

}