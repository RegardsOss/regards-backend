/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

import java.util.UUID;

public interface IJobHandler {

    StatusInfo create(IJob job);

    StatusInfo delete(IJob job);

    StatusInfo execute(IJob job);

    IJob getJob(UUID jobId);

    StatusInfo handle(IJob job, Object... params);

    StatusInfo restart(IJob job);

    StatusInfo stop(UUID jobId);

}
