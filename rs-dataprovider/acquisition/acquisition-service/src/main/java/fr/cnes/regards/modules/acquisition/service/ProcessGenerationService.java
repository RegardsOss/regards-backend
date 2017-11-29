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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.acquisition.dao.IProcessGenerationRepository;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.ProcessGeneration;

/**
 *
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class ProcessGenerationService implements IProcessGenerationService {
    
    private static final Logger LOG = LoggerFactory.getLogger(ProcessGenerationService.class);

    private final IProcessGenerationRepository processRepository;

    public ProcessGenerationService(IProcessGenerationRepository repository) {
        super();
        this.processRepository = repository;
    }

    @Override
    public ProcessGeneration save(ProcessGeneration chain) {
        return processRepository.save(chain);
    }

    @Override
    public void delete(ProcessGeneration processGeneration) {
        processRepository.delete(processGeneration);
    }

    @Override
    public Page<ProcessGeneration> retrieveAll(Pageable pageable) {
        return processRepository.findAll(pageable);
    }

    @Override
    public Page<ProcessGeneration> findByChainGeneration(ChainGeneration chainGeneration, Pageable pageable) {
        return processRepository.findByChainGeneration(chainGeneration, pageable);
    }

    @Override
    public Page<ProcessGeneration> findByStartDateBetween(OffsetDateTime start, OffsetDateTime stop,
            Pageable pageable) {
        return processRepository.findByStartDateBetween(start, stop, pageable);
    }

    @Override
    public Page<ProcessGeneration> findByStartDateAfterAndStopDateBefore(OffsetDateTime start, OffsetDateTime stop,
            Pageable pageable) {
        return processRepository.findByStartDateAfterAndStopDateBefore(start, stop, pageable);
    }

    @Override
    public Page<ProcessGeneration> findByStartDateAfter(OffsetDateTime start, Pageable pageable) {
        return processRepository.findByStartDateAfter(start, pageable);
    }

    @Override
    public ProcessGeneration findBySession(String session) {
        return processRepository.findBySession(session);
    }

    @Override
    public void updateProcessGeneration(String session, int nbSipCreated, int nbSipStored, int nbSipError) {
        ProcessGeneration processGeneration = this.findBySession(session);
        if (processGeneration != null) {
            LOG.info("[{}] add nb SIP in process : created:{} - stored:{} - error:{}",session, nbSipCreated, nbSipStored, nbSipError);
            processGeneration.setNbSipCreated(processGeneration.getNbSipCreated() + nbSipCreated);
            processGeneration.setNbSipStored(processGeneration.getNbSipStored() + nbSipStored);
            processGeneration.setNbSipError(processGeneration.getNbSipError() + nbSipError);

            if (processGeneration.getNbSipCreated() == processGeneration.getNbSipStored()
                    + processGeneration.getNbSipError()) {
                processGeneration.setStopDate(OffsetDateTime.now());
                LOG.info("[{}] set stop date in process : {}",processGeneration.getStopDate().toString());
            }

            this.save(processGeneration);
        }
    }

}
