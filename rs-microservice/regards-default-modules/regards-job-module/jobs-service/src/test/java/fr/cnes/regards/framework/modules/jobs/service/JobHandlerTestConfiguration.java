/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
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
