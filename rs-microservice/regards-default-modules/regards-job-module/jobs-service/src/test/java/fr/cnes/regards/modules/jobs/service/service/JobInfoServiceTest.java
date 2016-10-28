/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.service.service;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.modules.jobs.domain.JobInfo;

/**
 *
 */
public class JobInfoServiceTest {

    private IJobInfoRepository jobInfoRepository;

    private JobInfoService jobInfoService;

    @Before
    public void setUp() {
        jobInfoRepository = Mockito.mock(IJobInfoRepository.class);
        jobInfoService = new JobInfoService(jobInfoRepository);
    }

    @Test
    public void testCreate() {
        final JobInfo jobInfo = new JobInfo();
        jobInfoService.createJobInfo(jobInfo);
        Mockito.verify(jobInfoRepository).save(jobInfo);
    }

    @Test
    public void testSave() {
        final JobInfo jobInfo = new JobInfo();
        jobInfoService.save(jobInfo);
        Mockito.verify(jobInfoRepository).save(jobInfo);
    }
}
