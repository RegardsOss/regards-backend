/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.communication;

/**
 * @author lmieulet
 */
@FunctionalInterface
public interface INewJobPuller {

    public Long getJob(final String pProjectName);
}
