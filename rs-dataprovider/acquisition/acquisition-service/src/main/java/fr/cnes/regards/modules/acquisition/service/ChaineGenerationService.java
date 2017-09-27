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
package fr.cnes.regards.modules.acquisition.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.acquisition.dao.IChainGenerationRepository;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.job.ChainGenerationJobParameter;
import fr.cnes.regards.modules.acquisition.job.ScanJob;

/**
 *
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class ChaineGenerationService implements IChainGenerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChaineGenerationService.class);

    private final IChainGenerationRepository chainRepository;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    private final ISubscriber subscriber;

    //    @Autowired
    //    private IPluginService pluginService;

    private static Set<UUID> runnings = Collections.synchronizedSet(new HashSet<>());

    private static Set<UUID> succeededs = Collections.synchronizedSet(new HashSet<>());

    private static Set<UUID> aborteds = Collections.synchronizedSet(new HashSet<>());

    private static Set<UUID> faileds = Collections.synchronizedSet(new HashSet<>());

    public ChaineGenerationService(final ISubscriber subscriber, IChainGenerationRepository repository) {
        super();
        this.chainRepository = repository;
        this.subscriber = subscriber;
    }

    @PostConstruct
    public void init() {
        subscriber.subscribeTo(JobEvent.class, new ScanJobHandler());
    }

    @Override
    public ChainGeneration save(ChainGeneration chain) {
        return chainRepository.save(chain);
    }

    @Override
    public List<ChainGeneration> retrieveAll() {
        final List<ChainGeneration> chains = new ArrayList<>();
        chainRepository.findAll().forEach(c -> chains.add(c));
        return chains;
    }

    @Override
    public ChainGeneration retrieve(Long id) {
        return chainRepository.findOne(id);
    }

    @Override
    public void delete(Long id) {
        chainRepository.delete(id);
    }

    @Override
    public boolean run(Long id) throws InterruptedException {
        return run(this.retrieve(id));
    }

    @Override
    public boolean run(ChainGeneration chain) throws InterruptedException {
        // il ne faut pas lancer une chaine en cours d'exécution
        // non pas ici , c'est à gérér par celui qui appel

        // the ChainGeneration must be active
        if (!chain.isActive()) {
            StringBuilder strBuilder = new StringBuilder("Unable to run the chain generation ");
            strBuilder.append("<").append(chain.getLabel()).append("> : ").append("the chain is not active");
            LOGGER.warn(strBuilder.toString());
            return false;
        }

        // the difference between the previous activation date and current time must be greater than the periodicity
        if ((chain.getLastDateActivation() != null)
                && chain.getLastDateActivation().plusSeconds(chain.getPeriodicity()).isAfter(OffsetDateTime.now())) {
            StringBuilder strBuilder = new StringBuilder("Unable to run the chain generation ");
            strBuilder.append("<").append(chain.getLabel()).append("> : ").append("the periodicity of ")
                    .append(chain.getPeriodicity())
                    .append(" seconds is not verified. The last activation date is to close from now.");
            LOGGER.warn(strBuilder.toString());
            return false;
        }

        // TODO CMZ : pas ici à mon avis, ça dépends si au moins un fichier AcquisitionFile est détecté
        chain.setLastDateActivation(OffsetDateTime.now());

        // Create a ScanJob
        JobInfo scanJobInfo = new JobInfo();
        scanJobInfo.setParameters(new ChainGenerationJobParameter(chain));
        scanJobInfo.setClassName(ScanJob.class.getName());
        scanJobInfo.setOwner(authResolver.getUser());
        scanJobInfo.setPriority(50);

        scanJobInfo = jobInfoService.createAsQueued(scanJobInfo);

//        while (runnings.isEmpty() || runnings.contains(scanJobInfo.getId())) {
//            Thread.sleep(1_000);
//            LOGGER.info(scanJobInfo.getId() + " is running");
//        }
        LOGGER.info(scanJobInfo.getId() + " is create and queued");

        return true;

    }

    private class ScanJobHandler implements IHandler<JobEvent> {

        @Override
        public void handle(TenantWrapper<JobEvent> wrapper) {
            JobEvent event = wrapper.getContent();
            JobEventType type = event.getJobEventType();
            switch (type) {
                case RUNNING:
                    runnings.add(wrapper.getContent().getJobId());
                    LOGGER.info("RUNNING for " + wrapper.getContent().getJobId());
                    break;
                case SUCCEEDED:
                    succeededs.add(wrapper.getContent().getJobId());
                    LOGGER.info("SUCCEEDED for " + wrapper.getContent().getJobId());
                    break;
                case ABORTED:
                    aborteds.add(wrapper.getContent().getJobId());
                    LOGGER.info("ABORTED for " + wrapper.getContent().getJobId());
                    break;
                case FAILED:
                    faileds.add(wrapper.getContent().getJobId());
                    LOGGER.info("FAILED for " + wrapper.getContent().getJobId());
                    break;
                default:
                    throw new IllegalArgumentException(type + " is not an handled type of JobEvent ");
            }
        }

    }

}
