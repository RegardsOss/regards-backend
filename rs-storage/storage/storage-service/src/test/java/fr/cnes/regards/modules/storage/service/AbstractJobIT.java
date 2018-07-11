package fr.cnes.regards.modules.storage.service;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.MimeType;

@ContextConfiguration(classes = {TestConfig.class, AIPServiceIT.Config.class})
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=storage_test", "regards.amqp.enabled=true" },
        locations = { "classpath:storage.properties" })
@ActiveProfiles({"testAmqp", "disableStorageTasks"})
@EnableAsync
@RunWith(SpringRunner.class)
public abstract class AbstractJobIT extends AbstractMultitenantServiceTest {

    public static final String SESSION = "SESSION 42";

    @SuppressWarnings("unused")
    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    protected Set<JobParameter> parameters;

    @Autowired
    protected IPluginConfigurationRepository pluginRepo;

    @Autowired
    protected IAIPDao aipDao;

    @Autowired
    protected IDataFileDao dataFileDao;

    @Autowired
    protected IJobInfoRepository jobInfoRepo;

    @Autowired
    protected IRuntimeTenantResolver tenantResolver;

    @Autowired
    protected IAIPService aipService;

    @Before
    public void init() throws IOException, URISyntaxException, ModuleException {
        tenantResolver.forceTenant(DEFAULT_TENANT);

        AIPSession aipSession = aipService.getSession(SESSION, true);
        for (int i = 0; i < 20; i++) {
            AIP aip = getNewAipWithTags(aipSession.getId(), "first tag", "second tag");
            aip.setState(AIPState.STORED);
            aipDao.save(aip, aipSession);
        }
    }

    protected JobInfo waitForJobFinished() throws InterruptedException {
        // Wait until the job finishes
        Iterable<JobInfo> jobs = jobInfoRepo.findAll();
        Assert.assertTrue("should have 1 job queued/running", jobs.iterator().hasNext());
        JobInfo jobInfo = jobs.iterator().next();
        // this loop acts like a timeout
        for (int i = 0; i < 40; i++) {
            //Pause for 1 seconds
            Thread.sleep(1000);
            JobInfo jobInfoRefreshed = jobInfoRepo.findById(jobInfo.getId());
            if (JobStatus.SUCCEEDED.equals(jobInfoRefreshed.getStatus().getStatus())) {
                break;
            }
        }
        return jobInfo;
    }


    protected AIP getNewAipWithTags(String aipSession, String... tags) throws MalformedURLException {
        AIPBuilder aipBuilder = new AIPBuilder(
                new UniformResourceName(OAISIdentifier.AIP, EntityType.DATA, DEFAULT_TENANT, UUID.randomUUID(), 1),
                null, EntityType.DATA, aipSession);
        aipBuilder.getContentInformationBuilder().setSyntax("text", "description", MimeType.valueOf("text/plain"));
        aipBuilder.addContentInformation();
        aipBuilder.getPDIBuilder().setAccessRightInformation("public");
        aipBuilder.getPDIBuilder().setFacility("CS");
        aipBuilder.getPDIBuilder().addProvenanceInformationEvent(EventType.SUBMISSION.name(), "test event",
                OffsetDateTime.now());
        aipBuilder.addTags(tags);
        return aipBuilder.build();
    }

    @After
    public void after() throws URISyntaxException, IOException {
        tenantResolver.forceTenant(DEFAULT_TENANT);
        jobInfoRepo.deleteAll();
        dataFileDao.deleteAll();
        pluginRepo.deleteAll();
        aipDao.deleteAll();
    }

    @Configuration
    static class Config {

        @Bean
        public INotificationClient notificationClient() {
            return Mockito.mock(INotificationClient.class);
        }
    }
}
