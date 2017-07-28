package fr.cnes.regards.framework.modules.jobs.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.event.StopJobEvent;
import fr.cnes.regards.framework.modules.jobs.fr.cnes.framework.modules.jobs.domain.WaiterJob;
import fr.cnes.regards.framework.modules.jobs.service.service.IJobInfoService;
import fr.cnes.regards.framework.modules.jobs.test.JobConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * @author oroussel
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JobConfiguration.class })
public class JobServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobServiceTest.class);

    public static final String TENANT = "JOBS";

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IJobInfoRepository jobInfoRepos;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IRabbitVirtualHostAdmin rabbitVhostAdmin;

    @Autowired
    private RegardsAmqpAdmin amqpAdmin;

    @Before
    public void setUp() throws Exception {
        tenantResolver.forceTenant(TENANT);

        rabbitVhostAdmin.bind(tenantResolver.getTenant());

        amqpAdmin.purgeQueue(StopJobEvent.class, (Class<IHandler<StopJobEvent>>) Class
                .forName("fr.cnes.regards.framework.modules.jobs.service.JobService$StopJobHandler"), false);
        rabbitVhostAdmin.unbind();

        jobInfoRepos.deleteAll();
    }

    @Test
    public void test() throws InterruptedException {
        // TODO Ajouter des handler pour gérer le cycle de vie du JOB

        JobInfo waitJobInfo = new JobInfo();
        waitJobInfo.setPriority(10);
        waitJobInfo.setClassName(WaiterJob.class.getName());
        waitJobInfo.setDescription("Job that wait");
        waitJobInfo.setParameters(new JobParameter(WaiterJob.WAIT_PERIOD, "1000"),
                                  new JobParameter(WaiterJob.WAIT_PERIOD_COUNT, "10"));
        waitJobInfo = jobInfoService.create(waitJobInfo);

        Thread.sleep(1_000);
        jobInfoService.stopJob(waitJobInfo.getId());

        Thread.sleep(20_000);
    }
}