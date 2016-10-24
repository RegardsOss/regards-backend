/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.systemservice;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.jobs.domain.JobStatus;

/**
 *
 */
public class JobInfoSystemServiceTest {

    private IJobInfoRepository jobInfoRepository;

    private JobInfoSystemService jobInfoSystemService;

    @Before
    public void setUp() {
        jobInfoRepository = Mockito.mock(IJobInfoRepository.class);
        jobInfoSystemService = new JobInfoSystemService(jobInfoRepository);
    }

    @Test
    public void testCreate() {
        final JobInfo jobInfo = new JobInfo();
        final String pTenantId = "project1";
        jobInfoSystemService.findJobInfo(pTenantId, 1L);
    }

    @Test
    public void testUpdate() {
        final JobInfo jobInfo = new JobInfo();
        final String pTenantId = "project1";
        jobInfoSystemService.updateJobInfo(pTenantId, jobInfo);
    }

    @Test
    public void testUpdateToDone() {
        final JobInfo jobInfo = new JobInfo();
        final String pTenantId = "project1";
        jobInfoSystemService.updateJobInfoToDone(1L, JobStatus.QUEUED, pTenantId);
    }
}
