/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.service.service;

/**
 * @author Léo Mieulet
 */
@Deprecated
public class JobInfoServiceTest {

/*    private IJobInfoRepository jobInfoRepository;

    private JobInfoService jobInfoService;

    private INewJobPublisher newJobPublisher;

    private IStoppingJobPublisher stoppingJobPublisher;

    private JobInfo pJobInfo;

    @Before
    public void setUp() {
        jobInfoRepository = Mockito.mock(IJobInfoRepository.class);
        newJobPublisher = Mockito.mock(INewJobPublisher.class);
        stoppingJobPublisher = Mockito.mock(IStoppingJobPublisher.class);
        jobInfoService = new JobInfoService(jobInfoRepository, newJobPublisher, stoppingJobPublisher);

        // Create a new jobInfo
        final Set<JobParameter> pParameters = new HashSet<>();
        pParameters.add(new JobParameter("follow", "Kepler"));
        final String jobClassName = "fr.cnes.regards.framework.modules.jobs.service.manager.AJob";
        final OffsetDateTime pEstimatedCompletion = OffsetDateTime.now().plusHours(5);
        final OffsetDateTime pExpirationDate = OffsetDateTime.now().plusDays(15);
        final String description = "some job description";
        final String owner = "IntegrationTest";
        final JobConfiguration pJobConfiguration = new JobConfiguration(description, pParameters, jobClassName,
                pEstimatedCompletion, pExpirationDate, 1, null, owner);

        JobStatusInfo statusInfo = new JobStatusInfo(description, pExpirationDate, )
        pJobInfo = new JobInfo(pJobConfiguration);
        pJobInfo.setId(1L);
    }

    // TODO: create a new JobHandlerUT
    //
    @Test
    public void testCreateJobInfo() throws RabbitMQVhostException {
        Mockito.when(jobInfoRepository.save(pJobInfo)).thenReturn(pJobInfo);
        final JobInfo jobInfo = jobInfoService.createJobInfo(pJobInfo);

        Mockito.verify(newJobPublisher).sendJob(pJobInfo.getId());
        Assertions.assertThat(jobInfo.getStatus()).isEqualTo(pJobInfo.getStatus());
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

    @Test
    public void testRetrieveJobInfoById() {
        final Long jobInfoId = 14L;
        final JobInfo jobInfo = new JobInfo();
        jobInfo.setId(jobInfoId);
        Mockito.when(jobInfoRepository.findOne(jobInfoId)).thenReturn(jobInfo);
        try {
            final JobInfo job = jobInfoService.retrieveJobInfoById(jobInfoId);
            Assert.assertNotNull(job);
        } catch (EntityNotFoundException e) {
            Assert.fail();
        }
    }

    @Test
    public void testRetrieveJobInfoListByState() {
        final JobStatus pStatus = JobStatus.QUEUED;
        jobInfoService.retrieveJobInfoListByState(pStatus);
        Mockito.verify(jobInfoRepository).findAllByStatusStatus(pStatus);
    }

    @Test
    public void testRetrieveJobInfoList() {
        jobInfoService.retrieveJobInfoList();
        Mockito.verify(jobInfoRepository).findAll();
    }*/
}
