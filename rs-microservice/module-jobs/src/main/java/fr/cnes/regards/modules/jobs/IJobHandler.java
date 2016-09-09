package fr.cnes.regards.modules.jobs;

public interface IJobHandler {

    StatusInfo create(IJob job);

    StatusInfo delete(IJob job);

    StatusInfo execute(IJob job);

    IJob getJob(JobId jobId);

    StatusInfo handle(IJob job, Object... params);

    StatusInfo restart(IJob job);

    StatusInfo stop(JobId jobId);

}
