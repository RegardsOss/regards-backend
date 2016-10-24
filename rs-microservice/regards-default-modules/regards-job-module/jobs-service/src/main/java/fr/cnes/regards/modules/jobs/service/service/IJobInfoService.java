/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.service;

import fr.cnes.regards.modules.jobs.domain.JobInfo;

/**
 *
 */
public interface IJobInfoService {

    JobInfo createJobInfo(final JobInfo pJobInfo);

    JobInfo updateJobInfo(final JobInfo pJobInfo);

}
