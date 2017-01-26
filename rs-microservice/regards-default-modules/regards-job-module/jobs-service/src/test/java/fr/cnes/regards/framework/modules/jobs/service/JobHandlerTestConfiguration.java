/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.jobs.service.communication.INewJobPublisher;
import fr.cnes.regards.framework.modules.jobs.service.communication.NewJobPublisher;
import fr.cnes.regards.framework.modules.jobs.service.service.IJobInfoService;
import fr.cnes.regards.framework.modules.jobs.service.stub.JobInfoServiceStub;
import fr.cnes.regards.framework.modules.jobs.service.stub.JobInfoSystemServiceStub;
import fr.cnes.regards.framework.modules.jobs.service.systemservice.IJobInfoSystemService;

/**
 * @author Léo Mieulet
 */
@Configuration
@EnableAutoConfiguration
@PropertySource("classpath:tests.properties")
@ComponentScan
public class JobHandlerTestConfiguration {

    @Bean
    @Primary
    public IJobInfoSystemService getJobInfoSystemService() {
        return new JobInfoSystemServiceStub();
    }

    @Bean
    @Primary
    public IJobInfoService getJobInfoService() {
        return new JobInfoServiceStub();
    }

    @Autowired
    public IPublisher publisher;

    @Bean
    @Primary
    public INewJobPublisher getNewJobPublisher() {
        return new NewJobPublisher(publisher);
    }
}
