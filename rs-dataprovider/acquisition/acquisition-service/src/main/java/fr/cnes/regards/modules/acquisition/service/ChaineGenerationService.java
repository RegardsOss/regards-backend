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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.acquisition.dao.IChainGenerationRepository;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.job.ChainGenerationJobParameter;
import fr.cnes.regards.modules.acquisition.service.job.AcquisitionProductsJob;

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
    private IMetaProductService metaProductService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IAuthenticationResolver authResolver;

    public ChaineGenerationService(final ISubscriber subscriber, IChainGenerationRepository repository) {
        super();
        this.chainRepository = repository;
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
    public ChainGeneration retrieveComplete(Long id) {
        ChainGeneration chain = this.retrieve(id);

        chain.setMetaProduct(metaProductService.retrieveComplete(chain.getMetaProduct().getId()));

        if (chain.getScanAcquisitionPluginConf() != null) {
            chain.setScanAcquisitionPluginConf(pluginService
                    .loadPluginConfiguration(chain.getScanAcquisitionPluginConf().getId()));
        }

        if (chain.getCheckAcquisitionPluginConf() != null) {
            chain.setCheckAcquisitionPluginConf(pluginService
                    .loadPluginConfiguration(chain.getCheckAcquisitionPluginConf().getId()));
        }

        if (chain.getGenerateSipPluginConf() != null) {
            chain.setGenerateSipPluginConf(pluginService
                    .loadPluginConfiguration(chain.getGenerateSipPluginConf().getId()));
        }

        if (chain.getPostProcessSipPluginConf() != null) {
            chain.setPostProcessSipPluginConf(pluginService
                    .loadPluginConfiguration(chain.getPostProcessSipPluginConf().getId()));
        }

        return chain;
    }

    @Override
    public void delete(Long id) {
        chainRepository.delete(id);
    }

    @Override
    public boolean run(Long id) {
        return run(this.retrieve(id));
    }

    @Override
    public boolean run(ChainGeneration chain) {
        // the ChainGeneration must be active
        if (!chain.isActive()) {
            LOGGER.warn("[{}] Unable to run a not active the chain generation ", chain.getLabel());
            return false;
        }

        // the ChainGeneration must not be already running
        if (chain.isRunning()) {
            LOGGER.warn("[{}] Unable to run an already running chain generation ", chain.getLabel());
            return false;
        }

        // the difference between the previous activation date and current time must be greater than the periodicity
        if ((chain.getLastDateActivation() != null)
                && chain.getLastDateActivation().plusSeconds(chain.getPeriodicity()).isAfter(OffsetDateTime.now())) {
            LOGGER.warn("[{}] Unable to run the chain generation : the last activation date is to close from now with the periodicity {}. ",
                        chain.getLabel(), chain.getPeriodicity());
            return false;
        }

        chain.setRunning(true);
        chain.setSession(chain.getLabel() + ":" + OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ":"
                + OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));

        save(chain);

        LOGGER.info("[{}] a new session is created", chain.getSession());

        // Create a ScanJob
        JobInfo acquisition = new JobInfo();
        acquisition.setParameters(new ChainGenerationJobParameter(chain));
        acquisition.setClassName(AcquisitionProductsJob.class.getName());
        acquisition.setOwner(authResolver.getUser());
        acquisition.setPriority(50); //TODO CMZ priority ?

        acquisition = jobInfoService.createAsQueued(acquisition);

        return acquisition != null;
    }

}
