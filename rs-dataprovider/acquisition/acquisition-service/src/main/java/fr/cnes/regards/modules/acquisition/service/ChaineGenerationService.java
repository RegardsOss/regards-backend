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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
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

    @Autowired
    private IPluginService pluginService;

    public ChaineGenerationService(IChainGenerationRepository repository) {
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
    public void delete(Long id) {
        chainRepository.delete(id);
    }

    @Override
    public boolean run(Long id) {
        return run(this.retrieve(id));
    }

    @Override
    public boolean run(ChainGeneration chain) {
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

        chain.setLastDateActivation(OffsetDateTime.now());

        // Create a ScanJob
        JobInfo scanJobInfo = new JobInfo();
        scanJobInfo.setParameters(new ChainGenerationJobParameter(chain));
        scanJobInfo.setClassName(ScanJob.class.getName());
        scanJobInfo.setOwner(authResolver.getUser());
        scanJobInfo.setPriority(50);
        
        jobInfoService.createAsQueued(scanJobInfo);

        // TODO CMZ écouter la fin du Job

        return true;

    }

}
